# Chatbot AI

A local AI chatbot built with Angular, Spring Boot, and Ollama.

## Architecture

```text
Angular frontend → Spring Boot backend → Ollama → llama3.2
```

The application runs locally. Chat messages are sent to the Spring Boot API,
which forwards them to Ollama and returns the model response.

## Requirements

- Windows 10 or later
- Java JDK 17 or later
- Maven 3.9 or later
- Node.js and npm
- Angular CLI
- Ollama
- The `llama3.2` Ollama model

## Install Ollama

Open PowerShell:

```powershell
irm https://ollama.com/install.ps1 | iex
```

Verify the installation:

```powershell
ollama --version
```

Download the model:

```powershell
ollama pull llama3.2
```

Test Ollama:

```powershell
ollama run llama3.2
```

Type `/bye` to exit the test chat.

Verify the local Ollama server:

```powershell
Invoke-WebRequest http://localhost:11434/ -UseBasicParsing
```

The expected response is:

```text
Ollama is running
```

## Run the backend

Open a terminal in the project directory:

```cmd
cd /d D:\ChatbotAI\backend
mvn spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

The chat endpoint is:

```text
POST http://localhost:8080/api/chat
```

Example request:

```json
{
  "message": "Hello"
}
```

## Run the frontend

Open a second terminal:

```cmd
cd /d D:\ChatbotAI\frontend
npm.cmd install
ng.cmd serve --open
```

Open the chatbot at:

```text
http://localhost:4200
```

Keep Ollama, the backend, and the frontend running in their respective
terminal windows.

## Project structure

```text
ChatbotAI/
├── backend/
│   ├── pom.xml
│   └── src/
│       └── main/java/com/example/chatbot_backend/
│           ├── ChatbotBackendApplication.java
│           └── ChatController.java
├── frontend/
│   ├── package.json
│   └── src/
└── README.md
```

## GitHub upload

Upload the source and configuration files, including:

- `backend/src`
- `backend/pom.xml`
- `frontend/src`
- `frontend/package.json`
- `frontend/package-lock.json`
- `README.md`
- `.gitignore`

Do not upload:

- `backend/target`
- `frontend/node_modules`
- `frontend/dist`
- `.idea`
- compiled `.class` files
- Ollama model files

The model is downloaded locally with:

```powershell
ollama pull llama3.2
```
