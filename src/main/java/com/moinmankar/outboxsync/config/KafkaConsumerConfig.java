package com.moinmankar.outboxsync.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate
    ) {

        return new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {

                    log.error(
                            "[DLQ] Publishing failed event. topic={}, partition={}, offset={}",
                            record.topic(),
                            record.partition(),
                            record.offset()
                    );

                    return new org.apache.kafka.common.TopicPartition(
                            record.topic() + ".DLT",
                            record.partition()
                    );
                }
        );
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer
    ) {

        ExponentialBackOff backOff =
                new ExponentialBackOff(1000L, 2.0);

        backOff.setMaxElapsedTime(15000L);

        return new DefaultErrorHandler(
                recoverer,
                backOff
        );
    }
}