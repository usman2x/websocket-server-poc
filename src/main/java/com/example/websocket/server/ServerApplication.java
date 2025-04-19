package com.example.websocket.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(ServerApplication.class);
		ConfigurableApplicationContext context = app.run(args);

		// Log the WebSocket server port
		String port = context.getEnvironment().getProperty("server.port");
		System.out.println("WebSocket server is running on ws://localhost:" + port + "/audio-stream");
	}
}
