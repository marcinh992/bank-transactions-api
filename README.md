# Bank Transactions API

A REST API application for importing and analyzing bank account transactions. Upload your bank statements in CSV format and get aggregated statistics by category, IBAN, or month to better understand your personal budget.

## Tech Stack

- **Java 21** with modern features (records, pattern matching)
- **Spring Boot 4** (WebMVC, Validation, Actuator)
- **MongoDB 7** for flexible document storage
- **Docker & Docker Compose** for containerization
- **Gradle** build tool
- **OpenAPI 3 / Swagger UI** for interactive API documentation

## Architecture Overview

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  CSV Upload     │────▶│  Async Import    │────▶│  Transactions   │
│  (POST /imports)│     │  Processor       │     │  Collection     │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
┌─────────────────┐     ┌──────────────────┐              │
│  Stats API      │◀────│  Materialized    │◀─────────────┘
│  (GET /stats)   │     │  Stats Views     │
└─────────────────┘     └──────────────────┘
```

**Key Design Decisions:**
- **Asynchronous processing** - Large CSV files are processed in the background; clients poll for status
- **Materialized views** - Statistics are pre-computed after import for fast query performance
- **Batch writes** - Transactions are inserted in batches of 1000 for optimal MongoDB performance

## Getting Started

### Prerequisites

- Docker and Docker Compose
- (Optional) Java 21 and Gradle for local development

### Run with Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/your-username/bank-transactions-api.git
cd bank-transactions-api

# Start the application
docker-compose up --build

# API is available at http://localhost:8080
```

### Run Locally

```bash
# Start MongoDB
docker-compose up mongo -d

# Run the application
./gradlew bootRun

# Or build and run JAR
./gradlew bootJar
java -jar build/libs/bank-transactions-api-0.0.1-SNAPSHOT.jar
```

## API Documentation (Swagger UI)

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI spec (JSON):
```
http://localhost:8080/v3/api-docs
```

---

## API Reference

### Import Transactions

#### Upload CSV File

```http
POST /api/v1/imports
Content-Type: multipart/form-data
```

| Parameter   | Type   | Description                        |
|-------------|--------|------------------------------------|
| `yearMonth` | string | Target month in `yyyy-MM` format   |
| `file`      | file   | CSV file with transactions         |

**Example:**

```bash
curl -X POST http://localhost:8080/api/v1/imports \
  -F "yearMonth=2026-01" \
  -F "file=@test-data/sample/transactions_2026-01.csv"
```

**Response (202 Accepted):**

```json
{
  "id": "507f1f77bcf86cd799439011",
  "yearMonth": "2026-01",
  "fileName": "transactions_2026-01.csv",
  "status": "RECEIVED",
  "totalRows": 0,
  "importedRows": 0,
  "invalidRows": 0,
  "createdAt": "2026-01-15T10:30:00Z",
  "startedAt": null,
  "finishedAt": null,
  "errorMessage": null
}
```

#### Check Import Status

```http
GET /api/v1/imports/{jobId}
```

**Example:**

```bash
curl http://localhost:8080/api/v1/imports/507f1f77bcf86cd799439011
```

**Response (200 OK):**

```json
{
  "id": "507f1f77bcf86cd799439011",
  "yearMonth": "2026-01",
  "fileName": "transactions_2026-01.csv",
  "status": "COMPLETED",
  "totalRows": 15,
  "importedRows": 15,
  "invalidRows": 0,
  "createdAt": "2026-01-15T10:30:00Z",
  "startedAt": "2026-01-15T10:30:01Z",
  "finishedAt": "2026-01-15T10:30:02Z",
  "errorMessage": null
}
```

**Import Statuses:**
- `RECEIVED` - File uploaded, waiting for processing
- `PROCESSING` - Import in progress
- `COMPLETED` - Import finished successfully
- `FAILED` - Import failed (check `errorMessage`)

---

### Transaction Statistics

#### Get Stats by Category or IBAN

```http
GET /api/v1/stats
```

| Parameter   | Type   | Default      | Description                          |
|-------------|--------|--------------|--------------------------------------|
| `yearMonth` | string | required     | Month in `yyyy-MM` format            |
| `groupBy`   | string | required     | `CATEGORY` or `IBAN`                 |
| `limit`     | int    | 50           | Max results (1-500)                  |
| `sort`      | string | `TOTAL_DESC` | `TOTAL_DESC` or `TOTAL_ASC`          |

**Example - Stats by Category:**

```bash
curl "http://localhost:8080/api/v1/stats?yearMonth=2026-01&groupBy=CATEGORY"
```

**Response:**

```json
[
  {
    "key": "Salary",
    "currency": "PLN",
    "count": 1,
    "totalAmount": 12500.00
  },
  {
    "key": "Rent",
    "currency": "PLN",
    "count": 1,
    "totalAmount": -3200.00
  },
  {
    "key": "Groceries",
    "currency": "PLN",
    "count": 2,
    "totalAmount": -278.60
  }
]
```

#### Get Monthly Totals

```http
GET /api/v1/stats/monthly
```

| Parameter | Type   | Description                    |
|-----------|--------|--------------------------------|
| `from`    | string | Start month in `yyyy-MM`       |
| `to`      | string | End month in `yyyy-MM`         |

**Example:**

```bash
curl "http://localhost:8080/api/v1/stats/monthly?from=2026-01&to=2026-03"
```

**Response:**

```json
[
  {
    "yearMonth": "2026-01",
    "currency": "PLN",
    "count": 14,
    "totalAmount": 7289.64
  },
  {
    "yearMonth": "2026-01",
    "currency": "EUR",
    "count": 1,
    "totalAmount": -120.00
  }
]
```

---

## CSV Format

The import file must be a valid CSV with the following columns:

| Column   | Format              | Example                          |
|----------|---------------------|----------------------------------|
| IBAN     | 2 letters + 13-32 alphanumeric | `PL61109010140000071219812874` |
| date     | `yyyy-MM-dd`        | `2026-01-15`                     |
| currency | 3 uppercase letters | `PLN`, `EUR`, `USD`              |
| category | string              | `Groceries`, `Salary`, `Rent`    |
| amount   | decimal number      | `1500.00`, `-89.99`              |

**Example CSV:**

```csv
IBAN,date,currency,category,amount
PL61109010140000071219812874,2026-01-02,PLN,Salary,12500.00
PL61109010140000071219812874,2026-01-03,PLN,Rent,-3200.00
PL61109010140000071219812874,2026-01-04,PLN,Groceries,-186.47
```

**Validation Rules:**
- All transactions must have dates within the specified `yearMonth`
- IBAN must match pattern: `^[A-Z]{2}[0-9A-Z]{13,32}$`
- Currency must be 3 uppercase letters
- Invalid rows are skipped and counted in `invalidRows`

---

## Health Check

```bash
curl http://localhost:8080/actuator/health
```

---

## Running Tests

```bash
# Run all tests (requires Docker for Testcontainers)
./gradlew test

# Run specific test
./gradlew test --tests ImportControllerTest
```

---

## Sample Data

A sample CSV file is provided in `test-data/sample/transactions_2026-01.csv` for testing purposes.

```bash
# Quick test workflow
curl -X POST http://localhost:8080/api/v1/imports \
  -F "yearMonth=2026-01" \
  -F "file=@test-data/sample/transactions_2026-01.csv"

# Poll until status is COMPLETED, then:
curl "http://localhost:8080/api/v1/stats?yearMonth=2026-01&groupBy=CATEGORY"
```
