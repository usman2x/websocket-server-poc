package com.example.websocket.server.config;

import com.example.websocket.server.handler.AudioWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final AudioWebSocketHandler audioWebSocketHandler;

  public WebSocketConfig(AudioWebSocketHandler audioWebSocketHandler) {
    this.audioWebSocketHandler = audioWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(audioWebSocketHandler, "/audio-stream").setAllowedOrigins("*");
  }
}