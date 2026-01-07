package com.swemmanuelgz.users.impostorbackend.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Filtro de Rate Limiting para proteger la API contra abuso.
 * Usa el algoritmo Token Bucket con Bucket4j.
 * 
 * Límites configurados:
 * - Endpoints de autenticación: 10 requests/minuto (protección brute force)
 * - API general: 100 requests/minuto
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = Logger.getLogger(RateLimitingFilter.class.getName());
    
    // Cache de buckets por IP
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBucketCache = new ConcurrentHashMap<>();

    /**
     * Bucket para endpoints generales: 100 requests por minuto
     */
    private Bucket createGeneralBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(100)
                        .refillGreedy(100, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Bucket para autenticación: 10 requests por minuto (protección brute force)
     */
    private Bucket createAuthBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIP(request);
        String path = request.getServletPath();
        
        // Seleccionar bucket según el tipo de endpoint
        Bucket bucket;
        if (isAuthEndpoint(path)) {
            bucket = authBucketCache.computeIfAbsent(clientIp, k -> createAuthBucket());
        } else {
            bucket = bucketCache.computeIfAbsent(clientIp, k -> createGeneralBucket());
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Añadir headers informativos
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit excedido
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            
            logger.warning("Rate limit excedido para IP: " + clientIp + " en path: " + path);
            
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitSeconds));
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.getWriter().write(String.format(
                    "{\"error\": \"Too Many Requests\", \"message\": \"Has excedido el límite de peticiones. Intenta de nuevo en %d segundos.\", \"retryAfter\": %d}",
                    waitSeconds, waitSeconds
            ));
        }
    }

    private boolean isAuthEndpoint(String path) {
        return path.contains("/auth/login") || 
               path.contains("/auth/signup") || 
               path.contains("/auth/refresh");
    }

    private String getClientIP(HttpServletRequest request) {
        // Priorizar headers de proxy/load balancer (común en Hetzner con nginx/traefik)
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For puede tener múltiples IPs, tomar la primera
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // No filtrar WebSockets ni health checks
        return path.contains("/chat-socket") || 
               path.contains("/actuator") ||
               path.contains("/ws");
    }
}
