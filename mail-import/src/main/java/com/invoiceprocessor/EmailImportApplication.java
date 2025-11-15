package com.invoiceprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.invoiceprocessor.emailimport",
        "com.invoiceprocessor.adapter.out.email",
        "com.invoiceprocessor.adapter.out.db",
        "com.invoiceprocessor.application.port.out",
        "com.invoiceprocessor.domain.entity"
})
@EnableScheduling
public class EmailImportApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailImportApplication.class, args);
    }
}

