package com.jai.loglens;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogLensApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogLensApplication.class, args);
    }
}
