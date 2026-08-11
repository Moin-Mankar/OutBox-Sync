# Outbox-Sync

A Spring Boot implementation of the **Transactional Outbox Pattern** for reliably handling events generated from database transactions.

The project demonstrates how to avoid the **dual-write problem**, where a business operation successfully updates a database but the corresponding event fails to reach an external system.

---

## Overview

In a typical application, an operation may require two separate actions:

1. Save business data to the database.
2. Publish an event to another system.

If these operations are performed independently, they can become inconsistent.

For example:

```text
Save Order      -> SUCCESS
Publish Event   -> FAILURE
```

The order exists in the database, but the external system never receives the event.

Outbox-Sync solves this by storing the business data and its corresponding event in the same database transaction.

The event is then processed asynchronously by a scheduled worker that attempts delivery to an external HTTP endpoint.

---

## How the Transactional Outbox Pattern Works

Instead of publishing the event directly during the business operation:

```text
Client
  |
  v
Order Service
  |
  +----> Save Order
  |
  +----> Publish Event
```

The application stores the event in an Outbox table as part of the same database transaction:

```text
Client
  |
  v
Order Service
  |
  v
Database Transaction
  |
  +--------------------+
  |                    |
  v                    v
Order Table        Outbox Table
                       |
                       v
                Outbox Worker
                       |
                       v
                External HTTP API
```

This ensures that the order and its event are persisted together.

If the transaction fails, neither the order nor the outbox event is committed.

If the transaction succeeds, the event remains in the Outbox table until it is successfully processed.

---

## Project Flow

The application follows this general flow:

```text
POST /order/create
        |
        v
   OrderService
        |
        v
 @Transactional
        |
        +----------------------+
        |                      |
        v                      v
   Save Order          Save Outbox Event
        |                      |
        +----------+-----------+
                   |
                   v
              MySQL Database
                   |
                   v
            OutboxWorker
          (runs every 5 sec)
                   |
                   v
          Find PENDING Events
                   |
                   v
       HTTP Request using RestTemplate
                   |
          +--------+--------+
          |                 |
       Success            Failure
          |                 |
          v                 v
        SENT          Retry / FAILED
```

---

## Main Components

### 1. Order

The `Order` entity represents the business data being created by the application.

When an order is created, an associated outbox event is generated.

---

### 2. Outbox Event

The `OutboxEvent` entity stores events that need to be delivered.

The event contains information such as:

- Event ID
- Event type
- Event payload
- Processing status
- Retry count
- Creation timestamp
- Update timestamp

The event status is used to track its processing lifecycle.

---

## Event States

The project uses three main states:

```text
PENDING
   |
   | successful delivery
   v
 SENT


PENDING
   |
   | delivery failure
   v
 Retry
   |
   | after maximum retries
   v
 FAILED
```

### PENDING

The event has been stored in the Outbox table but has not yet been successfully delivered.

### SENT

The event was successfully delivered to the external HTTP endpoint.

### FAILED

The event could not be delivered after the configured retry attempts.

---

## Transactional Order Creation

The order creation operation is handled inside a database transaction.

Conceptually:

```text
BEGIN TRANSACTION

    Create Order

    Create ORDER_CREATED Outbox Event

COMMIT
```

Both records are committed together.

If an error occurs before the transaction commits:

```text
ROLLBACK

Order       -> Not saved
Outbox Event -> Not saved
```

This is the key part of the Transactional Outbox Pattern demonstrated by the project.

---

## Outbox Worker

The `OutboxWorker` is responsible for processing pending events.

It runs periodically using Spring's scheduling mechanism.

The worker:

1. Finds pending outbox events.
2. Attempts to send each event to the external HTTP endpoint.
3. Updates the event status based on the result.
4. Increments the retry count when delivery fails.
5. Marks the event as `FAILED` after the maximum number of retries.

The worker currently runs every **5 seconds**.

---

## Retry Handling

External systems can fail temporarily.

For example:

```text
Attempt 1 -> HTTP 500 -> Retry
Attempt 2 -> HTTP 500 -> Retry
Attempt 3 -> HTTP 200 -> SENT
```

The project tracks the number of attempts using a retry counter.

The current implementation allows up to **5 attempts** before marking an event as `FAILED`.

This allows the original order transaction to remain successful even if external event delivery temporarily fails.

---

## Failure Simulation

The project uses [HTTPBin](https://httpbin.org/) to simulate external HTTP behavior.

This makes it possible to test the retry mechanism without requiring another real service.

Different HTTPBin endpoints can be used to simulate successful and failed HTTP responses.

For example:

```text
HTTP 500
   |
   v
Delivery fails
   |
   v
Retry
   |
   v
HTTP 200
   |
   v
Event marked SENT
```

This is only used for demonstrating and testing the retry behavior.

---

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Java 21 | Programming language |
| Spring Boot | Application framework |
| Spring Data JPA | Database access |
| Hibernate | ORM |
| MySQL | Persistent database |
| RestTemplate | HTTP communication |
| Maven | Build and dependency management |

---

## Project Structure

The project follows a typical Spring Boot layered structure:

```text
src/
└── main/
    └── java/
        └── com/
            └── moinmankar/
                └── outboxsync/
                    ├── controller/
                    ├── entity/
                    ├── repository/
                    ├── service/
                    └── ...
```

The main responsibilities are separated into:

- **Controller** - Handles incoming HTTP requests.
- **Service** - Contains business logic and transaction handling.
- **Entity** - Represents database tables.
- **Repository** - Provides database access using Spring Data JPA.
- **Worker** - Processes pending outbox events asynchronously.

---

## Database Concept

The application uses two important pieces of data:

```text
+------------------+
|      orders      |
+------------------+
| id               |
| ...              |
+------------------+

+------------------+
|   outbox_event   |
+------------------+
| id               |
| event_type       |
| payload          |
| status           |
| retry_count      |
| created_at       |
| updated_at       |
+------------------+
```

The important relationship is that the outbox event is created as part of the same transaction that creates the order.

---

## Why Use an Outbox Table?

Without an outbox:

```text
Database
   |
   +---- Save Order
   |
   +---- Publish Event
              |
              X
           Failure
```

The database and external system can become inconsistent.

With the Outbox Pattern:

```text
Database Transaction
   |
   +---- Save Order
   |
   +---- Save Outbox Event
              |
              v
        Outbox Worker
              |
              v
      External System
```

The event is safely persisted before the application attempts delivery.

This provides a reliable mechanism for handling event publication without requiring the database transaction itself to depend on the availability of the external system.

---

## API

### Create Order

```http
POST /order/create
```

Creates an order and its corresponding outbox event within the same database transaction.

The exact request body depends on the DTO defined in the project.

After the transaction succeeds, the outbox worker is responsible for processing the generated event.

---

## Running the Project

### Prerequisites

Make sure the following are installed:

- Java 21
- Maven
- MySQL

---

### 1. Clone the Repository

```bash
git clone https://github.com/Moin-Mankar/OutBox-Sync.git
cd OutBox-Sync
```

---

### 2. Configure MySQL

Create a MySQL database for the application.

Update the database configuration in:

```text
src/main/resources/application.properties
```

Example:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/outbox_sync
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

Use your own local database credentials rather than committing credentials to the repository.

---

### 3. Build the Project

Using Maven:

```bash
./mvnw clean install
```

On Windows:

```bash
mvnw.cmd clean install
```

---

### 4. Run the Application

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The application will start using the configured Spring Boot settings.

---

## Example Event Lifecycle

When an order is created:

```text
1. Client sends order request
            |
            v
2. OrderService receives request
            |
            v
3. Database transaction starts
            |
            +---- Create Order
            |
            +---- Create ORDER_CREATED event
            |
            v
4. Transaction commits
            |
            v
5. Outbox event is PENDING
            |
            v
6. OutboxWorker detects event
            |
            v
7. Worker sends HTTP request
            |
       +----+----+
       |         |
     Success   Failure
       |         |
       v         v
     SENT      Retry
                 |
                 v
             Retry Limit
                 |
                 v
              FAILED
```

---

## What This Project Demonstrates

The main concepts demonstrated by this project are:

- Transactional Outbox Pattern
- Dual-write problem
- Database transactions
- Atomic persistence of business data and events
- Asynchronous event processing
- Scheduled background processing
- HTTP-based event delivery
- Retry handling
- Event status tracking
- Failure handling
- Eventual delivery of events

---

## Limitations

This project is a focused implementation of the Transactional Outbox Pattern and is intended primarily for learning and demonstrating the pattern.

It currently does **not** implement:

- Apache Kafka
- RabbitMQ
- Multiple independently deployed microservices
- Distributed transactions
- Dead-letter queues
- Distributed tracing
- Advanced monitoring and metrics
- Horizontal worker coordination
- Production-grade message broker delivery guarantees

The external HTTP endpoint is used to demonstrate event delivery and failure/retry behavior.

---

## Future Improvements

Possible improvements include:

- Replace HTTPBin with a real downstream service.
- Integrate a message broker such as Kafka or RabbitMQ.
- Add multiple services consuming the generated events.
- Implement dead-letter event handling.
- Add exponential backoff for retries.
- Add optimistic locking or worker coordination for concurrent processing.
- Add integration tests for transaction and retry scenarios.
- Add application metrics and monitoring.
- Containerize the application and database using Docker.

---

## Key Takeaway

Outbox-Sync demonstrates how the Transactional Outbox Pattern can be used to reliably persist business data and the events generated from that operation.

The core idea is:

```text
Business Data + Event
        |
        v
Same Database Transaction
        |
        v
Reliable Event Persistence
        |
        v
Asynchronous Processing
        |
        v
External Event Delivery
```

The project focuses on the reliability problem that occurs when database updates and external event publishing are treated as two independent operations.