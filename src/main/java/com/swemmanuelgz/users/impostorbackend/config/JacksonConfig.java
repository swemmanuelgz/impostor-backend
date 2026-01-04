package com.swemmanuelgz.users.impostorbackend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Configuración de Jackson para manejar formatos de fecha flexibles
 * Especialmente importante para WebSocket donde los clientes pueden enviar
 * timestamps sin zona horaria (ej: Flutter)
 */
@Configuration
public class JacksonConfig {

    /**
     * ObjectMapper configurado para manejar Instant con formatos flexibles
     * Acepta tanto "2025-12-20T03:34:43.522865" como "2025-12-20T03:34:43.522865Z"
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // Deserializador flexible para Instant que acepta formatos sin "Z"
        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .optionalStart()
                .appendZoneOrOffsetId()
                .optionalEnd()
                .toFormatter();
        
        javaTimeModule.addDeserializer(Instant.class, new FlexibleInstantDeserializer(flexibleFormatter));
        
        mapper.registerModule(javaTimeModule);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
        return mapper;
    }

    /**
     * Converter de mensajes para WebSocket con el ObjectMapper configurado
     */
    @Bean
    public MappingJackson2MessageConverter mappingJackson2MessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }
}
