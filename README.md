# Routemate

## Why Routemate?

Most Spring Boot applications don't need full-blown sharding frameworks.
They just need a **safe, predictable way to separate reads and writes**.

**Routemate** is a lightweight database routing library designed for this exact need.
It focuses on the 80% most common read/write separation use cases, without introducing
new infrastructure, SQL rewriting, or operational complexity.

If you've ever thought:
- "ShardingSphere is too heavy for this"
- "I just want read replicas, not a distributed database"
- "I want something that behaves like Spring, not replaces it"

Then Routemate is for you.

## Features

### Core Routing
- **Zero-Code Read/Write Splitting**
- **Multi-Read Replica Support**

### Load Balancing
- Round-Robin (Default)
- Random
- Weighted Round-Robin

### Reliability
- Background Health Checks
- Automatic Master Fallback

### Operational Flexibility
- Programmatic DataSource Management
- Per-DataSource HikariCP Tuning

> With Routemate, you don't change your code.
> You just annotate your transactions correctly.

## Quick Start

### Prerequisites
- Java 17+
- Spring Boot 3.x
- Docker & Docker Compose (for running the examples)

### Installation
Add the dependency to your `build.gradle` (assuming published or local project):

```groovy
dependencies {
    implementation 'io.github.krongdev:routemate-spring-boot-starter:1.0.1'
}
```

### Configuration
Configure your data sources in `application.yml`:

```yaml
spring:
  datasource:
    # Master DataSource (Standard Spring Boot config)
    url: jdbc:mysql://localhost:3306/mydb
    username: user
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

routemate:
  enabled: true
  
  # Configure Read Replicas
  reads:
    slave-1:
      url: jdbc:mysql://localhost:3307/mydb
      username: read_user
      password: read_password
      weight: 1
    slave-2:
      url: jdbc:mysql://localhost:3308/mydb
      username: read_user
      password: read_password
      weight: 2
      
  # Routing Strategy
  routing:
    load-balance-strategy: weighted-round-robin
```

### Usage
Simply use Spring's standard `@Transactional` annotation. Routemate handles the rest.

```java
@Service
public class UserService {

    private final UserRepository userRepository;

    // Routes to Master (Write)
    @Transactional
    public User createUser(String name) {
        return userRepository.save(new User(name));
    }

    // Routes to Read Replica (Slave)
    @Transactional(readOnly = true)
    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }
}
```
