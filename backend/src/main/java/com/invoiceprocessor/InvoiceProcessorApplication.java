package com.invoiceprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvoiceProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceProcessorApplication.class, args);
    }
}
