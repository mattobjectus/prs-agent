# Secrets Management Guide

This guide explains how to manage secrets (API keys, credentials) for the PRS Knowledge Agent application.

## 🔐 Overview

The application requires the `AI_API_KEY` to authenticate with the Grok AI service (xAI). This secret must be kept secure and never committed to version control.

## 📁 File Structure

```
prs-knowledge-agent/
├── .env                 # Contains actual secrets (NOT committed to git)
├── .env.example         # Template for environment variables (committed)
└── .gitignore          # Ensures .env is never committed
```

## 🚀 Quick Setup

### 1. Create .env File

The `.env` file has already been created with a placeholder value:

```bash
AI_API_KEY=XXX_SECRET_KEY_XXX
```

**⚠️ IMPORTANT**: Replace `XXX_SECRET_KEY_XXX` with your actual xAI API key:

```bash
AI_API_KEY=xai-your-actual-api-key-here
```

### 2. Verify .gitignore

Ensure `.env` is listed in `.gitignore`:

```bash
# .gitignore
.env
.env.local
*.key
*.pem
```

This prevents accidentally committing secrets to git.

## 🔧 Usage Methods

### Method 1: Docker Compose (Recommended)

Docker Compose automatically loads `.env`:

```yaml
# docker-compose.yml
services:
    prs-agent:
        env_file:
            - .env # Loads AI_API_KEY automatically
        environment:
            - REDIS_URL=redis://host.docker.internal:6379
            # ... other vars
```

**Run:**

```bash
docker-compose up -d --build
```

The `AI_API_KEY` from `.env` will be available as an environment variable in the container.

### Method 2: VS Code Debug

The `.vscode/launch.json` file is already configured:

```json
{
    "configurations": [
        {
            "name": "Debug PRS Knowledge Agent",
            "env": {
                "AI_API_KEY": "XXX_SECRET_KEY_XXX",
                "REDIS_HOST": "localhost",
                "REDIS_PORT": "6379"
            }
        }
    ]
}
```

**Steps:**

1. Update `AI_API_KEY` value in `.vscode/launch.json` with your real key
2. Press F5 or click "Debug PRS Knowledge Agent"

**Note**: `.vscode/` is in `.gitignore`, so your key won't be committed.

### Method 3: Command Line Export

#### macOS/Linux (bash/zsh):

```bash
# Load from .env
export $(cat .env | xargs)

# Or set manually
export AI_API_KEY=xai-your-actual-key-here
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Run application
mvn spring-boot:run
```

#### Windows (PowerShell):

```powershell
# Read from .env
Get-Content .env | ForEach-Object {
    $var = $_.Split('=')
    [Environment]::SetEnvironmentVariable($var[0], $var[1], "Process")
}

# Or set manually
$env:AI_API_KEY="xai-your-actual-key-here"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"

# Run application
mvn spring-boot:run
```

#### Windows (CMD):

```cmd
REM Set manually
set AI_API_KEY=xai-your-actual-key-here
set REDIS_HOST=localhost
set REDIS_PORT=6379

REM Run application
mvn spring-boot:run
```

### Method 4: .env File (Alternative)

You can also use a `.env` file instead of `secrets.txt`:

```bash
# Copy the example
cp .env.example .env

# Edit .env with your actual values
AI_API_KEY=xai-your-actual-key-here
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_URL=redis://localhost:6379
```

Load it before running:

```bash
# Using source
export $(cat .env | xargs)
mvn spring-boot:run

# Or with direnv (auto-loading)
direnv allow .
mvn spring-boot:run
```

### Method 5: IntelliJ IDEA

#### Create Run Configuration:

1. **Open Run/Debug Configurations**

    - Go to `Run` → `Edit Configurations...`

2. **Create Spring Boot Configuration**

    - Click `+` → `Spring Boot`
    - Name: "PRS Knowledge Agent"
    - Main class: `org.musser.prsknowledgeagent.PRSKnowledgeAgentApplication`

3. **Set Environment Variables**

    - Find "Environment variables" section
    - Click folder icon
    - Add:
        ```
        AI_API_KEY=xai-your-actual-key-here
        REDIS_HOST=localhost
        REDIS_PORT=6379
        ```

4. **Save and Run/Debug**

## 🔍 How Application Reads Secrets

The application reads secrets through environment variables:

### application.properties

```properties
# Uses AI_API_KEY environment variable
langchain4j.open-ai.chat-model.api-key=${AI_API_KEY:unknown}

# Falls back to localhost if not set
redis.url=${REDIS_URL:redis://localhost:6379}
```

### Precedence Order

1. Environment variables (highest priority)
2. .env file (via docker-compose)
3. .env file (if loaded manually)
4. Default values in application.properties

## 🛡️ Security Best Practices

### ✅ DO:

-   ✅ Keep `.env` in `.gitignore`
-   ✅ Use different keys for development/production
-   ✅ Rotate API keys regularly
-   ✅ Use `.env.example` as a template (committed to git)
-   ✅ Use environment-specific files: `.env.dev`, `.env.prod`
-   ✅ Document required secrets in README
-   ✅ Use secrets management tools in production (AWS Secrets Manager, HashiCorp Vault)

### ❌ DON'T:

-   ❌ Commit `.env` to git
-   ❌ Share secrets in Slack, email, or documentation
-   ❌ Use production keys in development
-   ❌ Hardcode API keys in source code
-   ❌ Log or print API keys
-   ❌ Share your actual API keys with others

## 🔄 Rotating Secrets

When you need to update your API key:

### For Local Development:

1. Update `.env`:

    ```bash
    AI_API_KEY=xai-new-key-here
    ```

2. Restart the application

### For Docker:

1. Update `.env`
2. Restart containers:
    ```bash
    docker-compose down
    docker-compose up -d
    ```

### For VS Code:

1. Update `.vscode/launch.json`
2. Restart debug session

## 📋 Troubleshooting

### Secret Not Loading

**Symptom**: Application fails with "unknown" API key

**Solutions**:

```bash
# Check if environment variable is set
echo $AI_API_KEY  # Linux/macOS
echo %AI_API_KEY%  # Windows CMD
echo $env:AI_API_KEY  # Windows PowerShell

# Verify .env exists
cat .env

# Check if it's loaded in Docker
docker exec prs-agent env | grep AI_API_KEY
```

### Permission Denied

**Symptom**: Cannot read .env

**Solution**:

```bash
# Fix permissions
chmod 600 .env  # Read/write for owner only
```

### Git Commit Warning

**Symptom**: Git tries to commit .env

**Solution**:

```bash
# Verify gitignore
cat .gitignore | grep "^\.env$"

# If missing, add it
echo ".env" >> .gitignore

# Remove from git if already committed
git rm --cached .env
git commit -m "Remove .env from version control"
```

## 📚 Example Workflows

### Workflow 1: First Time Setup

```bash
# 1. Clone repository
git clone <repo-url>
cd prs-knowledge-agent

# 2. Copy environment template
cp .env.example .env

# 3. Edit with your API key
nano .env
# Change AI_API_KEY=XXX_SECRET_KEY_XXX to your actual key

# 4. Start Redis
redis-stack-server --daemonize yes

# 5. Run application
export $(cat .env | xargs)
mvn spring-boot:run
```

### Workflow 2: Docker Deployment

```bash
# 1. Verify .env has correct key
cat .env

# 2. Build and run with Docker Compose
docker-compose up -d --build

# 3. Verify secret is loaded
docker exec prs-agent env | grep AI_API_KEY
```

### Workflow 3: Team Development

```bash
# Developer A (creates template)
echo "AI_API_KEY=your-key-here" > .env.example
git add .env.example
git commit -m "Add environment template"
git push

# Developer B (sets up their environment)
git pull
cp .env.example .env
# Edit .env with their own key
# .env is NOT committed due to .gitignore
```

## 🎯 Production Deployment

For production, use proper secrets management:

### AWS Secrets Manager

```bash
# Store secret
aws secretsmanager create-secret \
    --name prs-agent/ai-api-key \
    --secret-string "xai-production-key"

# Retrieve in application startup script
export AI_API_KEY=$(aws secretsmanager get-secret-value \
    --secret-id prs-agent/ai-api-key \
    --query SecretString \
    --output text)
```

### Docker Secrets

```yaml
# docker-compose.yml (production)
version: "3.8"
services:
    prs-agent:
        secrets:
            - ai_api_key
        environment:
            - AI_API_KEY_FILE=/run/secrets/ai_api_key

secrets:
    ai_api_key:
        file: ./secrets/ai_api_key.txt
```

### Kubernetes Secrets

```yaml
# Create secret
kubectl create secret generic prs-agent-secrets \
    --from-literal=ai-api-key=xai-production-key

# Use in deployment
env:
    - name: AI_API_KEY
      valueFrom:
          secretKeyRef:
              name: prs-agent-secrets
              key: ai-api-key
```

## 📞 Getting API Keys

### xAI (Grok) API Key

1. Visit [https://console.x.ai/](https://console.x.ai/)
2. Sign up or log in
3. Navigate to API Keys section
4. Create new API key
5. Copy and save securely

### Redis (Optional Authentication)

If using Redis with authentication:

```bash
# In secrets.txt
REDIS_PASSWORD=your-redis-password
REDIS_URL=redis://:your-redis-password@localhost:6379
```

## 📖 Additional Resources

-   [12-Factor App: Config](https://12factor.net/config)
-   [OWASP Secrets Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Secrets_Management_Cheat_Sheet.html)
-   [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)

---

**Remember**: Treat your API keys like passwords. Never share them, never commit them, and rotate them regularly!
