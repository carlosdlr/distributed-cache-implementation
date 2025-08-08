# Distributed Cache Implementation

A simple distributed cache implementation using Java 17 and HTTP communication between nodes.

## Features

- In-memory caching with `ConcurrentHashMap`
- HTTP-based node synchronization
- Docker support for multi-node testing
- Modern Java features (Records, Streams, Optional)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose

## Project Structure
```
distributed-cache-impl/
│
├── src/                    # Source files
│   └── main/               # Main source code
│       └── java/           # Java source files
│           └── com/        # Base package
│               └── jikkosoft/  # Company/org package
│                   ├── Main.java          # Main application class
│                   └── cache/             # Cache implementation
│                       ├── impl/          # Implementation classes
│                       │   └── DistributedCache.java  # Main cache class
│                       └── model/         # Data models
│                           └── CacheUpdate.java  # Cache update model
│
├── docker-compose.yml      # Docker compose configuration
├── Dockerfile              # Docker container definition
└── pom.xml                 # Maven project configuration
```

## Building the Project

```bash
mvn clean package
```

## Building the Project
Start three cache nodes using Docker Compose:
```bash
docker-compose up --build
```
This will create three nodes listening on ports 8081, 8082, and 8083

## Usage Example

### Basic Usage
```java
// Initialize cache with port and peer nodes
List<String> peers = List.of("localhost:8082", "localhost:8083");
DistributedCache cache = new DistributedCache(8081, peers);

// Store value in cache
cache.put("user_1", "My Value");

// Retrieve value from cache
cache.get("user_1").ifPresent(value -> 
    System.out.println("Retrieved: " + value)
);
```
Using curl to interact with the cache nodes:
```bash
# Store value in node 1
curl -X POST -H "Content-Type: application/json" \
     -d '{"key":"test_key","value":"test_value"}' \
     http://localhost:8081/update

# Verify value in node 2
curl "http://localhost:8082/get?key=test_key"

# Verify value in node 3
curl "http://localhost:8083/get?key=test_key"
```