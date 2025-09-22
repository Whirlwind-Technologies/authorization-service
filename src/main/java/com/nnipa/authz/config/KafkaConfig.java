package com.nnipa.authz.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for authz-service with byte array handling.
 * We'll handle protobuf deserialization manually in the consumer methods.
 */
@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:kafka-0:29092,kafka-1:29092,kafka-2:29092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:authz-service}")
    private String groupId;

    /**
     * Producer factory for byte[] messages (Protobuf serialized).
     */
    @Bean
    public ProducerFactory<String, byte[]> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Consumer factory for byte[] messages with manual acknowledgment.
     */
    @Bean
    public ConsumerFactory<String, byte[]> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory with manual acknowledgment mode.
     * No custom converter - we'll handle deserialization in the consumer methods.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Enable manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    log.error("Error processing Kafka message: topic={}, partition={}, offset={}, key={}",
                            consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(),
                            consumerRecord.key(), exception);
                }
        ));

        // Set concurrency (number of consumer threads)
        factory.setConcurrency(3);

        return factory;
    }

    /**
     * Kafka template for sending byte[] messages.
     */
    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Topic definitions for authz events
    @Bean
    public NewTopic authorizationCheckedTopic() {
        return TopicBuilder.name("nnipa.events.authz.checked")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic roleCreatedTopic() {
        return TopicBuilder.name("nnipa.events.authz.role-created")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic roleUpdatedTopic() {
        return TopicBuilder.name("nnipa.events.authz.role-updated")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic roleDeletedTopic() {
        return TopicBuilder.name("nnipa.events.authz.role-deleted")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic roleAssignedTopic() {
        return TopicBuilder.name("nnipa.events.authz.role-assigned")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic roleRevokedTopic() {
        return TopicBuilder.name("nnipa.events.authz.role-revoked")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic permissionGrantedTopic() {
        return TopicBuilder.name("nnipa.events.authz.permission-granted")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic permissionRevokedTopic() {
        return TopicBuilder.name("nnipa.events.authz.permission-revoked")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic policyCreatedTopic() {
        return TopicBuilder.name("nnipa.events.authz.policy-created")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic policyEvaluatedTopic() {
        return TopicBuilder.name("nnipa.events.authz.policy-evaluated")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }
}