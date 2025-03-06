# simple-chat-ui

A very simple chat ui that can be used to test Spring-AI/Tanzu AI Server (or any OpenAI compatible endpoint)

## Run against OpenAI

```shell
export SPRING_AI_OPENAI_API_KEY=xxx
./mvnw spring-boot:run
```

Then access the UI in the browser at http://localhost:8080

## Run against Tanzu AI Server

```shell
export SPRING_AI_OPENAI_API_KEY=xxx
export SPRING_AI_OPENAI_CHAT_BASE_URL=yyy
./mvnw spring-boot:run
```

Then access the UI in the browser at http://localhost:8080

## Run with a local MCP server

```shell
export SPRING_AI_OPENAI_API_KEY=xxx
export SPRING_AI_OPENAI_CHAT_BASE_URL=yyy
export SPRING_AI_MCP_CLIENT_STDIO_SERVERS_CONFIGURATION=file://$PWD/../servers.json 
./mvnw clean spring-boot:run
```

## Run on CF

```shell
export VCAP_SERVICES=...
./mvnw clean spring-boot:run
```

NOTE: when `cf push`'ing the application, VCAP_SERVICES will be created for you.
