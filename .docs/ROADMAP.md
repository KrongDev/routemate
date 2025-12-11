# 🗺️ Routemate Roadmap

This document outlines the development plan and upcoming features for Routemate.

## ✅ Phase 1: Core Foundation (Completed)
- [x] Multi-module Gradle Project Structure (`core`, `starter`, `examples`)
- [x] `DataSourceRouter` Implementation (AbstractRoutingDataSource)
- [x] `RoutingContext` (ThreadLocal based context)
- [x] Spring AOP Integration (`@Transactional` support)
- [x] Spring Boot Auto-Configuration
- [x] Basic Round-Robin Load Balancing for Read Replicas

## 🚧 Phase 2: Reliability & Resilience (In Progress)
The current implementation assumes all databases are always up. We need to add resilience.

### 1. Health Checks & Automatic Fallback (Completed)
- [x] Implement background Health Checker thread (`DataSourceHealthChecker`).
- [x] Logic to bypass offline replicas in `DataSourceRouter`.
- [x] Fallback to Master if ALL read replicas are down.
- [x] Configurable properties (`enabled`, `interval`, `timeout`).

### 2. Connection Pool Optimization (Completed)
- [x] Allow distinct pool settings for Master vs Read (HikariCP config per datasource).

## 🔮 Phase 3: Advanced Load Balancing (Completed)
- [x] **Strategy Pattern**: Converted internal logic to `LoadBalancer` interface.
- [x] **Round-Robin**: Thread-safe implementation using `AtomicInteger`.
- [x] **Random**: `RandomLoadBalancer` using `ThreadLocalRandom`.
- [x] **Weighted Round-Robin**: `WeightedRoundRobinLoadBalancer` with configurable weights.
- [x] **Configuration**: Selectable via `routemate.routing.load-balance-strategy`.

## 🧩 Phase 4: Dynamic Features (Completed)
- [x] **Dynamic DataSource Management**: Add/Remove replicas at runtime.
- [x] **REST API**: Secure API to manage data sources (`routemate.management.enabled`).
- [x] **Duration Config**: Improved configuration using `java.time.Duration`.

## 🛡️ Phase 5: Observability & Resilience (Next Steps)
- [ ] **Circuit Breaker**: Failure threshold for health checks.

## 🧪 Testing & Quality
- **Integration Tests**: Verify with Testcontainers (MySQL/PostgreSQL containers).
- **Performance Benchmarking**: JMH tests to ensure routing overhead is < 5%.
