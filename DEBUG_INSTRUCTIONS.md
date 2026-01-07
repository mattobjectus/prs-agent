# How to Run in Debug Mode

## Step 1: Start the Application in Debug Mode

Run this command in your terminal:

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

You should see this output indicating debug mode is active:

```
Listening for transport dt_socket at address: 5005
```

**Keep this terminal running!** The application will be listening for debugger connections on port 5005.

## Step 2: Attach VS Code Debugger

1. **Open the Run and Debug panel** in VS Code:

    - Press `Cmd+Shift+D` (Mac) or `Ctrl+Shift+D` (Windows/Linux)
    - Or click the "Run and Debug" icon in the left sidebar (looks like a play button with a bug)

2. **Select the debug configuration**:

    - At the top of the Run and Debug panel, you'll see a dropdown
    - Select **"Debug (Attach) - Port 5005"**

3. **Start debugging**:

    - Click the green **▶ Play button** next to the dropdown
    - Or press `F5`

4. **You should see**:
    - A debug toolbar appear at the top of VS Code
    - The status bar at the bottom turn orange
    - "Debugger attached" message in the Debug Console

## Step 3: Set Breakpoints and Debug

1. **Set breakpoints**:

    - Open any Java file (e.g., `PRSKnowledgeAgentController.java`)
    - Click to the left of the line number where you want to pause execution
    - A red dot will appear

2. **Trigger the breakpoint**:

    - Access your application at http://localhost:8080/chat.html
    - Interact with the UI to trigger the code path with your breakpoint
    - When the breakpoint is hit, VS Code will pause execution

3. **Debug controls**:

    - **Continue** (F5): Resume execution
    - **Step Over** (F10): Execute current line and move to next
    - **Step Into** (F11): Enter into method calls
    - **Step Out** (Shift+F11): Exit current method
    - **Restart** (Cmd+Shift+F5): Restart debugging session
    - **Stop** (Shift+F5): Stop debugging

4. **Inspect variables**:
    - View variables in the "Variables" panel on the left
    - Hover over variables in the code to see their values
    - Use the "Watch" panel to monitor specific expressions
    - Use the "Debug Console" to evaluate expressions

## Troubleshooting

### Can't attach debugger?

-   Make sure the application is running and shows "Listening for transport dt_socket at address: 5005"
-   Check that port 5005 isn't blocked by a firewall
-   Try restarting the application

### Breakpoints not working?

-   Make sure you're attaching the debugger AFTER the application starts
-   Verify the source code matches the compiled code
-   Try rebuilding with `mvn clean compile`

### Application won't start?

-   Check that port 8080 is not already in use
-   Ensure Redis is running: `docker ps | grep redis`
-   Check for errors in the terminal output

## Quick Start Commands

```bash
# Start Redis (if not already running)
docker run -d --name redis -p 6379:6379 redis:latest

# Start application in debug mode
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# In VS Code: Press Cmd+Shift+D, select "Debug (Attach) - Port 5005", press F5
```

## Useful Breakpoint Locations

-   **PRSKnowledgeAgentController.java** - Line where `PRSKnowledgeAgent.answer()` is called
-   **RedisChatMemoryStore.java** - `getMessages()` and `updateMessages()` methods
-   **PRSKnowledgeAgentConfiguration.java** - Bean creation methods
-   **PRSKnowledgeAgent.java** - The `answer()` method interface

## Additional Tips

-   Use **conditional breakpoints** by right-clicking a breakpoint and adding a condition
-   Use **logpoints** to log messages without stopping execution (right-click → Add Logpoint)
-   The **Call Stack** panel shows the sequence of method calls that led to current execution point
-   Press `Cmd+K Cmd+I` to see inline variable values while debugging
