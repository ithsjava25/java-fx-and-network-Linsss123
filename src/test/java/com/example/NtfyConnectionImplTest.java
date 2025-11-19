package com.example;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class NtfyConnectionImplTest {

    @Test
    @DisplayName("sendMessage should return true on successful HTTP 200 response")
    void sendMessageShouldReturnTrueOnSuccess(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test message");
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic"))
                .withRequestBody(equalTo("Test message")));
    }

    @Test
    @DisplayName("sendMessage should return false on non-200 HTTP response")
    void sendMessageShouldReturnFalseOnError(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(serverError()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test message");
        
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should return false on 404 Not Found")
    void sendMessageShouldReturnFalseOn404(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(notFound()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test message");
        
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should return false on 401 Unauthorized")
    void sendMessageShouldReturnFalseOn401(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(unauthorized()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test message");
        
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should return false on 403 Forbidden")
    void sendMessageShouldReturnFalseOn403(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(forbidden()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test message");
        
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should handle empty message")
    void sendMessageShouldHandleEmptyMessage(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("");
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic"))
                .withRequestBody(equalTo("")));
    }

    @Test
    @DisplayName("sendMessage should handle messages with special characters")
    void sendMessageShouldHandleSpecialCharacters(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        String specialMessage = "Hello @#$%^&*() <>/\\|";
        boolean result = connection.sendMessage(specialMessage);
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic")));
    }

    @Test
    @DisplayName("sendMessage should handle Unicode characters")
    void sendMessageShouldHandleUnicode(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        String unicodeMessage = "Hello 世界 🌍 مرحبا Привет";
        boolean result = connection.sendMessage(unicodeMessage);
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic")));
    }

    @Test
    @DisplayName("sendMessage should handle very long messages")
    void sendMessageShouldHandleLongMessages(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        String longMessage = "A".repeat(10000);
        boolean result = connection.sendMessage(longMessage);
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic")));
    }

    @Test
    @DisplayName("sendMessage should post to correct endpoint")
    void sendMessageShouldPostToCorrectEndpoint(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        connection.sendMessage("Test");
        
        verify(postRequestedFor(urlEqualTo("/mytopic")));
        verify(0, postRequestedFor(urlPathMatching("/other.*")));
    }

    @Test
    @DisplayName("sendMessage should return false on connection timeout")
    void sendMessageShouldHandleTimeout(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok().withFixedDelay(30000)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        boolean result = connection.sendMessage("Test");
        
        // This might timeout and return false
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should return false when server is unreachable")
    void sendMessageShouldHandleUnreachableServer() {
        var connection = new NtfyConnectionImpl("http://localhost:9999");
        boolean result = connection.sendMessage("Test");
        
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("sendMessage should handle newlines in message")
    void sendMessageShouldHandleNewlines(WireMockRuntimeInfo wmInfo) {
        stubFor(post("/mytopic").willReturn(ok()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        String multilineMessage = "Line 1\nLine 2\nLine 3";
        boolean result = connection.sendMessage(multilineMessage);
        
        assertThat(result).isTrue();
        verify(postRequestedFor(urlEqualTo("/mytopic")));
    }

    @Test
    @DisplayName("receiveMessage should connect to JSON stream endpoint")
    void receiveMessageShouldConnectToJsonEndpoint(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"test123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Hello\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        
        connection.receiveMessage(msg -> latch.countDown());
        
        latch.await(2, TimeUnit.SECONDS);
        verify(getRequestedFor(urlMatching("/mytopic/json.*")));
    }

    @Test
    @DisplayName("receiveMessage should parse valid JSON messages")
    void receiveMessageShouldParseJsonMessages(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"msg123\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test Message\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0).id()).isEqualTo("msg123");
        assertThat(receivedMessages.get(0).time()).isEqualTo(1700000000L);
        assertThat(receivedMessages.get(0).event()).isEqualTo("message");
        assertThat(receivedMessages.get(0).topic()).isEqualTo("mytopic");
        assertThat(receivedMessages.get(0).message()).isEqualTo("Test Message");
    }

    @Test
    @DisplayName("receiveMessage should filter out non-message events")
    void receiveMessageShouldFilterNonMessageEvents(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"keepalive1\",\"time\":1700000000,\"event\":\"keepalive\",\"topic\":\"mytopic\",\"message\":\"\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger messageCount = new AtomicInteger(0);
        
        connection.receiveMessage(msg -> messageCount.incrementAndGet());
        
        // Wait a bit to ensure no message is processed
        Thread.sleep(500);
        
        assertThat(messageCount.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("receiveMessage should handle multiple messages in stream")
    void receiveMessageShouldHandleMultipleMessages(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"msg1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"First\"}\n" +
                             "{\"id\":\"msg2\",\"time\":1700000001,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Second\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(2);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(2);
        assertThat(receivedMessages.get(0).message()).isEqualTo("First");
        assertThat(receivedMessages.get(1).message()).isEqualTo("Second");
    }

    @Test
    @DisplayName("receiveMessage should ignore invalid JSON lines")
    void receiveMessageShouldIgnoreInvalidJson(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "invalid json\n" +
                             "{\"id\":\"msg1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Valid\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0).message()).isEqualTo("Valid");
    }

    @Test
    @DisplayName("receiveMessage should handle messages with missing optional fields")
    void receiveMessageShouldHandleMissingFields(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"msg1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Test\",\"extra\":\"ignored\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0).message()).isEqualTo("Test");
    }

    @Test
    @DisplayName("receiveMessage should handle non-200 status codes")
    void receiveMessageShouldHandleErrorStatus(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(serverError()));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        
        connection.receiveMessage(msg -> handlerCalled.set(true));
        
        Thread.sleep(500);
        
        assertThat(handlerCalled.get()).isFalse();
        verify(getRequestedFor(urlMatching("/mytopic/json.*")));
    }

    @Test
    @DisplayName("receiveMessage should handle messages with Unicode")
    void receiveMessageShouldHandleUnicodeInMessages(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = "{\"id\":\"msg1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Hello 世界 🌍\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(1);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(1);
        assertThat(receivedMessages.get(0).message()).isEqualTo("Hello 世界 🌍");
    }

    @Test
    @DisplayName("constructor should accept custom hostname")
    void constructorShouldAcceptCustomHostname() {
        var connection = new NtfyConnectionImpl("https://custom.example.com");
        assertThat(connection).isNotNull();
    }

    @Test
    @DisplayName("receiveMessage should handle empty response")
    void receiveMessageShouldHandleEmptyResponse(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody("")));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        
        connection.receiveMessage(msg -> handlerCalled.set(true));
        
        Thread.sleep(500);
        
        assertThat(handlerCalled.get()).isFalse();
    }

    @Test
    @DisplayName("receiveMessage should handle mixed valid and invalid messages")
    void receiveMessageShouldHandleMixedMessages(WireMockRuntimeInfo wmInfo) throws InterruptedException {
        String jsonResponse = 
            "{\"id\":\"msg1\",\"time\":1700000000,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"First\"}\n" +
            "invalid json line\n" +
            "{\"id\":\"ka1\",\"time\":1700000001,\"event\":\"keepalive\",\"topic\":\"mytopic\",\"message\":\"\"}\n" +
            "{\"id\":\"msg2\",\"time\":1700000002,\"event\":\"message\",\"topic\":\"mytopic\",\"message\":\"Second\"}";
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok().withBody(jsonResponse)));
        
        var connection = new NtfyConnectionImpl("http://localhost:" + wmInfo.getHttpPort());
        CountDownLatch latch = new CountDownLatch(2);
        List<NtfyMessageDto> receivedMessages = new ArrayList<>();
        
        connection.receiveMessage(msg -> {
            receivedMessages.add(msg);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(receivedMessages).hasSize(2);
        assertThat(receivedMessages.get(0).message()).isEqualTo("First");
        assertThat(receivedMessages.get(1).message()).isEqualTo("Second");
    }

    @Test
    @DisplayName("receiveMessage should handle connection errors gracefully")
    void receiveMessageShouldHandleConnectionErrors() throws InterruptedException {
        var connection = new NtfyConnectionImpl("http://localhost:9999");
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        
        connection.receiveMessage(msg -> handlerCalled.set(true));
        
        Thread.sleep(500);
        
        assertThat(handlerCalled.get()).isFalse();
    }
}