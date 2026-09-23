package com.moinmankar.outboxsync;

import com.moinmankar.outboxsync.config.KafkaConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = KafkaRetryDltTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = {"order-events", "order-events.DLT"})
class KafkaRetryDltTest {

    @Autowired
    KafkaTemplate<String, String> template;

    @Autowired
    TestConsumer consumer;

    @Test
    void poisonMessageIsRetriedThenRoutedToDlt() throws Exception {

        template.send("order-events", "POISON").get(20, TimeUnit.SECONDS);

        String dead = consumer.dltMessages.poll(90, TimeUnit.SECONDS);

        assertThat(dead).isEqualTo("POISON");
        assertThat(consumer.attempts.get()).isGreaterThanOrEqualTo(2);
    }

    @EnableKafka
    @Configuration
    @Import(KafkaConsumerConfig.class)
    static class TestConfig {

        @Value("${spring.embedded.kafka.brokers}")
        String brokers;

        @Bean
        ProducerFactory<String, String> producerFactory() {
            Map<String, Object> config = new HashMap<>();
            config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
            config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            return new DefaultKafkaProducerFactory<>(config);
        }

        @Bean
        KafkaTemplate<String, String> kafkaTemplate() {
            return new KafkaTemplate<>(producerFactory());
        }

        @Bean
        org.springframework.kafka.core.ConsumerFactory<String, String> consumerFactory() {
            Map<String, Object> config = new HashMap<>();
            config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
            config.put(ConsumerConfig.GROUP_ID_CONFIG, "order-dlt-test");
            config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            return new DefaultKafkaConsumerFactory<>(config);
        }

        @Bean
        TestConsumer testConsumer() {
            return new TestConsumer();
        }
    }

    static class TestConsumer {

        final BlockingQueue<String> dltMessages = new LinkedBlockingQueue<>();
        final AtomicInteger attempts = new AtomicInteger();

        @KafkaListener(topics = "order-events", groupId = "order-dlt-test")
        public void main(String value) {
            attempts.incrementAndGet();
            throw new RuntimeException("poison: " + value);
        }

        @KafkaListener(topics = "order-events.DLT", groupId = "dlt-listener")
        public void dead(String value) {
            dltMessages.add(value);
        }
    }
}
