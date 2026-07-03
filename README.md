# TurboSQL

TurboSQL is a standalone SQL analysis and optimization engine. It exposes a REST API that parses SQL, validates it, builds compiler-style intermediate representations, applies deterministic optimization rules, estimates cost, recommends indexes, detects anti-patterns, rewrites the query, and returns structured JSON.

TurboSQL does not execute SQL against a database.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Maven
- Jackson
- JUnit 5
- Mockito
- Testcontainers
- Docker
- OpenAPI / Swagger
- Lombok
- MapStruct

## Architecture

TurboSQL follows Clean Architecture with these module boundaries:

- `api`
- `application`
- `domain`
- `infrastructure`
- `common`

Dependencies should point inward, and constructor-based dependency injection should be used throughout.

## Developer Commands

### Build

```bash
mvn clean install
```

### Run Tests

```bash
mvn test
```

### Run Integration Tests

```bash
mvn verify
```

### Run Locally

```bash
mvn spring-boot:run
```

### Package Application

```bash
mvn clean package
```

### Run Packaged JAR

```bash
java -jar target/turbosql-*.jar
```

### Build Docker Image

```bash
docker build -t turbosql:latest .
```

### Run Docker Container

```bash
docker run --rm -p 8080:8080 turbosql:latest
```

### Run With Docker Compose

```bash
docker compose up --build
```

### Stop Docker Compose

```bash
docker compose down
```

## REST API Commands

All APIs return JSON. TurboSQL does not return HTML or natural language optimization output.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/analyze` | Full SQL analysis and optimization report |
| `POST` | `/api/v1/tokenize` | Generate SQL lexer tokens |
| `POST` | `/api/v1/parse` | Parse SQL and validate syntax |
| `POST` | `/api/v1/ast` | Generate AST |
| `POST` | `/api/v1/relational-algebra` | Generate relational algebra |
| `POST` | `/api/v1/logical-plan` | Generate logical plan |
| `POST` | `/api/v1/optimize` | Apply optimization rules |
| `POST` | `/api/v1/cost` | Estimate execution cost |
| `POST` | `/api/v1/physical-plan` | Generate physical plan |
| `POST` | `/api/v1/index-advisor` | Recommend indexes |
| `POST` | `/api/v1/anti-patterns` | Detect SQL anti-patterns |
| `POST` | `/api/v1/rewrite` | Rewrite SQL while preserving results |
| `POST` | `/api/v1/health` | Health check |

### Analyze Request

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM employee WHERE YEAR(created_at)=2025"
  }'
```

### Tokenize Request

```bash
curl -X POST http://localhost:8080/api/v1/tokenize \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT id, name FROM employee"
  }'
```

### Parse Request

```bash
curl -X POST http://localhost:8080/api/v1/parse \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT id FROM employee WHERE active = true"
  }'
```

### AST Request

```bash
curl -X POST http://localhost:8080/api/v1/ast \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT department_id, COUNT(*) FROM employee GROUP BY department_id"
  }'
```

### Relational Algebra Request

```bash
curl -X POST http://localhost:8080/api/v1/relational-algebra \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT e.name, d.name FROM employee e JOIN department d ON e.department_id = d.id"
  }'
```

### Logical Plan Request

```bash
curl -X POST http://localhost:8080/api/v1/logical-plan \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT name FROM employee WHERE salary > 100000 ORDER BY name LIMIT 10"
  }'
```

### Optimize Request

```bash
curl -X POST http://localhost:8080/api/v1/optimize \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM employee WHERE active = true AND department_id = 10"
  }'
```

### Cost Request

```bash
curl -X POST http://localhost:8080/api/v1/cost \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM orders WHERE created_at >= DATE '\''2026-01-01'\''"
  }'
```

### Physical Plan Request

```bash
curl -X POST http://localhost:8080/api/v1/physical-plan \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT customer_id, SUM(total) FROM orders GROUP BY customer_id"
  }'
```

### Index Advisor Request

```bash
curl -X POST http://localhost:8080/api/v1/index-advisor \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM employee WHERE department_id = 10 AND active = true"
  }'
```

### Anti-Patterns Request

```bash
curl -X POST http://localhost:8080/api/v1/anti-patterns \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM employee WHERE LOWER(email) = '\''a@example.com'\''"
  }'
```

### Rewrite Request

```bash
curl -X POST http://localhost:8080/api/v1/rewrite \
  -H 'Content-Type: application/json' \
  -d '{
    "dialect": "postgresql",
    "sql": "SELECT * FROM employee WHERE YEAR(created_at)=2025"
  }'
```

### Health Request

```bash
curl -X POST http://localhost:8080/api/v1/health
```

## Request Format

```json
{
  "dialect": "postgresql",
  "sql": "SELECT * FROM employee WHERE YEAR(created_at)=2025"
}
```

## Response Format

```json
{
  "success": true,
  "dialect": "postgresql",
  "tokens": [],
  "ast": {},
  "semanticAnalysis": {},
  "relationalAlgebra": {},
  "logicalPlan": {},
  "optimizationRules": [],
  "physicalPlan": {},
  "estimatedCost": {
    "cpu": 0,
    "memory": 0,
    "diskIo": 0,
    "networkIo": 0,
    "totalCost": 0
  },
  "recommendedIndexes": [],
  "antiPatterns": [],
  "rewrittenQuery": "",
  "summary": {
    "estimatedImprovement": "42%",
    "risk": "LOW"
  }
}
```

## Error Response

```json
{
  "success": false,
  "error": {
    "code": "SQL_SYNTAX_ERROR",
    "message": "Unexpected token FROM",
    "line": 1,
    "column": 18
  }
}
```

## Supported SQL Commands

### DDL

- `CREATE TABLE`
- `ALTER TABLE`
- `DROP TABLE`
- `CREATE INDEX`
- `DROP INDEX`
- `CREATE VIEW`
- `DROP VIEW`
- `TRUNCATE`
- `COMMENT`
- `RENAME`
- `PRIMARY KEY`
- `FOREIGN KEY`
- `CHECK`
- `UNIQUE`
- `DEFAULT`
- `AUTO_INCREMENT`
- `IDENTITY`
- `PARTITION`

### DML

- `SELECT`
- `INSERT`
- `UPDATE`
- `DELETE`
- `MERGE`
- `UPSERT`
- `RETURNING`

### Transactions

- `BEGIN`
- `COMMIT`
- `ROLLBACK`
- `SAVEPOINT`

### Joins

- `INNER JOIN`
- `LEFT JOIN`
- `RIGHT JOIN`
- `FULL JOIN`
- `CROSS JOIN`
- `SELF JOIN`
- `NATURAL JOIN`
- `LATERAL JOIN`

### Window Functions

- `ROW_NUMBER`
- `RANK`
- `DENSE_RANK`
- `LEAD`
- `LAG`
- `FIRST_VALUE`
- `LAST_VALUE`
- `NTILE`

### Aggregation

- `GROUP BY`
- `ROLLUP`
- `CUBE`
- `GROUPING SETS`
- `HAVING`

### Operators And Clauses

- `CASE`
- `COALESCE`
- `NULLIF`
- `EXISTS`
- `NOT EXISTS`
- `ANY`
- `ALL`
- `IN`
- `LIKE`
- `ILIKE`
- `BETWEEN`
- `UNION`
- `UNION ALL`
- `INTERSECT`
- `EXCEPT`
- `DISTINCT`
- `ORDER BY`
- `LIMIT`
- `OFFSET`
- `FETCH`

### Subqueries

- Nested subqueries
- Correlated subqueries
- CTE
- Recursive CTE

### Functions

- Date functions
- Math functions
- String functions
- JSON functions
- Regex functions
- UUID functions
- Array functions

## Lexer Output

The lexer must generate tokens without using regex-based SQL parsing. Each token contains:

- `type`
- `value`
- `line`
- `column`
- `position`

Supported token categories include:

- `identifier`
- `operator`
- `literal`
- `keyword`
- `punctuation`

## Parser Output

The parser should be a recursive descent parser and produce a complete AST. Each AST node contains:

- `id`
- `type`
- `value`
- `children`
- `metadata`

## Semantic Analyzer Checks

- Unknown tables
- Unknown columns
- Unknown aliases
- Duplicate aliases
- Invalid `GROUP BY`
- Aggregate misuse
- Invalid `ORDER BY`
- Invalid `HAVING`
- Ambiguous column references
- Invalid `JOIN`

## Relational Algebra Operators

- Projection
- Selection
- Join
- Rename
- Grouping
- Sort
- Union
- Difference
- Cartesian Product

## Logical Plan Operators

- Projection
- Selection
- Join
- Aggregate
- Sort
- Limit
- Filter
- Scan

## Optimization Rules

- Predicate Pushdown
- Projection Pushdown
- Constant Folding
- Boolean Simplification
- Join Reordering
- Join Elimination
- Dead Predicate Removal
- Subquery Flattening
- Filter Merge
- Expression Simplification
- Distinct Elimination
- Sort Elimination
- Limit Pushdown
- Aggregation Pushdown
- Window Optimization
- Common Subexpression Elimination
- Predicate Simplification

## Cost Estimation

TurboSQL estimates:

- CPU
- Memory
- Disk IO
- Network IO
- Estimated Rows
- Cardinality
- Selectivity
- Scan Cost
- Join Cost
- Sort Cost
- Hash Cost
- Total Estimated Cost

## Physical Plan Operators

- Sequential Scan
- Index Scan
- Bitmap Scan
- Hash Join
- Nested Loop
- Merge Join
- Aggregate
- Sort
- Materialize
- Limit
- Hash Aggregate

## Index Advisor Recommendations

TurboSQL recommends:

- Single-column indexes
- Composite indexes
- Covering indexes
- Expression indexes
- Partial indexes

Each recommendation contains:

- `recommendedColumns`
- `reason`
- `estimatedBenefit`

## Anti-Pattern Detection

TurboSQL detects:

- `SELECT *`
- Cartesian Join
- Missing `WHERE`
- Function on indexed column
- Leading wildcard `LIKE`
- Redundant `DISTINCT`
- Unused `ORDER BY`
- Duplicate predicates
- Large `OFFSET`
- Correlated subqueries
- Unnecessary casts
- `OR` conditions preventing index usage
- Multiple nested subqueries

Severity values:

- `LOW`
- `MEDIUM`
- `HIGH`
- `CRITICAL`

## Query Rewriter

The query rewriter must preserve correctness and never change query results. It returns:

- `originalQuery`
- `optimizedQuery`
- `changes`

## Non-Goals

- TurboSQL is not a database.
- TurboSQL is not an ORM.
- TurboSQL is not an SQL executor.
- TurboSQL does not rely on PostgreSQL `EXPLAIN`, MySQL `EXPLAIN`, or any database optimizer internally.
