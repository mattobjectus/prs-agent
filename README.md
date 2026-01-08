# PRS Knowledge Agent

A Spring Boot application that uses LangChain4j and AI to provide intelligent responses about PRS Guitars based on ingested documentation and forum content. The application uses RAG (Retrieval-Augmented Generation) with Redis as a vector store for embeddings.

Currently it is being hosted here: https://prs-agent-1089045512594.us-west1.run.app

## 🎯 Overview

The PRS Knowledge Agent is an AI-powered chatbot that can answer questions about PRS Guitars. It uses:

-   **LangChain4j** for AI orchestration
-   **Grok (xAI)** as the LLM provider
-   **Redis Stack** with RediSearch for vector storage
-   **AllMiniLmL6V2 Quantized** embedding model for document embeddings
-   **Spring Boot** as the application framework

## 🏗️ Architecture

```
┌─────────────┐      ┌──────────────────┐      ┌──────────────┐
│   User      │─────▶│  Spring Boot     │─────▶│   Grok API   │
│  (Browser)  │      │   Application    │      │    (xAI)     │
└─────────────┘      └──────────────────┘      └──────────────┘
                              │
                              ▼
                     ┌──────────────────┐
                     │  Redis Stack     │
                     │  (Vector Store)  │
                     └──────────────────┘
```

### How It Works

1. **Document Ingestion**: On startup, the application loads PRS documentation from `src/main/resources/prs-docs.txt`
2. **Text Splitting**: Documents are split into 13,720 text segments
3. **Embedding Generation**: Each segment is embedded using the AllMiniLmL6V2 model (384 dimensions)
4. **Vector Storage**: Embeddings are stored in Redis with RediSearch for efficient similarity search
5. **Query Processing**: User questions are embedded and matched against stored vectors
6. **Response Generation**: Relevant context is retrieved and sent to Grok for answer generation

## 📋 Prerequisites

-   **Java 17** or higher
-   **Maven 3.9+**
-   **Docker** (optional, for containerized deployment)
-   **Redis Stack** (with RediSearch module)

## 🚀 Quick Start

### Option 1: Run Locally

#### 1. Start Redis Stack

```bash
redis-stack-server --daemonize yes
```

Verify Redis is running:

```bash
redis-cli ping
# Should return: PONG
```

#### 2. Configure Application

The application uses environment variables with sensible defaults. By default, it connects to `localhost:6379`.

#### 3. Run the Application

```bash
# Using Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/prs-knowledge-agent-0.10.0.jar
```

#### 4. Run in Debug Mode

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Then attach your debugger to port **5005**.

### Option 2: Run with Docker

#### 1. Start Local Redis Stack

```bash
redis-stack-server --daemonize yes
```

#### 2. Build and Run Container

```bash
docker-compose up -d --build
```

This will:

-   Build the Docker image
-   Start the application on ports 80 and 8080
-   Connect to Redis on the host machine via `host.docker.internal:6379`

#### 3. View Logs

```bash
docker logs prs-agent --tail 50 -f
```

#### 4. Stop the Application

```bash
docker-compose down
```

## 🔧 Configuration

### Application Properties

Key configuration in `src/main/resources/application.properties`:

```properties
# AI Model Configuration
langchain4j.open-ai.chat-model.api-key=<your-xai-api-key>
langchain4j.open-ai.chat-model.model-name=grok-3
langchain4j.open-ai.chat-model.base-url=https://api.x.ai/v1
langchain4j.open-ai.chat-model.temperature=0.0
langchain4j.open-ai.chat-model.max-tokens=5000

# Redis Configuration (with environment variable fallbacks)
redis.url=${REDIS_URL:redis://localhost:6379}
langchain4j.community.redis.port=${REDIS_PORT:6379}
langchain4j.community.redis.host=${REDIS_HOST:localhost}
langchain4j.community.redis.dimension=384

# Logging
logging.level.dev.langchain4j=DEBUG
```

### Environment Variables

| Variable     | Description          | Default                      |
| ------------ | -------------------- | ---------------------------- |
| `REDIS_URL`  | Redis connection URL | `redis://localhost:6379`     |
| `REDIS_HOST` | Redis hostname       | `localhost`                  |
| `REDIS_PORT` | Redis port           | `6379`                       |
| `JAVA_OPTS`  | JVM options          | `-Xms1g -Xmx4g -XX:+UseG1GC` |

## 📱 Usage

### Web Interface

Once the application is running, access the chat interface at:

-   **http://localhost:8080** (local or Docker)
-   **http://localhost:80** (Docker only)

### Example Questions

Try asking questions like:

-   "What are the features of PRS guitars?"
-   "Tell me about PRS core models"
-   "What is the difference between PRS S2 and CE series?"
-   "What pickups do PRS guitars use?"

## 🔍 API Endpoints

### Chat Endpoint

```bash
POST http://localhost:8080/chat
Content-Type: application/json

{
  "message": "What are PRS guitars known for?"
}
```

### Health Check

```bash
GET http://localhost:8080/actuator/health
```

## 🛠️ Development

### Project Structure

```
prs-knowledge-agent/
├── src/
│   ├── main/
│   │   ├── java/org/musser/prsknowledgeagent/
│   │   │   ├── PRSKnowledgeAgent.java          # AI Service interface
│   │   │   ├── PRSKnowledgeAgentApplication.java
│   │   │   ├── PRSKnowledgeAgentConfiguration.java  # Bean configuration
│   │   │   ├── PRSKnowledgeAgentController.java     # REST endpoints
│   │   │   ├── DataPopulation.java             # Data ingestion
│   │   │   ├── EmbeddingInitializationService.java
│   │   │   ├── PrsCrawler.java                 # Web scraping (optional)
│   │   │   └── RedisChatMemoryStore.java       # Chat memory
│   │   └── resources/
│   │       ├── application.properties           # Configuration
│   │       ├── prs-docs.txt                    # PRS documentation
│   │       └── static/                         # Web UI
│   │           ├── index.html
│   │           └── css/chat.css
├── docker-compose.yml                          # Docker deployment
├── Dockerfile                                  # Container image
└── pom.xml                                     # Maven dependencies
```

### Key Components

-   **PRSKnowledgeAgent**: LangChain4j AI Service interface with system prompts
-   **EmbeddingModel**: AllMiniLmL6V2QuantizedEmbeddingModel (384 dimensions)
-   **Vector Store**: Redis with RediSearch for similarity search
-   **Chat Memory**: Redis-backed conversation history per user
-   **Data Ingestion**: Automatic on startup via `@EventListener(ApplicationReadyEvent.class)`

### Adding New Documentation

1. Add content to `src/main/resources/prs-docs.txt`
2. Restart the application
3. Documents will be automatically re-ingested and embedded

## 🐛 Troubleshooting

### Redis Connection Issues

If you see "Failed to create socket" or "Connection refused":

```bash
# Check if Redis is running
redis-cli ping

# Start Redis if needed
redis-stack-server --daemonize yes

# Check Redis logs
tail -f /opt/homebrew/var/log/redis-stack.log  # macOS with Homebrew
```

### Port Already in Use

If port 8080 is already in use:

```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>
```

### Out of Memory Errors

Increase JVM memory in `docker-compose.yml`:

```yaml
JAVA_OPTS: "-Xms2g -Xmx8g -XX:+UseG1GC"
```

### Embedding Generation is Slow

The first startup takes ~25 seconds to embed 13,720 text segments. Subsequent startups are faster as embeddings are cached in Redis. This has been delegated to a background thread during start up so the service is available
but may take a half a minute to get a better brain so to speak.
The optimal version of this app uses a redis cache to store all the additional knowledge in and therefore
it is persisted and start up becomes very fast. The amount of data exceeds and free tier redis hosting
so we are not using it.

## 📊 Performance

-   **Embedding Model**: 384-dimensional vectors (AllMiniLmL6V2-Q)
-   **Document Segments**: 13,720 chunks
-   **Embedding Time**: ~25 seconds on first run
-   **Query Response**: < 2 seconds (including vector search + LLM)
-   **Memory Usage**: ~2-4GB heap recommended

## 🔐 Security Notes

-   **API Keys**: Never commit API keys to version control
-   **Production**: Use environment variables or secrets management
-   **Redis**: Consider enabling authentication in production
-   **Network**: Restrict Docker network access in production

## 📄 License

This project is part of the LangChain4j examples repository.

## 🤝 Contributing

Contributions are welcome! Please ensure:

-   Code follows existing style
-   Tests pass
-   Documentation is updated

## 📚 Resources

-   [LangChain4j Documentation](https://docs.langchain4j.dev/)
-   [Redis Stack Documentation](https://redis.io/docs/stack/)
-   [Grok API Documentation](https://docs.x.ai/)
-   [Spring Boot Documentation](https://spring.io/projects/spring-boot)

## ✨ Features

-   ✅ AI-powered chat with PRS guitar knowledge
-   ✅ Vector similarity search with Redis
-   ✅ Persistent chat memory per user
-   ✅ Real-time streaming responses
-   ✅ Docker deployment support
-   ✅ Debug mode for development
-   ✅ Automatic document ingestion
-   ✅ Responsive web interface

## 🎓 Learning Resources

This project demonstrates:

-   RAG (Retrieval-Augmented Generation) pattern
-   Vector embeddings and similarity search
-   LangChain4j AI orchestration
-   Redis as a vector database
-   Spring Boot with AI integration
-   Docker containerization

---

**Built with ❤️ using LangChain4j, Spring Boot, and Redis Stack**
