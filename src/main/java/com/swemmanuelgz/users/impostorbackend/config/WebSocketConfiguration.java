package com.swemmanuelgz.users.impostorbackend.config;

import com.swemmanuelgz.users.impostorbackend.utils.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.logging.Logger;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final Logger logger = Logger.getLogger(WebSocketConfiguration.class.getName());
//
//    @Autowired
//    private ApiKeyStompInterceptor apiKeyStompInterceptor;
    @Bean
    public ThreadPoolTaskScheduler webSocketTaskScheduler() {
        AnsiColors.infoLog(logger, "Configurando el TaskScheduler");
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-thread-");
        taskScheduler.initialize();
        return taskScheduler;
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        AnsiColors.infoLog( logger, "Configurando el broker de mensajes: ");
       registry.enableSimpleBroker("/topic", "/queue","/user") // Añadido /queue para notificaciones personales
                       .setHeartbeatValue(new long[]{10000,10000})
                       .setTaskScheduler(webSocketTaskScheduler());
       registry.setApplicationDestinationPrefixes("/app");
       registry.setUserDestinationPrefix("/user"); // Prefijo para destinos de usuario específico
    }



    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        AnsiColors.infoLog(logger,"Registrando endpoint STOMP: /chat-socket");
        registry.addEndpoint("/chat-socket")
                //.setAllowedOrigins("http://127.0.0.1:5500","http://127.0.0.1:3000")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                //.setSessionCookieNeeded(true);
    }


    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {    
        AnsiColors.infoLog(logger, "Configurando canal de entrada de cliente");
        registration.taskExecutor().corePoolSize(5);

        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                try {
                    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                        // Obtener el userId desde los headers de conexión
                        String userId = accessor.getFirstNativeHeader("userId");
                        
                        if (userId != null && !userId.isEmpty()) {
                            AnsiColors.successLog(logger, "Cliente conectandose con userId: " + userId);
                            
                            // Crear un Principal con el userId para que Spring pueda enrutar mensajes
                            accessor.setUser(() -> userId);
                        } else {
                            AnsiColors.infoLog(logger, "Cliente conectandose SIN userId en headers - usando anonymous");
                            accessor.setUser(new AnonymousAuthenticationToken(
                                    "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
                        }
                    }
                    return message;
                } catch (Exception e) {
                    AnsiColors.errorLog(logger, "Error en interceptor: " + e.getMessage());
                    return message; // Importante: seguir devolviendo el mensaje original
                }
            }
        });
    }
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Aumentar el tamaño del pool de hilos para mensajes salientes también
        registration.taskExecutor().corePoolSize(5).maxPoolSize(10);
    }
}
