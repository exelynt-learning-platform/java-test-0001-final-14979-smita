
# Resource Booking API

A RESTful Resource Booking System built with Spring Boot, Java 17+, Spring Security, JWT, JPA/Hibernate and MySQL/PostgreSQL.

## Features

- JWT authentication with `POST /auth/login`
- BCrypt password hashing
- Stateless Spring Security
- `ADMIN` and `USER` RBAC
- ADMIN full CRUD for resources and reservations
- USER can read resources
- USER can create reservations
- USER can view only their own reservations
- USER identity is taken from the JWT, not from the request
- Reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Decimal reservation price using `BigDecimal`
- Reservation filtering by status/minPrice/maxPrice
- Pagination with page/size
- Optional sorting
- Double-booking prevention
- Validation and consistent JSON error responses
- MySQL production database and H2 test database
- Swagger/OpenAPI documentation
- Seed ADMIN and USER accounts
- Unit/integration test support

## Technology

- Java 17
- Spring Boot 3.3.4
- Spring Web
- Spring Data JPA / Hibernate
- Spring Security
- JWT (JJWT 0.12.6)
- MySQL / PostgreSQL driver
- H2 for tests
- SpringDoc OpenAPI / Swagger
- Maven

## Database setup

Create a MySQL database:

```sql
CREATE DATABASE booking_db;
```

Default local configuration:

```text
DB_URL=jdbc:mysql://localhost:3306/booking_db
DB_USERNAME=root
DB_PASSWORD=root
```

You can override these using environment variables.

For PostgreSQL, set for example:

```text
DB_URL=jdbc:postgresql://localhost:5432/booking_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
DB_DRIVER=org.postgresql.Driver
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8080` | Application port |
| `DB_URL` | MySQL localhost URL | Database URL |
| `DB_USERNAME` | `root` | Database user |
| `DB_PASSWORD` | `root` | Database password |
| `DB_DRIVER` | MySQL driver | JDBC driver |
| `DDL_AUTO` | `update` | Hibernate schema mode |
| `JWT_SECRET` | development secret | JWT signing key; use a strong secret in real deployments |
| `JWT_EXPIRATION_MS` | `86400000` | JWT validity |
| `SEED_ENABLED` | `true` | Enable seed users |
| `SEED_ADMIN_USERNAME` | `admin` | Seed ADMIN username |
| `SEED_ADMIN_PASSWORD` | `Admin@123` | Seed ADMIN password |
| `SEED_USER_USERNAME` | `user` | Seed USER username |
| `SEED_USER_PASSWORD` | `User@123` | Seed USER password |

The JWT secret must be at least 32 bytes.

## Run the application

```bash
mvn clean test
mvn spring-boot:run
```

Or run `BookingApiApplication` from IntelliJ IDEA.

## Swagger

After startup:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Use `POST /auth/login` first, copy the JWT, then click Swagger's **Authorize** button and enter:

```text
Bearer <your-token>
```

## Seed users

By default:

### ADMIN

```text
username: admin
password: Admin@123
role: ADMIN
```

### USER

```text
username: user
password: User@123
role: USER
```

Change these passwords through environment variables for a real deployment.

## Authentication

### POST /auth/login

Request:

```json
{
  "username": "user",
  "password": "User@123"
}
```

Response:

```json
{
  "token": "eyJ..."
}
```

Send the token on protected endpoints:

```text
Authorization: Bearer eyJ...
```

## Resource APIs

### USER + ADMIN

```text
GET /resources
GET /resources/{id}
```

### ADMIN only

```text
POST   /resources
PUT    /resources/{id}
DELETE /resources/{id}
```

Create resource example:

```json
{
  "name": "Meeting Room A",
  "type": "ROOM",
  "description": "Board room",
  "available": true
}
```

## Reservation APIs

### USER + ADMIN

```text
GET /reservations
GET /reservations/{id}
POST /reservations
```

### ADMIN only

```text
PUT    /reservations/{id}
DELETE /reservations/{id}
```

A USER cannot update or delete a reservation. This matches the assignment's requirement that USERs create reservations and view their own reservations, while ADMIN has full reservation CRUD.

## Create reservation

USER request:

```json
{
  "resourceId": 1,
  "startTime": "2026-09-15T10:00:00",
  "endTime": "2026-09-15T12:00:00",
  "price": 500.00
}
```

The authenticated USER is taken from the JWT. A `userId` sent by a USER is never used to choose ownership.

ADMIN can optionally provide `userId` when creating a reservation:

```json
{
  "resourceId": 1,
  "userId": 2,
  "startTime": "2026-09-15T10:00:00",
  "endTime": "2026-09-15T12:00:00",
  "price": 500.00,
  "status": "CONFIRMED"
}
```

## Update reservation

ADMIN only:

```json
{
  "resourceId": 1,
  "userId": 2,
  "startTime": "2026-09-15T11:00:00",
  "endTime": "2026-09-15T13:00:00",
  "price": 600.00,
  "status": "CONFIRMED"
}
```

Allowed statuses:

```text
PENDING
CONFIRMED
CANCELLED
```

Cancelled reservations do not block a resource from being booked again.

## Filtering

Filter by status:

```text
GET /reservations?status=CONFIRMED
```

Minimum price:

```text
GET /reservations?minPrice=100
```

Maximum price:

```text
GET /reservations?maxPrice=1000
```

Combined:

```text
GET /reservations?status=PENDING&minPrice=100&maxPrice=1000
```

## Pagination

```text
GET /reservations?page=0&size=10
```

`page` starts from `0`.

`size` must be between `1` and `100`.

## Sorting

Ascending:

```text
GET /reservations?sort=price,asc
```

Descending:

```text
GET /reservations?sort=price,desc
```

Allowed sort fields:

```text
id
startTime
endTime
price
status
createdAt
updatedAt
```

## Ownership and security rules

| Endpoint | ADMIN | USER |
|---|---:|---:|
| `POST /auth/login` | Public | Public |
| `GET /resources` | Yes | Yes |
| `GET /resources/{id}` | Yes | Yes |
| `POST /resources` | Yes | No |
| `PUT /resources/{id}` | Yes | No |
| `DELETE /resources/{id}` | Yes | No |
| `GET /reservations` | All | Own only |
| `GET /reservations/{id}` | Any | Own only |
| `POST /reservations` | Yes | Yes |
| `PUT /reservations/{id}` | Yes | No |
| `DELETE /reservations/{id}` | Yes | No |
| `/users/**` | Yes | No |

## Error responses

Example:

```json
{
  "timestamp": "2026-09-13T14:00:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource"
}
```

Common status codes:

- `201 Created` - successful creation
- `200 OK` - successful read/update/login
- `204 No Content` - successful deletion
- `400 Bad Request` - validation or invalid parameters
- `401 Unauthorized` - missing/invalid/expired JWT or bad login
- `403 Forbidden` - authenticated but insufficient role/ownership
- `404 Not Found` - requested entity does not exist
- `409 Conflict` - double booking/database conflict

## Testing

Tests use an in-memory H2 database.

Run:

```bash
mvn clean test
```

The test profile disables seed users and creates its own test data.

## Project structure

```text
src/main/java/com/Booking/booking_api
├── config
│   ├── DataInitializer.java
│   ├── JwtAuthenticationFilter.java
│   ├── OpenAPIConfig.java
│   └── SecurityConfig.java
├── controller
├── dto
├── entity
├── enums
├── exception
├── repository
└── service
```
