package com.example.javaai.websocket.football;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * STOMP CONNECT 时将 Handshake 中的 principalName 写入 User。
 */
@Component
@Slf4j
public class StompChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Object principalName = accessor.getSessionAttributes().get(StompHandshakeInterceptor.PRINCIPAL_NAME_ATTR);
            if (principalName instanceof String name) {
                accessor.setUser(new StompPrincipal(name));
                log.debug("STOMP CONNECT principal={}", name);
            }
        }
        return message;
    }
}
