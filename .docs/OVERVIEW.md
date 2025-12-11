# Routemate - Lightweight Database Router

## 🚀 Introduction
**Routemate** (SimpleDB-Router) is a lightweight, zero-learning-curve database routing library for Spring Boot applications. It allows developers to implement **Read/Write splitting** (Master/Slave replication) in 5 minutes with just a few lines of configuration.

Unlike complex solutions like ShardingSphere, Routemate focuses on the 80% most common use cases with 5% of the complexity.

## ✨ Key Features (Implemented)

### 1. Zero-Code Read/Write Splitting
- Automatically detects `@Transactional(readOnly = true)` and routes queries to **Read Replicas**.
- Routes standard `@Transactional` (readOnly = false) or non-transactional queries to the **Master DB**.
- Powered by Spring AOP and `AbstractRoutingDataSource`.

### 2. Multi-DataSource Management
- Configure multiple data sources purely via `application.yml`.
- Supports one Master and multiple Read Replicas.

### 3. Load Balancing
- Built-in **Round-Robin** strategy for distributing traffic among multiple Read Replicas.
- Automatically handles fallback to Master if no Read Replicas are configured (basic fallback).

### 4. Spring Boot Starter
- **Auto-Configuration**: Just add the dependency, and it works.
- **Properties binding**: Type-safe configuration via `routemate.*`.

## 🛠 Architecture
- **RoutingAspect**: Intercepts method calls to determine Read/Write intent.
- **RoutingContext**: Uses `ThreadLocal` to propagate the current intent (READ/WRITE) to the DataSource.
- **DataSourceRouter**: Extends `AbstractRoutingDataSource` to dynamically switch JDBC Connections based on the context.

## 📦 Usage
Add the starter dependency:
```groovy
implementation project(':routemate-spring-boot-starter')
```

Configure `application.yml`:
```yaml
routemate:
  enabled: true
  default-datasource: master
  datasources:
    master:
      url: jdbc:mysql://...
    read-1:
      url: jdbc:mysql://...
  routing:
    read-datasources:
      - read-1
```
