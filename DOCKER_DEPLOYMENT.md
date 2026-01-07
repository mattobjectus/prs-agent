# Docker Deployment Guide

This guide explains how to manually deploy the PRS PRS Knowledge Agent application to Docker.

## Prerequisites

-   Docker installed and running
-   Docker Compose installed
-   Maven installed
-   Java 17 or later installed

## Deployment Steps

### Step 1: Build the Application

First, build the application JAR file using Maven:

```bash
mvn clean package -DskipTests
```

This will create a JAR file in the `target` directory:

-   `target/customer-support-agent-example-1.10.0-beta18.jar`

### Step 2: Build the Docker Image

Build the Docker image using docker-compose:

```bash
docker-compose build
```

Or build it manually using docker:

```bash
docker build -t customer-support-agent-example-prs-agent .
```

The Dockerfile does the following:

1. Uses maven:3.9-eclipse-temurin-17 as the build image
2. Copies the pom.xml and downloads dependencies
3. Copies the source code and builds the application
4. Creates a runtime image using eclipse-temurin:17-jre
5. Copies the built JAR file
6. Exposes port 8080
7. Runs the application

### Step 3: Deploy with Docker Compose (Recommended)

The easiest way to deploy is using docker-compose, which handles both Redis and the application:

```bash
docker-compose up -d
```

This command:

-   Creates a custom network (`prs-network`)
-   Starts a Redis container (`prs-redis`) on port 6379
-   Starts the application container (`prs-agent`) on port 8080
-   The application waits for Redis to be healthy before starting

### Step 4: Verify the Deployment

Check that both containers are running:

```bash
docker ps
```

You should see two containers:

-   `prs-redis` - The Redis database
-   `prs-agent` - The application

View the logs:

```bash
# View application logs
docker logs prs-agent

# Follow application logs in real-time
docker logs -f prs-agent

# View Redis logs
docker logs prs-redis
```

### Step 5: Access the Application

Once deployed, access the application at:

**http://localhost:8080/**

The root URL will automatically redirect to the chat interface at `/index.html`.

## Manual Deployment (Without Docker Compose)

If you prefer to deploy manually without docker-compose:

### 1. Create a Docker Network

```bash
docker network create prs-network
```

### 2. Start Redis

```bash
docker run -d \
  --name prs-redis \
  --network prs-network \
  -p 6379:6379 \
  redis:latest
```

### 3. Start the Application

```bash
docker run -d \
  --name prs-agent \
  --network prs-network \
  -p 8080:8080 \
  -e SPRING_DATA_REDIS_HOST=prs-redis \
  -e SPRING_DATA_REDIS_PORT=6379 \
  customer-support-agent-example-prs-agent
```

## Environment Variables

The application uses the following environment variables (configured in docker-compose.yml):

-   `SPRING_DATA_REDIS_HOST` - Redis host (default: `prs-redis`)
-   `SPRING_DATA_REDIS_PORT` - Redis port (default: `6379`)
-   `XAI_API_KEY` - X.AI API key (set in your environment or .env file)
-   `FIRECRAWL_API_KEY` - Firecrawl API key (optional, for web crawling)

## Stopping the Application

### Using Docker Compose

```bash
# Stop containers
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Manual Cleanup

```bash
# Stop containers
docker stop prs-agent prs-redis

# Remove containers
docker rm prs-agent prs-redis

# Remove network
docker network rm prs-network
```

## Redeployment After Code Changes

When you make changes to your code and want to redeploy:

### Quick Redeploy with Docker Compose

```bash
# 1. Build the application
mvn clean package -DskipTests

# 2. Rebuild the Docker image
docker-compose build

# 3. Stop existing containers
docker-compose down

# 4. Start new containers
docker-compose up -d
```

### Or use a single command

```bash
# Build, stop, and restart in one command
mvn clean package -DskipTests && docker-compose build && docker-compose down && docker-compose up -d
```

## Troubleshooting

### Application won't start

Check the logs:

```bash
docker logs prs-agent
```

Common issues:

-   Redis not ready: Wait for Redis health check to pass
-   Missing API keys: Ensure XAI_API_KEY is set
-   Port conflict: Make sure port 8080 is not in use

### Redis connection issues

Check Redis is running:redis-cli

```bash
docker ps | grep redis
```

Test Redis connection:

```bash
docker exec -it prs-redis redis-cli ping
# Should return
```

### View application environment

```bash
docker exec prs-agent envmate
```

### Restart containers

```bashls
# Restart application only
docker restart prs-agent

# Restart everything
docker-compose restart
```

## Health Checks

The docker-compose configuration includes health checks:

-   **Redis**: Checked every 5 seconds with `redis-cli ping`
-   **Application**: Waits for Redis to be healthy before starting

## Ports

-   **8080**: Application web interface
-   **6379**: Redis database (exposed to host for debugging)

## Data Persistence

Redis data is stored in a Docker volume to persist between container restarts. To clear all data:

```bash
docker-compose down -v
```

## Production Considerations

For production deployment, consider:

1. **Environment Variables**: Use a `.env` file or secrets management
2. **Resource Limits**: Add CPU/memory limits in docker-compose.yml
3. **Logging**: Configure log aggregation (ELK stack, CloudWatch, etc.)
4. **Monitoring**: Add health check endpoints and monitoring
5. **Scaling**: Use Docker Swarm or Kubernetes for scaling
6. **Security**:
    - Don't expose Redis port publicly
    - Use SSL/TLS for web traffic
    - Scan images for vulnerabilities
7. **Backup**: Regular Redis database backups
8. **Updates**: Implement rolling updates strategy

## Development vs Production

This setup is optimized for development. For production:

-   Use managed Redis (AWS ElastiCache, Redis Cloud, etc.)
-   Configure proper logging and monitoring
-   Implement load balancing
-   Use container orchestration (Kubernetes, ECS, etc.)
-   Implement proper secrets management
-   Add reverse proxy (nginx, Traefik) with SSL
