# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl redisson-cache-consistency
mvn clean install -pl redisson-hyperloglog-visit

# Run tests
mvn test

# Run single test class
mvn test -pl redisson-cache-consistency -Dtest=ProductControllerTest
mvn test -pl redisson-hyperloglog-visit -Dtest=VisitControllerTest

# Run specific test method
mvn test -pl redisson-cache-consistency -Dtest=ProductControllerTest#testGetProduct_Success

# Run application
mvn spring-boot:run -pl redisson-cache-consistency
mvn spring-boot:run -pl redisson-hyperloglog-visit
```

## Architecture Overview

This is a multi-module Maven project demonstrating Redisson (Redis client) usage patterns with Spring Boot 3.2.4 and Java 17.

### Modules

**redisson-cache-consistency** (port 8080)
- Demonstrates cache consistency patterns using Redisson RReadWriteLock
- Product CRUD with cache-aside pattern: read-lock for cache hits, write-lock for cache misses/updates
- Implements cache deletion with retry mechanism on database updates
- Uses null-object pattern with short TTL to prevent cache penetration
- Adds TTL jitter to prevent cache avalanche

**redisson-hyperloglog-visit** (port 8081)
- Demonstrates Redis HyperLogLog for UV (unique visitor) statistics
- Uses RHyperLogLog for memory-efficient cardinality counting
- ~0.81% standard error acceptable for large-scale visit tracking

### Common Patterns

- Both modules connect to Redis at `redis://host.docker.internal:6379`
- Entity classes implement `Serializable` for Redis storage
- Lombok `@Data`, `@Slf4j` for boilerplate reduction
- Constructor injection for dependencies
- All tests use Mockito for service layer mocking
