package com.marcinpypec.banktransactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcinpypec.banktransactions.importjob.dto.ImportJobResponse;
import com.marcinpypec.banktransactions.importjob.model.ImportStatus;
import com.marcinpypec.banktransactions.importjob.repository.ImportJobRepository;
import com.marcinpypec.banktransactions.stats.TransactionStatsRepository;
import com.marcinpypec.banktransactions.stats.TransactionStatsRow;
import com.marcinpypec.banktransactions.transactions.TransactionRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
class StatsControllerIntegrationTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", () -> mongo.getConnectionString() + "/test_db");
    }

    @Autowired
    MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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

    @Nested
    class GetStats {

        @Test
        void shouldReturnStatsByCategory() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "CATEGORY", 50, "TOTAL_DESC");

            // then
            assertThat(stats).hasSize(3);
            assertThat(stats).anySatisfy(row -> {
                assertThat(row.key()).isEqualTo("Salary");
                assertThat(row.count()).isEqualTo(1);
            });
            assertThat(stats).anySatisfy(row -> {
                assertThat(row.key()).isEqualTo("Groceries");
                assertThat(row.count()).isEqualTo(2);
            });
        }

        @Test
        void shouldReturnStatsByIban() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "IBAN", 50, "TOTAL_DESC");

            // then
            assertThat(stats).hasSize(1);
            assertThat(stats.getFirst().key()).isEqualTo("PL61109010140000071219812874");
            assertThat(stats.getFirst().count()).isEqualTo(4);
        }

        @Test
        void shouldReturnStatsByMonth() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "MONTH", 50, "TOTAL_DESC");

            // then
            assertThat(stats).hasSize(1);
            assertThat(stats.getFirst().key()).isEqualTo("TOTAL");
            assertThat(stats.getFirst().currency()).isEqualTo("PLN");
        }

        @Test
        void shouldSortAscending() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "CATEGORY", 50, "TOTAL_ASC");

            // then - Rent should be first (most negative)
            assertThat(stats.getFirst().key()).isEqualTo("Rent");
        }

        @Test
        void shouldSortDescending() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "CATEGORY", 50, "TOTAL_DESC");

            // then - Salary should be first (highest positive)
            assertThat(stats.getFirst().key()).isEqualTo("Salary");
        }

        @Test
        void shouldRespectLimit() throws Exception {
            // given
            importTestData();

            // when
            List<TransactionStatsRow> stats = getStats("2026-01", "CATEGORY", 2, "TOTAL_DESC");

            // then
            assertThat(stats).hasSize(2);
        }

        @Test
        void shouldReturnEmptyListWhenNoData() throws Exception {
            mockMvc.perform(get("/api/v1/stats")
                            .param("yearMonth", "2026-01")
                            .param("groupBy", "CATEGORY"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldRejectInvalidYearMonthFormat() throws Exception {
            mockMvc.perform(get("/api/v1/stats")
                            .param("yearMonth", "invalid")
                            .param("groupBy", "CATEGORY"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectLimitBelowOne() throws Exception {
            mockMvc.perform(get("/api/v1/stats")
                            .param("yearMonth", "2026-01")
                            .param("groupBy", "CATEGORY")
                            .param("limit", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectLimitAbove500() throws Exception {
            mockMvc.perform(get("/api/v1/stats")
                            .param("yearMonth", "2026-01")
                            .param("groupBy", "CATEGORY")
                            .param("limit", "501"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetMonthlyStats {
        @Test
        void shouldReturnEmptyListWhenNoData() throws Exception {
            mockMvc.perform(get("/api/v1/stats/monthly")
                            .param("from", "2026-01")
                            .param("to", "2026-12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void shouldRejectFromAfterTo() throws Exception {
            mockMvc.perform(get("/api/v1/stats/monthly")
                            .param("from", "2026-12")
                            .param("to", "2026-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectInvalidFromFormat() throws Exception {
            mockMvc.perform(get("/api/v1/stats/monthly")
                            .param("from", "invalid")
                            .param("to", "2026-01"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectInvalidToFormat() throws Exception {
            mockMvc.perform(get("/api/v1/stats/monthly")
                            .param("from", "2026-01")
                            .param("to", "invalid"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldAcceptSameFromAndTo() throws Exception {
            mockMvc.perform(get("/api/v1/stats/monthly")
                            .param("from", "2026-01")
                            .param("to", "2026-01"))
                    .andExpect(status().isOk());
        }
    }

    // Helper methods

    private void importTestData() throws Exception {
        String csv = """
                IBAN,date,currency,category,amount
                PL61109010140000071219812874,2026-01-02,PLN,Salary,5000.00
                PL61109010140000071219812874,2026-01-03,PLN,Rent,-2000.00
                PL61109010140000071219812874,2026-01-04,PLN,Groceries,-150.00
                PL61109010140000071219812874,2026-01-05,PLN,Groceries,-100.00
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        String json = mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("yearMonth", "2026-01")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ImportJobResponse response = objectMapper.readValue(json, ImportJobResponse.class);

        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> {
                    String statusJson = mockMvc.perform(get("/api/v1/imports/{id}", response.id()))
                            .andReturn().getResponse().getContentAsString();
                    return objectMapper.readValue(statusJson, ImportJobResponse.class).status();
                }, status -> status == ImportStatus.COMPLETED);
    }

    private List<TransactionStatsRow> getStats(String yearMonth, String groupBy, int limit, String sort) throws Exception {
        String json = mockMvc.perform(get("/api/v1/stats")
                        .param("yearMonth", yearMonth)
                        .param("groupBy", groupBy)
                        .param("limit", String.valueOf(limit))
                        .param("sort", sort))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Arrays.asList(objectMapper.readValue(json, TransactionStatsRow[].class));
    }
}
