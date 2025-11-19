package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NtfyMessageDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("should create NtfyMessageDto with all fields")
    void shouldCreateWithAllFields() {
        var dto = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        
        assertThat(dto.id()).isEqualTo("id123");
        assertThat(dto.time()).isEqualTo(1700000000L);
        assertThat(dto.event()).isEqualTo("message");
        assertThat(dto.topic()).isEqualTo("mytopic");
        assertThat(dto.message()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("should deserialize from JSON with all fields")
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.id()).isEqualTo("msg123");
        assertThat(dto.time()).isEqualTo(1700000000L);
        assertThat(dto.event()).isEqualTo("message");
        assertThat(dto.topic()).isEqualTo("mytopic");
        assertThat(dto.message()).isEqualTo("Test");
    }

    @Test
    @DisplayName("should ignore unknown JSON fields")
    void shouldIgnoreUnknownFields() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\",\"extra\":\"ignored\",\"unknown\":123}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.id()).isEqualTo("msg123");
        assertThat(dto.message()).isEqualTo("Test");
    }

    @Test
    @DisplayName("should serialize to JSON")
    void shouldSerializeToJson() throws Exception {
        var dto = new NtfyMessageDto("msg123", 1700000000L, "message", "mytopic", "Test");
        
        String json = mapper.writeValueAsString(dto);
        
        assertThat(json).contains("\"id\":\"msg123\"");
        assertThat(json).contains("\"time\":1700000000");
        assertThat(json).contains("\"event\":\"message\"");
        assertThat(json).contains("\"topic\":\"mytopic\"");
        assertThat(json).contains("\"message\":\"Test\"");
    }

    @Test
    @DisplayName("should handle null message field")
    void shouldHandleNullMessage() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"keepalive\",\"topic\":\"mytopic\",\"message\":null}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.message()).isNull();
    }

    @Test
    @DisplayName("should handle empty message field")
    void shouldHandleEmptyMessage() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.message()).isEmpty();
    }

    @Test
    @DisplayName("should handle Unicode in message")
    void shouldHandleUnicodeInMessage() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Hello 世界 🌍\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.message()).isEqualTo("Hello 世界 🌍");
    }

    @Test
    @DisplayName("should handle special characters in message")
    void shouldHandleSpecialCharacters() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\\n\\t\\\"quote\\\"\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.message()).contains("Test");
        assertThat(dto.message()).contains("quote");
    }

    @Test
    @DisplayName("should have JsonIgnoreProperties annotation")
    void shouldHaveJsonIgnorePropertiesAnnotation() {
        JsonIgnoreProperties annotation = NtfyMessageDto.class.getAnnotation(JsonIgnoreProperties.class);
        
        assertThat(annotation).isNotNull();
        assertThat(annotation.ignoreUnknown()).isTrue();
    }

    @Test
    @DisplayName("equals should work correctly for same values")
    void equalsShouldWorkForSameValues() {
        var dto1 = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        var dto2 = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    @DisplayName("equals should return false for different values")
    void equalsShouldReturnFalseForDifferentValues() {
        var dto1 = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        var dto2 = new NtfyMessageDto("id456", 1700000000L, "message", "mytopic", "Hello");
        
        assertThat(dto1).isNotEqualTo(dto2);
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
        var dto = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        
        int hash1 = dto.hashCode();
        int hash2 = dto.hashCode();
        
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
        var dto = new NtfyMessageDto("id123", 1700000000L, "message", "mytopic", "Hello");
        
        String str = dto.toString();
        
        assertThat(str).contains("id123");
        assertThat(str).contains("1700000000");
        assertThat(str).contains("message");
        assertThat(str).contains("mytopic");
        assertThat(str).contains("Hello");
    }

    @Test
    @DisplayName("should handle very long message strings")
    void shouldHandleLongMessages() throws Exception {
        String longMessage = "A".repeat(10000);
        String json = String.format("{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"%s\"}", longMessage);
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.message()).hasSize(10000);
    }

    @Test
    @DisplayName("should handle zero timestamp")
    void shouldHandleZeroTimestamp() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":0,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.time()).isEqualTo(0L);
    }

    @Test
    @DisplayName("should handle negative timestamp")
    void shouldHandleNegativeTimestamp() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":-1,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.time()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("should handle different event types")
    void shouldHandleDifferentEventTypes() throws Exception {
        String[] events = {"message", "keepalive", "open", "poll_request"};
        
        for (String event : events) {
            String json = String.format("{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"%s\",\"topic\":\"mytopic\",\"message\":\"Test\"}", event);
            NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
            assertThat(dto.event()).isEqualTo(event);
        }
    }

    @Test
    @DisplayName("should handle missing optional fields in JSON")
    void shouldHandleMissingOptionalFields() throws Exception {
        String json = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\"}";
        
        NtfyMessageDto dto = mapper.readValue(json, NtfyMessageDto.class);
        
        assertThat(dto.id()).isEqualTo("msg123");
        assertThat(dto.message()).isNull();
    }
}