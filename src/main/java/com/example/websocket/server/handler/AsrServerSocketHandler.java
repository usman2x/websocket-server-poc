package com.example.websocket.server.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class AsrServerSocketHandler extends WebSocketClient {

  private final Queue<CompletableFuture<String>> pendingRequests;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public AsrServerSocketHandler(URI uri, Queue<CompletableFuture<String>> pendingRequests) {
    super(uri);
    this.pendingRequests = pendingRequests;
  }

  @Override
  public void onOpen(ServerHandshake handshake) {
    System.out.println("Connected to external WebSocket server");
  }

  @Override
  public void onMessage(String message) {
    try {
      // Parse the JSON response
      JsonNode jsonNode = objectMapper.readTree(message);

      // Extract transcription
      String transcription = jsonNode.get("transcription").asText();
      System.out.println("Received transcription from external server: " + transcription);

      // Complete the next pending request
      synchronized (pendingRequests) {
        if (!pendingRequests.isEmpty()) {
          CompletableFuture<String> future = pendingRequests.poll();
          if (future != null && !future.isDone()) {
            future.complete(transcription);
          }
        } else {
          System.err.println("No pending request found for received transcription.");
        }
      }
    } catch (Exception e) {
      // Handle malformed responses or missing fields
      String errorMessage = "Error processing server response: " + e.getMessage();
      System.err.println(errorMessage);

      // Fail all pending requests with a default error message
      failAllPendingRequests(new RuntimeException(errorMessage));
    }
  }

  @Override
  public void onClose(int code, String reason, boolean remote) {
    System.out.println("Connection to external server closed: " + reason);
    failAllPendingRequests(new RuntimeException("WebSocket connection closed: " + reason));
  }

  @Override
  public void onError(Exception ex) {
    System.err.println("Error with external WebSocket server: " + ex.getMessage());
    failAllPendingRequests(ex);
  }

  /**
   * Fails all pending requests with the given exception.
   *
   * @param ex The exception to propagate to all pending requests.
   */
  private void failAllPendingRequests(Exception ex) {
    String errorMessage = "Error: " + ex.getMessage();
    synchronized (pendingRequests) {
      while (!pendingRequests.isEmpty()) {
        CompletableFuture<String> future = pendingRequests.poll();
        if (future != null && !future.isDone()) {
          future.complete(errorMessage); // Send error as transcription
        }
      }
    }
  }
}