package com.example;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class HelloModelTest {

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    @Test
    @DisplayName("When calling sendMessage it should call connection send")
    void sendMessageCallsConnectionWithMessageToSend() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("Hello World");
        
        model.sendMessage();
        
        assertThat(spy.message).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should not send message when messageToSend is null")
    void shouldNotSendMessageWhenNull() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend(null);
        
        model.sendMessage();
        
        assertThat(spy.message).isNull();
    }

    @Test
    @DisplayName("Should not send message when messageToSend is empty")
    void shouldNotSendMessageWhenEmpty() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("");
        
        model.sendMessage();
        
        assertThat(spy.message).isNull();
    }

    @Test
    @DisplayName("Should not send message when messageToSend is blank")
    void shouldNotSendMessageWhenBlank() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("   ");
        
        model.sendMessage();
        
        assertThat(spy.message).isNull();
    }

    @Test
    @DisplayName("Should handle message with whitespace")
    void shouldHandleMessageWithWhitespace() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("  Hello World  ");
        
        model.sendMessage();
        
        assertThat(spy.message).isEqualTo("  Hello World  ");
    }

    @Test
    @DisplayName("messageToSendProperty should be bidirectional")
    void messageToSendPropertyShouldBeBidirectional() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        model.messageToSendProperty().set("Test Message");
        
        assertThat(model.getMessageToSend()).isEqualTo("Test Message");
    }

    @Test
    @DisplayName("getMessageToSend should return current value")
    void getMessageToSendShouldReturnCurrentValue() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        model.setMessageToSend("Current Message");
        
        String result = model.getMessageToSend();
        
        assertThat(result).isEqualTo("Current Message");
    }

    @Test
    @DisplayName("getGreeting should return valid greeting with Java and JavaFX versions")
    void getGreetingShouldReturnValidGreeting() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        String greeting = model.getGreeting();
        
        assertThat(greeting).isNotNull();
        assertThat(greeting).contains("Hello, JavaFX");
        assertThat(greeting).contains("running on Java");
    }

    @Test
    @DisplayName("getGreeting should contain actual version numbers")
    void getGreetingShouldContainVersionNumbers() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        String greeting = model.getGreeting();
        String javaVersion = System.getProperty("java.version");
        
        assertThat(greeting).contains(javaVersion);
    }

    @Test
    @DisplayName("messages list should be initially empty")
    void messagesListShouldBeInitiallyEmpty() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        assertThat(model.getMessages()).isEmpty();
    }

    @Test
    @DisplayName("messages should be observable list")
    void messagesShouldBeObservableList() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        assertThat(model.getMessages()).isNotNull();
        assertThat(model.getMessages().getClass().getName()).contains("ObservableList");
    }

    @Test
    @DisplayName("receiveMessage should call connection receiveMessage on initialization")
    void receiveMessageShouldBeCalledOnInitialization() {
        var spy = new NtfyConnectionSpy();
        
        new HelloModel(spy);
        
        assertThat(spy.receiveMessageCalled).isTrue();
    }

    @Test
    @DisplayName("receiveMessage handler should add messages to observable list")
    void receiveMessageHandlerShouldAddMessages() throws InterruptedException {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            if (spy.messageHandler != null) {
                var msg = new NtfyMessageDto("id1", 1700000000L, "message", "mytopic", "Test Message");
                spy.messageHandler.accept(msg);
                latch.countDown();
            }
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(model.getMessages()).hasSize(1);
        assertThat(model.getMessages().get(0).message()).isEqualTo("Test Message");
    }

    @Test
    @DisplayName("should handle multiple messages in sequence")
    void shouldHandleMultipleMessages() throws InterruptedException {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        CountDownLatch latch = new CountDownLatch(3);
        
        Platform.runLater(() -> {
            if (spy.messageHandler != null) {
                spy.messageHandler.accept(new NtfyMessageDto("id1", 1700000000L, "message", "mytopic", "Message 1"));
                latch.countDown();
                spy.messageHandler.accept(new NtfyMessageDto("id2", 1700000001L, "message", "mytopic", "Message 2"));
                latch.countDown();
                spy.messageHandler.accept(new NtfyMessageDto("id3", 1700000002L, "message", "mytopic", "Message 3"));
                latch.countDown();
            }
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertThat(model.getMessages()).hasSize(3);
        assertThat(model.getMessages().get(0).message()).isEqualTo("Message 1");
        assertThat(model.getMessages().get(1).message()).isEqualTo("Message 2");
        assertThat(model.getMessages().get(2).message()).isEqualTo("Message 3");
    }

    @Test
    void sendMessageToFakeServer(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok()));
        stubFor(post("/mytopic").willReturn(ok()));

        var con = new NtfyConnectionImpl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        model.setMessageToSend("Hello World");

        model.sendMessage();

        verify(postRequestedFor(urlEqualTo("/mytopic"))
                .withRequestBody(containing("Hello World")));
    }

    @Test
    @DisplayName("should handle message sending with special characters")
    void shouldHandleSpecialCharacters(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok()));
        stubFor(post("/mytopic").willReturn(ok()));

        var con = new NtfyConnectionImpl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        model.setMessageToSend("Hello @#$%^&*() Test!");

        model.sendMessage();

        verify(postRequestedFor(urlEqualTo("/mytopic")));
    }

    @Test
    @DisplayName("should handle very long messages")
    void shouldHandleLongMessages(WireMockRuntimeInfo wireMockRuntimeInfo) {
        stubFor(get(urlPathMatching("/mytopic/json.*")).willReturn(ok()));
        stubFor(post("/mytopic").willReturn(ok()));

        var con = new NtfyConnectionImpl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
        var model = new HelloModel(con);
        String longMessage = "A".repeat(1000);
        model.setMessageToSend(longMessage);

        model.sendMessage();

        verify(postRequestedFor(urlEqualTo("/mytopic"))
                .withRequestBody(containing(longMessage)));
    }

    @Test
    @DisplayName("setMessageToSend should update property")
    void setMessageToSendShouldUpdateProperty() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        model.setMessageToSend("New Message");
        
        assertThat(model.messageToSendProperty().get()).isEqualTo("New Message");
    }

    @Test
    @DisplayName("should handle rapid message updates")
    void shouldHandleRapidMessageUpdates() {
        var spy = new NtfyConnectionSpy();
        var model = new HelloModel(spy);
        
        model.setMessageToSend("Message 1");
        model.setMessageToSend("Message 2");
        model.setMessageToSend("Message 3");
        
        assertThat(model.getMessageToSend()).isEqualTo("Message 3");
    }
}