package com.example.chatbot_backend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "llama3.2";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Request must not be empty."));
        }

        boolean hasMessage = request.message() != null && !request.message().isBlank();
        boolean hasFile = request.fileContent() != null && !request.fileContent().isBlank();

        if (!hasMessage && !hasFile) {
            return ResponseEntity.badRequest().body(Map.of("error", "Provide a message or a file."));
        }

        String prompt = buildPrompt(request, hasMessage, hasFile);

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", MODEL,
                    "prompt", prompt,
                    "stream", false
            ));

            HttpRequest ollamaRequest = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> ollamaResponse = httpClient.send(
                    ollamaRequest,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (ollamaResponse.statusCode() != 200) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Ollama returned HTTP " + ollamaResponse.statusCode()));
            }

            Map<String, Object> responseBody = objectMapper.readValue(
                    ollamaResponse.body(),
                    new TypeReference<>() {
                    }
            );

            return ResponseEntity.ok(new ChatResponse(
                    String.valueOf(responseBody.getOrDefault("response", "No response received."))
            ));
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Could not communicate with Ollama."));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "The request was interrupted."));
        }
    }

    private String buildPrompt(ChatRequest request, boolean hasMessage, boolean hasFile) {
        StringBuilder prompt = new StringBuilder();

        if (hasFile) {
            String fileName = request.fileName() != null && !request.fileName().isBlank()
                    ? request.fileName()
                    : "uploaded-file";
            prompt.append("The user uploaded a file named \"")
                  .append(fileName)
                  .append("\". Its contents are between the markers below:\n\n")
                  .append("----- BEGIN FILE -----\n")
                  .append(request.fileContent())
                  .append("\n----- END FILE -----\n\n");
        }

        if (hasMessage) {
            prompt.append("User request: ").append(request.message());
        } else {
            prompt.append("User request: Please review the uploaded file content and provide a summary.");
        }

        return prompt.toString();
    }

    public record ChatRequest(String message, String fileName, String fileContent) {
    }

    public record ChatResponse(String response) {
    }
}
