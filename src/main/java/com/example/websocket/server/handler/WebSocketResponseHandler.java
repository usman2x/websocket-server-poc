package com.example.websocket.server.handler;

import com.example.websocket.server.service.ResponseUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketResponseHandler extends WebSocketClient {

  private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests;
  private static final ObjectMapper objectMapper = new ObjectMapper();


  public WebSocketResponseHandler(URI uri, ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests) {
    super(uri);
    this.pendingRequests = pendingRequests;
  }

  @Override
  public void onOpen(ServerHandshake handshake) {
    System.out.println("Connected to external WebSocket server");
  }

  //TODO Problematic
  @Override
  public void onMessage(String message) {
    try {
      // Parse the JSON response
      JsonNode jsonNode = objectMapper.readTree(message);

      if (jsonNode.has("transcription")) {
        String correlationId = jsonNode.get("correlationId").asText();
        String transcription = jsonNode.get("transcription").asText();

        System.out.println("Received response from external server: " + transcription);

        // Complete the corresponding future
        CompletableFuture<String> future = pendingRequests.remove(correlationId);
        if (future != null && !future.isDone()) {
          future.complete(transcription);
        } else {
          System.err.println("No pending request found for correlation ID: " + correlationId);
        }
      }

    } catch (Exception e) {
      System.err.println("Failed to parse text message: " + e.getMessage());
      failAllPendingRequests(e);
    }
  }

  @Override
  public void onMessage(ByteBuffer bytes) {
    System.out.println("Received bytes response from external server: " + bytes.toString());
    try {
      byte[] response = new byte[bytes.remaining()];
      bytes.get(response);

      // Extract correlation ID and transcription
      String correlationId = new String(response, 0, 16, StandardCharsets.UTF_8);
      String transcription = new String(response, 16, response.length - 16, StandardCharsets.UTF_8);

      // Complete the corresponding future
      CompletableFuture<String> future = pendingRequests.remove(correlationId);
      if (future != null && !future.isDone()) {
        future.complete(transcription);
      }else {
        System.err.println("No pending request found for correlation ID: " + correlationId);
      }
    } catch (Exception e) {
      System.err.println("Failed to parse binary response: " + e.getMessage());
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
    failAllPendingRequests(new RuntimeException("WebSocket connection closed: " + ex.getMessage()));
  }
  private void completePendingRequest(String transcription) {
    for (CompletableFuture<String> future : pendingRequests.values()) {
      if (!future.isDone()) {
        future.complete(transcription);
        break; // Assuming only one active request at a time
      }
    }
  }

  private void failAllPendingRequests(Exception ex) {
    for (String correlationId : pendingRequests.keySet()) {
      CompletableFuture<String> future = pendingRequests.remove(correlationId);
      if (future != null && !future.isDone()) {
        future.completeExceptionally(ex);
      }
    }
  }

}