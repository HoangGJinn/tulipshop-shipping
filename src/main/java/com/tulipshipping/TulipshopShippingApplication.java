package com.tulipshipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // Import cái này
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate; // Import cái này

@SpringBootApplication
@EnableAsync
public class TulipshopShippingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TulipshopShippingApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
