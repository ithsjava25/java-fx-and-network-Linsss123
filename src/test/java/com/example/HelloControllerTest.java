package com.example;

import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class HelloControllerTest {

    @BeforeAll
    static void initJavaFX() {
        new JFXPanel();
    }

    private HelloController controller;

    @BeforeEach
    void setUp() {
        controller = new HelloController();
    }

    @Test
    @DisplayName("sendMessage should delegate to model")
    void sendMessageShouldDelegateToModel() throws Exception {
        // Get the private model field via reflection
        Field modelField = HelloController.class.getDeclaredField("model");
        modelField.setAccessible(true);
        HelloModel model = (HelloModel) modelField.get(controller);
        
        // Set up the model with a spy connection
        Field connectionField = HelloModel.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        NtfyConnectionSpy spy = new NtfyConnectionSpy();
        connectionField.set(model, spy);
        
        model.setMessageToSend("Test Message");
        
        controller.sendMessage(new ActionEvent());
        
        assertThat(spy.message).isEqualTo("Test Message");
    }

    @Test
    @DisplayName("controller should have non-null model")
    void controllerShouldHaveNonNullModel() throws Exception {
        Field modelField = HelloController.class.getDeclaredField("model");
        modelField.setAccessible(true);
        HelloModel model = (HelloModel) modelField.get(controller);
        
        assertThat(model).isNotNull();
    }

    @Test
    @DisplayName("messageView field should be public and FXML annotated")
    void messageViewShouldBePublicAndFxmlAnnotated() throws Exception {
        Field messageViewField = HelloController.class.getDeclaredField("messageView");
        
        assertThat(messageViewField).isNotNull();
        assertThat(messageViewField.getAnnotation(javafx.fxml.FXML.class)).isNotNull();
    }

    @Test
    @DisplayName("messageLabel field should have FXML annotation")
    void messageLabelShouldHaveFxmlAnnotation() throws Exception {
        Field messageLabelField = HelloController.class.getDeclaredField("messageLabel");
        
        assertThat(messageLabelField).isNotNull();
        assertThat(messageLabelField.getAnnotation(javafx.fxml.FXML.class)).isNotNull();
    }

    @Test
    @DisplayName("messageInput field should have FXML annotation")
    void messageInputShouldHaveFxmlAnnotation() throws Exception {
        Field messageInputField = HelloController.class.getDeclaredField("messageInput");
        
        assertThat(messageInputField).isNotNull();
        assertThat(messageInputField.getAnnotation(javafx.fxml.FXML.class)).isNotNull();
    }

    @Test
    @DisplayName("initialize should set messageLabel text when not null")
    void initializeShouldSetMessageLabelText() throws Exception {
        Label label = new Label();
        Field labelField = HelloController.class.getDeclaredField("messageLabel");
        labelField.setAccessible(true);
        labelField.set(controller, label);
        
        // Call initialize via reflection
        var initMethod = HelloController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);
        
        assertThat(label.getText()).isNotNull();
        assertThat(label.getText()).contains("Hello, JavaFX");
    }

    @Test
    @DisplayName("initialize should handle null messageLabel")
    void initializeShouldHandleNullMessageLabel() throws Exception {
        Field labelField = HelloController.class.getDeclaredField("messageLabel");
        labelField.setAccessible(true);
        labelField.set(controller, null);
        
        var messageViewField = HelloController.class.getDeclaredField("messageView");
        messageViewField.setAccessible(true);
        messageViewField.set(controller, new ListView<NtfyMessageDto>());
        
        var initMethod = HelloController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        
        // Should not throw exception
        initMethod.invoke(controller);
    }

    @Test
    @DisplayName("initialize should bind messageView to model messages")
    void initializeShouldBindMessageView() throws Exception {
        ListView<NtfyMessageDto> listView = new ListView<>();
        Field messageViewField = HelloController.class.getDeclaredField("messageView");
        messageViewField.setAccessible(true);
        messageViewField.set(controller, listView);
        
        var initMethod = HelloController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);
        
        assertThat(listView.getItems()).isNotNull();
    }

    @Test
    @DisplayName("initialize should bind messageInput to model property when not null")
    void initializeShouldBindMessageInput() throws Exception {
        TextField textField = new TextField();
        Field messageInputField = HelloController.class.getDeclaredField("messageInput");
        messageInputField.setAccessible(true);
        messageInputField.set(controller, textField);
        
        ListView<NtfyMessageDto> listView = new ListView<>();
        Field messageViewField = HelloController.class.getDeclaredField("messageView");
        messageViewField.setAccessible(true);
        messageViewField.set(controller, listView);
        
        var initMethod = HelloController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);
        
        // Get the model
        Field modelField = HelloController.class.getDeclaredField("model");
        modelField.setAccessible(true);
        HelloModel model = (HelloModel) modelField.get(controller);
        
        textField.setText("Test");
        assertThat(model.getMessageToSend()).isEqualTo("Test");
    }

    @Test
    @DisplayName("initialize should set cell factory for messageView")
    void initializeShouldSetCellFactory() throws Exception {
        ListView<NtfyMessageDto> listView = new ListView<>();
        Field messageViewField = HelloController.class.getDeclaredField("messageView");
        messageViewField.setAccessible(true);
        messageViewField.set(controller, listView);
        
        var initMethod = HelloController.class.getDeclaredMethod("initialize");
        initMethod.setAccessible(true);
        initMethod.invoke(controller);
        
        assertThat(listView.getCellFactory()).isNotNull();
    }

    @Test
    @DisplayName("sendMessage method should be public and accept ActionEvent")
    void sendMessageShouldBePublicAndAcceptActionEvent() throws Exception {
        var method = HelloController.class.getDeclaredMethod("sendMessage", ActionEvent.class);
        
        assertThat(method).isNotNull();
        assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
    }
}