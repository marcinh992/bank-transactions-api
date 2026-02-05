package com.marcinpypec.banktransactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcinpypec.banktransactions.importjob.dto.ImportJobResponse;
import com.marcinpypec.banktransactions.importjob.model.ImportStatus;
import com.marcinpypec.banktransactions.importjob.repository.ImportJobRepository;
import com.marcinpypec.banktransactions.stats.StatsGroupBy;
import com.marcinpypec.banktransactions.stats.TransactionStatsRepository;
import com.marcinpypec.banktransactions.stats.TransactionStatsRow;
import com.marcinpypec.banktransactions.transactions.TransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ImportControllerIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> mongo.getConnectionString() + "/test_db");
    }

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @Autowired
    ImportJobRepository importJobRepository;

    @Autowired
    TransactionRepository transactionRepository;

    @Autowired
    TransactionStatsRepository statsRepository;

    @BeforeEach
    void cleanup() {
        importJobRepository.deleteAll();
        transactionRepository.deleteAll();
        statsRepository.deleteAll();
    }

    @Test
    void shouldImportCsvAndGenerateStats() throws Exception {
        // given
        String csv = """
                IBAN,date,currency,category,amount
                PL61109010140000071219812874,2026-01-02,PLN,Salary,12500.00
                PL61109010140000071219812874,2026-01-03,PLN,Rent,-3200.00
                PL61109010140000071219812874,2026-01-04,PLN,Groceries,-186.47
                PL61109010140000071219812874,2026-01-05,PLN,Groceries,-92.13
                """;

        // when: upload CSV
        ImportJobResponse created = uploadCsv("2026-01", csv, "test.csv");

        // then: job is created with RECEIVED status
        assertThat(created.id()).isNotBlank();
        assertThat(created.status()).isEqualTo(ImportStatus.RECEIVED);
        assertThat(created.yearMonth()).isEqualTo("2026-01");

        // when: poll until completed
        ImportJobResponse completed = pollUntilCompleted(created.id());

        // then: all rows imported
        assertThat(completed.status()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(completed.totalRows()).isEqualTo(4);
        assertThat(completed.importedRows()).isEqualTo(4);
        assertThat(completed.invalidRows()).isZero();

        // and: stats by category are available
        List<TransactionStatsRow> categoryStats = getStats("2026-01", StatsGroupBy.CATEGORY);

        assertThat(categoryStats).hasSize(3);
        assertThat(categoryStats).anySatisfy(row -> {
            assertThat(row.key()).isEqualTo("Salary");
            assertThat(row.totalAmount()).isEqualByComparingTo(new BigDecimal("12500.00"));
            assertThat(row.count()).isEqualTo(1);
        });
        assertThat(categoryStats).anySatisfy(row -> {
            assertThat(row.key()).isEqualTo("Groceries");
            assertThat(row.totalAmount()).isEqualByComparingTo(new BigDecimal("-278.60"));
            assertThat(row.count()).isEqualTo(2);
        });

        // and: stats by IBAN are available
        List<TransactionStatsRow> ibanStats = getStats("2026-01", StatsGroupBy.IBAN);

        assertThat(ibanStats).hasSize(1);
        assertThat(ibanStats.getFirst().key()).isEqualTo("PL61109010140000071219812874");
        assertThat(ibanStats.getFirst().count()).isEqualTo(4);
    }

    @Test
    void shouldHandlePartialImportWithInvalidRows() throws Exception {
        // given: CSV with some invalid rows
        String csv = """
                IBAN,date,currency,category,amount
                PL61109010140000071219812874,2026-01-02,PLN,Salary,12500.00
                INVALID_IBAN,2026-01-03,PLN,Food,-50.00
                PL61109010140000071219812874,2026-02-15,PLN,Food,-30.00
                PL61109010140000071219812874,2026-01-05,PLN,Groceries,-100.00
                """;

        // when
        ImportJobResponse created = uploadCsv("2026-01", csv, "partial.csv");
        ImportJobResponse completed = pollUntilCompleted(created.id());

        // then: partial success
        assertThat(completed.status()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(completed.totalRows()).isEqualTo(4);
        assertThat(completed.importedRows()).isEqualTo(2);
        assertThat(completed.invalidRows()).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateImportForSameMonth() throws Exception {
        // given: first import
        String csv = """
                IBAN,date,currency,category,amount
                PL61109010140000071219812874,2026-01-02,PLN,Salary,5000.00
                """;
        uploadCsv("2026-01", csv, "first.csv");

        // when: try to import again for same month
        MockMultipartFile file = new MockMultipartFile(
                "file", "second.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // then: 409 Conflict
        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("yearMonth", "2026-01"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidYearMonthFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                "IBAN,date,currency,category,amount".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("yearMonth", "2026-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("yearMonth", "01-2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForNonExistentJob() throws Exception {
        mockMvc.perform(get("/api/v1/imports/{jobId}", "nonexistent123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnEmptyStatsWhenNoData() throws Exception {
        mockMvc.perform(get("/api/v1/stats")
                        .param("yearMonth", "2026-01")
                        .param("groupBy", "CATEGORY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void shouldRejectInvalidStatsLimit() throws Exception {
        mockMvc.perform(get("/api/v1/stats")
                        .param("yearMonth", "2026-01")
                        .param("groupBy", "CATEGORY")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/stats")
                        .param("yearMonth", "2026-01")
                        .param("groupBy", "CATEGORY")
                        .param("limit", "501"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidMonthlyRange() throws Exception {
        mockMvc.perform(get("/api/v1/stats/monthly")
                        .param("from", "2026-03")
                        .param("to", "2026-01"))
                .andExpect(status().isBadRequest());
    }

    // Helper methods

    private ImportJobResponse uploadCsv(String yearMonth, String csvContent, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                filename,
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        String json = mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("yearMonth", yearMonth)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(json, ImportJobResponse.class);
    }

    private ImportJobResponse pollUntilCompleted(String jobId) {
        return Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> getImportStatus(jobId),
                        response -> response.status() == ImportStatus.COMPLETED
                                || response.status() == ImportStatus.FAILED);
    }

    private ImportJobResponse getImportStatus(String jobId) throws Exception {
        String json = mockMvc.perform(get("/api/v1/imports/{jobId}", jobId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(json, ImportJobResponse.class);
    }

    private List<TransactionStatsRow> getStats(String yearMonth, StatsGroupBy groupBy) throws Exception {
        String json = mockMvc.perform(get("/api/v1/stats")
                        .param("yearMonth", yearMonth)
                        .param("groupBy", groupBy.name()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Arrays.asList(objectMapper.readValue(json, TransactionStatsRow[].class));
    }
}
