# simple-chat-ui

A very simple chat ui that can be used to test Spring-AI/Tanzu AI Server (or any OpenAI compatible endpoint)

To run against OpenAI use the following:

```shell
export OPENAI_KEY=xxx
./mvnw spring-boot:run
```

Then access the UI in the browser at http://localhost:8080

To run against Tanzu AI Server:

```shell
export OPENAI_KEY=xxx
export OPENAI_URL=yyy
./mvnw spring-boot:run
```

Then access the UI in the browser at http://localhost:8080

To run with local mcp client:

```shell
SPRING_AI_MCP_CLIENT_STDIO_SERVERS_CONFIGURATION=file://$PWD/../servers.json ./mvnw clean spring-boot:run
```
