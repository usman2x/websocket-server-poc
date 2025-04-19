package com.example.websocket.server.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static com.example.websocket.server.service.WebSocketUtils.isSilent;

@Service
public class AsrWebSocketService {

  private final AsrWebSocketClient asrWebSocketClient;

  public AsrWebSocketService(AsrWebSocketClient asrWebSocketClient) {
    this.asrWebSocketClient = asrWebSocketClient;
  }

  /**
   * Handles incoming binary messages from the WebSocket client.
   *
   * @param session The WebSocket session.
   * @param message The binary message received from the client.
   */
  public void handleBinaryMessage(WebSocketSession session, org.springframework.web.socket.BinaryMessage message) {
    try {
      // Extract raw byte data from the incoming message
      ByteBuffer payload = message.getPayload();
      byte[] audioChunk = new byte[payload.remaining()];
      payload.get(audioChunk);

      // Validate that the binary data is compatible with int16 (2 bytes per sample)
      if (audioChunk.length % 2 != 0) {
        throw new IllegalArgumentException("Invalid binary data: Buffer size must be a multiple of 2 (int16). OR ");
      }

      // Process the binary chunk asynchronously
      processBinaryChunk(session, audioChunk)
          .exceptionally(ex -> {
            System.err.println("Error processing request: " + ex.getMessage());
            try {
              session.sendMessage(new TextMessage("ERROR: " + ex.getMessage()));
            } catch (Exception e) {
              System.err.println("Failed to send error message to client: " + e.getMessage());
            }
            return null;
          });

    } catch (Exception e) {
      System.err.println("Error processing binary message 1: " + e.getMessage());
    }
  }

  /**
   * Processes a binary chunk asynchronously.
   *
   * @param session    The WebSocket session.
   * @param audioChunk The binary audio chunk.
   * @return A CompletableFuture representing the asynchronous operation.
   */
  public CompletableFuture<Void> processBinaryChunk(WebSocketSession session, byte[] audioChunk) {
    return CompletableFuture.runAsync(() -> {
      try {
        System.out.println("Forwarding binary chunk to external server...");
        CompletableFuture<String> futureResponse = asrWebSocketClient.sendAndReceive(audioChunk);

        // Wait for the response asynchronously
        String transcription = futureResponse.join();
        System.out.println("Received transcription from external server: " + transcription);

        // Handle empty responses gracefully
        if (transcription == null || transcription.isEmpty()) {
          System.err.println("Skipping empty transcription.");
          return;
        }

        // Send the transcription back to the client
        if (isSessionOpen(session)) {
          try {
            System.out.println("Sending transcription to client: " + transcription);
            if(!isSilent(audioChunk, 200)){
              session.sendMessage(new TextMessage("{\"transcription\": \"" + transcription + "\"}"));
            } else {
              System.out.println("Silent chunk detected, skipping...");
            }
          } catch (Exception e) {
            System.err.println("Failed to send message to client: " + e.getMessage());
          }
        } else {
          System.err.println("Cannot send message: WebSocket session is closed");
        }
      } catch (Exception e) {
        // Send an error response to the client
        if (isSessionOpen(session)) {
          try {
            session.sendMessage(new TextMessage("{\"transcription\": \"Error: " + e.getMessage() + "\"}"));
          } catch (Exception ex) {
            System.err.println("Failed to send error message to client: " + ex.getMessage());
          }
        }
      }
    });
  }

  private boolean isSessionOpen(WebSocketSession session) {
    boolean isOpen = session != null && session.isOpen();
    if (!isOpen) {
      System.err.println("WebSocket session is closed. Cannot send message.");
    }
    return isOpen;
  }
}