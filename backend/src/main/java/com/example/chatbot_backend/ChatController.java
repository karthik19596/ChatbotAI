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
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message must not be empty."));
        }

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "model", MODEL,
                    "prompt", request.message(),
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

    public record ChatRequest(String message) {
    }

    public record ChatResponse(String response) {
    }
}
