package com.example.websocket.server.config;

import com.example.websocket.server.handler.ClientWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ClientWebSocketHandler clientWebSocketHandler;

  public WebSocketConfig(ClientWebSocketHandler clientWebSocketHandler) {
    this.clientWebSocketHandler = clientWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(clientWebSocketHandler, "/audio-stream").setAllowedOrigins("*");
  }
}