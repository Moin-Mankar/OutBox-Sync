package com.moinmankar.outboxsync.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class DlqReplayService {

    private static final String DLT_TOPIC = "order-events.DLT";
    private static final String MAIN_TOPIC = "order-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public DlqReplayService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void replay(String key, String payload) {

        kafkaTemplate.send(
                MAIN_TOPIC,
                key,
                payload
        );
    }
}