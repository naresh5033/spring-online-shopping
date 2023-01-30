package com.programmingtechie.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced // the webclient from spring web flux
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    
    } //lets inject this webclient bean into our orderservice
}
