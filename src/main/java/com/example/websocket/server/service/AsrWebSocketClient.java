package com.example.websocket.server.service;

import com.example.websocket.server.handler.AsrServerSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

@Component
public class AsrWebSocketClient {

  private final URI uri;
  private final Queue<CompletableFuture<String>> pendingRequests = new ArrayDeque<>();
  private AsrServerSocketHandler webSocketResponseHandler;

  public AsrWebSocketClient(@Value("${external.websocket.url}") String wsUrl) throws Exception {
    this.uri = new URI(wsUrl);
    connect();
  }

  /**
   * Establishes a connection to the external WebSocket server.
   */
  private void connect() throws Exception {
    webSocketResponseHandler = new AsrServerSocketHandler(uri, pendingRequests);
    webSocketResponseHandler.connectBlocking(); // Block until the connection is established
  }

  /**
   * Sends audio data to the external WebSocket server and waits for a response.
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

        // Create a future for the response and add it to the queue
        CompletableFuture<String> futureResponse = new CompletableFuture<>();
        synchronized (pendingRequests) {
          pendingRequests.add(futureResponse);
        }

        // Send the binary payload
        webSocketResponseHandler.send(ByteBuffer.wrap(audioChunk));

        // Wait for the response
        return futureResponse.get();

      } catch (Exception e) {
        throw new RuntimeException("Error communicating with external WebSocket server", e);
      }
    });
  }
}