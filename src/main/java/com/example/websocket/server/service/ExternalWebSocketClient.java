package com.example.websocket.server.service;


import com.example.websocket.server.handler.WebSocketResponseHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExternalWebSocketClient {

  private final URI uri;
  private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
  private WebSocketResponseHandler webSocketResponseHandler;

  public ExternalWebSocketClient(@Value("${external.websocket.url}") String wsUrl) throws Exception {
    this.uri = new URI(wsUrl);
    connect();
  }

  /**
   * Establishes a connection to the external WebSocket server.
   */
  private void connect() throws Exception {
    webSocketResponseHandler = new WebSocketResponseHandler(uri, pendingRequests);
    webSocketResponseHandler.connectBlocking(); // Block until the connection is established
  }

  /**
   * Sends binary data to the external WebSocket server and waits for a response.
   *
   * @param audioChunk The binary audio chunk to send.
   * @return A CompletableFuture representing the asynchronous operation.
   */
  public CompletableFuture<String> sendAndReceive(byte[] audioChunk) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Ensure the connection is open
        if (!webSocketResponseHandler.isOpen()) {
          System.out.println("Reconnecting to external WebSocket server...");
          connect(); // Attempt to reconnect
        }

        // Generate a correlation ID
        String correlationId = java.util.UUID.randomUUID().toString().substring(0, 16);

        // Create the payload with the correlation ID prepended
        ByteBuffer buffer = ByteBuffer.allocate(16 + audioChunk.length);
        buffer.put(correlationId.getBytes(StandardCharsets.UTF_8));
        buffer.put(audioChunk);
        byte[] payload = buffer.array();

        // Send the payload and wait for the response
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        pendingRequests.put(correlationId, futureResponse);
        webSocketResponseHandler.send(payload);

        String transcription = futureResponse.get(); // Wait for the response

        // Handle empty responses
        if (transcription == null || transcription.isEmpty()) {
          System.err.println("Received empty transcription from external server.");
          return ""; // Return an empty string instead of throwing an exception
        }

        return transcription;

      } catch (Exception e) {
        throw new RuntimeException("Error communicating with external WebSocket server", e);
      }
    });
  }
}