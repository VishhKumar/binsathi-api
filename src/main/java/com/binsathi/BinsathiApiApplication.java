package com.binsathi;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BinsathiApiApplication {

    public static void main(String[] args) {
        // Always run in UTC so laptop and server behave the same, and
        // PostgreSQL never sees legacy names like "Asia/Calcutta".
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BinsathiApiApplication.class, args);
    }
}