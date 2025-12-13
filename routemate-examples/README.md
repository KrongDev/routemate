# Routemate Examples

This module contains example applications demonstrating how to use Routemate.

## MySQL Example

This example demonstrates master-slave routing using MySQL.

### Prerequisites

- Docker and Docker Compose installed.
- Java 17+ installed.

### Setup

1. **Start Database Infrastructure**

   Start the Master and Slave MySQL instances using Docker Compose:

   ```bash
   docker-compose up -d
   ```

   This will start:
   - Master: localhost:3306
   - Slave 1: localhost:3307
   - Slave 2: localhost:3308

2. **Run the Application**

   Run the Spring Boot application with the `mysql` profile:

   ```bash
   ./gradlew :routemate-examples:bootRun --args='--spring.profiles.active=mysql'
   ```

   *Note: Ensure you are in the root directory.*

### Testing

You can test the API using curl or Postman.

1. **Create User (Write -> Master)**

   ```bash
   curl -X POST "http://localhost:8080/users?name=JohnDoe&email=john@example.com"
   ```

2. **Get Users (Read -> Slaves)**

   ```bash
   curl http://localhost:8080/users
   ```

   Check the logs to see if the connection is routed to the slaves. Since we are using 3 independent databases for this example (no real replication), if you write to Master (User created), you might NOT see it in Slaves immediately unless you manually sync or simpler:
   
   **Verification of Routing:**
   - Check the application logs.
   - Or, manually insert data into Slave 1 via MySQL Client:
     ```sql
     -- In Slave 1 (port 3307)
     INSERT INTO users (name, email) VALUES ('Slave1User', 'slave1@test.com');
     ```
   - Then query the API. If you see 'Slave1User', it routed to Slave 1.

### Cleanup

To stop the databases:

```bash
docker-compose down
```
