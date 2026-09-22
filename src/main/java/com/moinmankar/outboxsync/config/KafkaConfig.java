package com.moinmankar.outboxsync.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic orderEventsTopic() {
        return new NewTopic("order-events", 3, (short) 1);
    }

    @Bean
    public NewTopic orderEventsDltTopic() {
        return new NewTopic(
                "order-events.DLT",
                3,
                (short) 1
        );
    }
}