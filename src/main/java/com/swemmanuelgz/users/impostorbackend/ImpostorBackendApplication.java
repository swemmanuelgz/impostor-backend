package com.swemmanuelgz.users.impostorbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ImpostorBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImpostorBackendApplication.class, args);
    }

}
