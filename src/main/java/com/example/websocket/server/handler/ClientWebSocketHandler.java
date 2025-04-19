package com.example.websocket.server.handler;

import com.example.websocket.server.service.AsrWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
public class ClientWebSocketHandler extends AbstractWebSocketHandler {

  private static final Logger logger = LoggerFactory.getLogger(ClientWebSocketHandler.class);

  private final AsrWebSocketService asrWebSocketService;

  public ClientWebSocketHandler(AsrWebSocketService asrWebSocketService) {
    this.asrWebSocketService = asrWebSocketService;
  }

  @Override
  protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
    System.out.println("Raw binary message received: " + message.getPayload().remaining() + " bytes");
    session.setBinaryMessageSizeLimit(2048*2024);
    session.setTextMessageSizeLimit(2048*2024);
    asrWebSocketService.handleBinaryMessage(session, message);
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    logger.info("WebSocket connection established: {}", session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    logger.info("WebSocket connection closed: {}", session.getId());
  }
}