package com.pruebatecnica.distribucion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DistribucionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistribucionApplication.class, args);
    }
}
