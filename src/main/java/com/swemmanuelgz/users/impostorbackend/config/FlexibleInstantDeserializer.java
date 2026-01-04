package com.swemmanuelgz.users.impostorbackend.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Deserializador flexible para Instant que acepta múltiples formatos:
 * - ISO 8601 con Z: "2025-12-20T03:34:43.522865Z"
 * - ISO 8601 con offset: "2025-12-20T03:34:43.522865+01:00"
 * - ISO 8601 sin zona horaria: "2025-12-20T03:34:43.522865" (asume UTC)
 * - Epoch milliseconds: 1703041683000
 */
public class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {

    private final DateTimeFormatter formatter;

    public FlexibleInstantDeserializer(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        text = text.trim();
        
        // Si es un número, asumir epoch milliseconds
        if (text.matches("^\\d+$")) {
            return Instant.ofEpochMilli(Long.parseLong(text));
        }
        
        // Intentar parsear como Instant estándar (con Z o offset)
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            // Si falla, intentar como LocalDateTime y convertir a UTC
            try {
                DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                        .optionalStart()
                        .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                        .optionalEnd()
                        .toFormatter();
                
                LocalDateTime localDateTime = LocalDateTime.parse(text, flexibleFormatter);
                // Asumimos que si no tiene zona horaria, es UTC
                return localDateTime.toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e2) {
                throw new IOException("No se pudo parsear el timestamp: " + text, e2);
            }
        }
    }
}
