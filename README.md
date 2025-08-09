A distributed caching system implementation with eventual consistency using vector clocks.

## Features

- In-memory distributed caching
- REST API for cache operations
- Peer-to-peer communication
- Eventual consistency with vector clocks
- Leader/follower architecture
- Docker containerization

## Architecture

The system uses a distributed architecture where each node:
- Maintains its own cache copy
- Communicates updates to peers
- Uses vector clocks for conflict resolution
- Supports leader/follower configuration

### Eventual Consistency

The cache implements eventual consistency using vector clocks to:
- Track causal relationships between updates
- Resolve conflicts across distributed nodes
- Ensure all nodes eventually converge to the same state

## Getting Started

## Configuration
The system can be configured using environment variables:
- `PORT`: The port on which the node listens (default: 8080)
- `PEER_NODES`: Comma-separated list of peer node addresses (e.g., `localhost:8081,localhost:8082`)
- `LEADER`: Set to `true` if the node is a leader (default: `false`)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose

## Project Structure
```
distributed-cache-implementation/
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
This will start 3 nodes:


Node1 (Leader): http://localhost:8081
Node2 (Follower): http://localhost:8082
Node3 (Follower): http://localhost:8083

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

