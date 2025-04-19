package com.example.websocket.server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WebSocketUtils {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Parses the JSON response from the external server and extracts the transcription.
   *
   * @param payload The JSON string received from the external server.
   * @return The transcription text.
   * @throws Exception If the payload is invalid or cannot be parsed.
   */
  public static String parseTranscription(String payload) throws Exception {
    try {
      // Parse the JSON payload
      JsonNode jsonNode = objectMapper.readTree(payload);

      // Extract the "transcription" field
      if (jsonNode.has("transcription")) {
        return jsonNode.get("transcription").asText();
      } else {
        throw new IllegalArgumentException("Missing 'transcription' field in server response.");
      }
    } catch (Exception e) {
      // Log the error and rethrow
      System.err.println("Failed to parse server response: " + e.getMessage());
      throw e;
    }
  }

  public static boolean isSilent(byte[] audioData, int threshold) {
    // Convert byte array to short array (16-bit PCM)
    short[] samples = new short[audioData.length / 2];
    for (int i = 0; i < samples.length; i++) {
      samples[i] = (short) ((audioData[i * 2] & 0xFF) | (audioData[i * 2 + 1] << 8));
    }

    // Calculate the root mean square (RMS) of the samples
    double sumOfSquares = 0.0;
    for (short sample : samples) {
      sumOfSquares += sample * sample;
    }
    double rms = Math.sqrt(sumOfSquares / samples.length);

    // Return true if the RMS is below the threshold (silent)
    return rms < threshold;
  }
}