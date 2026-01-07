# Debug Setup Guide

This guide explains how to set environment variables for debugging the PRS Knowledge Agent application.

## 🎯 Environment Variables

The application uses these key environment variables:

| Variable     | Description          | Default                  | Example                  |
| ------------ | -------------------- | ------------------------ | ------------------------ |
| `REDIS_URL`  | Redis connection URL | `redis://localhost:6379` | `redis://localhost:6379` |
| `REDIS_HOST` | Redis hostname       | `localhost`              | `localhost`              |
| `REDIS_PORT` | Redis port           | `6379`                   | `6379`                   |
| `JAVA_OPTS`  | JVM options          | See below                | `-Xms2g -Xmx4g`          |

## 🔧 Setting Environment Variables

### 1. Command Line (Maven)

#### macOS/Linux (bash/zsh):

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
mvn spring-boot:run
```

#### Windows (PowerShell):

```powershell
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
mvn spring-boot:run
```

#### Windows (CMD):

```cmd
set REDIS_HOST=localhost
set REDIS_PORT=6379
mvn spring-boot:run
```

### 2. Maven Command with Debug Port

```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
  -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### 3. IntelliJ IDEA Setup

#### Option A: Run Configuration

1. **Open Run/Debug Configurations**

    - Go to `Run` → `Edit Configurations...`

2. **Create New Spring Boot Configuration**

    - Click `+` → `Spring Boot`
    - Name: "PRS Knowledge Agent Debug"
    - Main class: `org.musser.prsknowledgeagent.PRSKnowledgeAgentApplication`

3. **Set Environment Variables**

    - In the configuration dialog, find "Environment variables"
    - Click the folder icon to open the Environment Variables dialog
    - Add variables:
        ```
        REDIS_HOST=localhost
        REDIS_PORT=6379
        SPRING_PROFILES_ACTIVE=dev
        ```

4. **Enable Debug Mode**
    - Check "Debug" mode when running
    - Or click the debug icon (🐛) instead of run

#### Option B: `.run` Configuration File

Create `.run/PRSKnowledgeAgent-Debug.run.xml` in your project:

```xml
<component name="ProjectRunConfigurationManager">
  <configuration default="false" name="PRS Knowledge Agent Debug" type="SpringBootApplicationConfigurationType">
    <option name="ACTIVE_PROFILES" value="dev" />
    <envs>
      <env name="REDIS_HOST" value="localhost" />
      <env name="REDIS_PORT" value="6379" />
      <env name="REDIS_URL" value="redis://localhost:6379" />
    </envs>
    <module name="prs-knowledge-agent" />
    <option name="SPRING_BOOT_MAIN_CLASS" value="org.musser.prsknowledgeagent.PRSKnowledgeAgentApplication" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
```

### 4. VS Code Setup

#### Option A: launch.json

Create or edit `.vscode/launch.json`:

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug PRS Knowledge Agent",
            "request": "launch",
            "mainClass": "org.musser.prsknowledgeagent.PRSKnowledgeAgentApplication",
            "projectName": "prs-knowledge-agent",
            "env": {
                "REDIS_HOST": "localhost",
                "REDIS_PORT": "6379",
                "REDIS_URL": "redis://localhost:6379",
                "SPRING_PROFILES_ACTIVE": "dev"
            },
            "vmArgs": ["-Xms1g", "-Xmx4g"]
        },
        {
            "type": "java",
            "name": "Attach to Remote JVM",
            "request": "attach",
            "hostName": "localhost",
            "port": 5005
        }
    ]
}
```

#### Option B: settings.json

Edit `.vscode/settings.json`:

```json
{
    "java.debug.settings.vmArgs": "-Xms1g -Xmx4g",
    "terminal.integrated.env.osx": {
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379"
    },
    "terminal.integrated.env.linux": {
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379"
    },
    "terminal.integrated.env.windows": {
        "REDIS_HOST": "localhost",
        "REDIS_PORT": "6379"
    }
}
```

### 5. Environment File (.env)

Create a `.env` file in the project root:

```bash
# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_URL=redis://localhost:6379

# JVM Options
JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC

# Spring Profile
SPRING_PROFILES_ACTIVE=dev
```

Load the file before running:

#### Using source (bash/zsh):

```bash
export $(cat .env | xargs)
mvn spring-boot:run
```

#### Using direnv (macOS/Linux):

```bash
# Install direnv
brew install direnv  # macOS
# or
apt-get install direnv  # Linux

# Allow direnv for this directory
direnv allow .

# Now env vars are loaded automatically
mvn spring-boot:run
```

### 6. System-Wide Environment Variables

#### macOS/Linux:

Add to `~/.bashrc`, `~/.zshrc`, or `~/.profile`:

```bash
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_URL=redis://localhost:6379
```

Then reload:

```bash
source ~/.bashrc  # or ~/.zshrc
```

#### Windows:

1. Open System Properties → Advanced → Environment Variables
2. Add under "User variables" or "System variables":
    - Variable: `REDIS_HOST`
    - Value: `localhost`
3. Restart your terminal/IDE

### 7. Docker Compose with Debug

Edit `docker-compose.yml` to add debug port:

```yaml
services:
    prs-agent:
        build: .
        container_name: prs-agent
        ports:
            - "80:8080"
            - "8080:8080"
            - "5005:5005" # Debug port
        environment:
            - REDIS_URL=redis://host.docker.internal:6379
            - REDIS_HOST=host.docker.internal
            - REDIS_PORT=6379
            - JAVA_OPTS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -Xms1g -Xmx4g
```

Then attach your debugger to `localhost:5005`.

## 🐛 Debugging Scenarios

### Scenario 1: Debug Locally with Maven

```bash
# Terminal 1: Start Redis
redis-stack-server --daemonize yes

# Terminal 2: Set env vars and run with debug
export REDIS_HOST=localhost
export REDIS_PORT=6379
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

Connect your IDE debugger to `localhost:5005`.

### Scenario 2: Debug in IntelliJ IDEA

1. Set breakpoints in your code
2. Right-click on `PRSKnowledgeAgentApplication.java`
3. Select "Debug 'PRSKnowledgeAgentApplication'"
4. Environment variables from Run Configuration are automatically applied

### Scenario 3: Debug Docker Container

```bash
# Start with debug port exposed
docker-compose up -d

# View logs
docker logs prs-agent -f

# Attach debugger to localhost:5005
```

In your IDE, create a "Remote JVM Debug" configuration:

-   Host: `localhost`
-   Port: `5005`

### Scenario 4: Debug with Different Redis Instance

```bash
# Use external Redis
export REDIS_HOST=my-redis-server.com
export REDIS_PORT=6380
export REDIS_URL=redis://my-redis-server.com:6380

mvn spring-boot:run
```

## 📝 Verify Environment Variables

### Check Current Environment Variables

#### In Application (Java):

```java
System.out.println("REDIS_HOST: " + System.getenv("REDIS_HOST"));
```

#### From Command Line:

```bash
# macOS/Linux
echo $REDIS_HOST
env | grep REDIS

# Windows PowerShell
echo $env:REDIS_HOST
Get-ChildItem Env: | Where-Object {$_.Name -like "*REDIS*"}

# Windows CMD
echo %REDIS_HOST%
set | findstr REDIS
```

## 🔍 Troubleshooting

### Environment Variables Not Loading

1. **Restart your IDE** after setting system environment variables
2. **Check spelling** - variable names are case-sensitive on Unix systems
3. **Use quotes** for values with spaces: `export JAVA_OPTS="-Xms2g -Xmx4g"`
4. **Verify the file** - Make sure `.env` file is in the correct directory

### Debug Port Already in Use

```bash
# Find process using port 5005
lsof -i :5005  # macOS/Linux
netstat -ano | findstr :5005  # Windows

# Kill the process
kill -9 <PID>  # macOS/Linux
taskkill /PID <PID> /F  # Windows
```

### Can't Connect to Redis

```bash
# Test Redis connection
redis-cli -h $REDIS_HOST -p $REDIS_PORT ping

# Check if Redis is running
ps aux | grep redis  # macOS/Linux
Get-Process redis*  # Windows PowerShell
```

## 🎓 Best Practices

1. **Never commit** `.env` files with secrets to version control
2. **Use different env files** for different environments: `.env.dev`, `.env.prod`
3. **Document required variables** in README or `.env.example`
4. **Use IDE features** instead of system-wide variables when possible
5. **Test with fresh environment** to ensure all variables are documented

## 📚 Additional Resources

-   [Spring Boot Environment Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
-   [IntelliJ IDEA Run Configurations](https://www.jetbrains.com/help/idea/run-debug-configuration.html)
-   [VS Code Java Debugging](https://code.visualstudio.com/docs/java/java-debugging)
-   [Docker Environment Variables](https://docs.docker.com/compose/environment-variables/)

---

**Need Help?** Check the main [README.md](README.md) for more information about the project.
