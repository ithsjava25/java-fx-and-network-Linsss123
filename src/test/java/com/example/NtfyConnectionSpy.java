package com.example;

import java.util.function.Consumer;

public class NtfyConnectionSpy implements NtfyConnection {

    String message;
    boolean receiveMessageCalled = false;
    Consumer<NtfyMessageDto> messageHandler;

    @Override
    public boolean sendMessage(String message) {
        this.message = message;
        return true;
    }

    @Override
    public void receiveMessage(Consumer<NtfyMessageDto> messageHandler) {
        this.receiveMessageCalled = true;
        this.messageHandler = messageHandler;
    }
}
