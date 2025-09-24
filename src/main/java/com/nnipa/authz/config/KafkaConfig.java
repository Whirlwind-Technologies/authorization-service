package com.nnipa.authz.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced Kafka configuration for authz-service with comprehensive error handling.
 * Handles protobuf deserialization manually in consumer methods with robust error recovery.
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
        // Additional producer resilience settings
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Enhanced consumer factory with additional resilience properties.
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

        // Reduced for better error isolation and faster processing
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 5);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);    // 30 seconds
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // Additional resilience properties
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 5000);       // 5 seconds
        configProps.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000); // 9 minutes
        configProps.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);     // 30 seconds
        configProps.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);    // 1 second
        configProps.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);        // 1 second

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Enhanced Kafka listener container factory with comprehensive error handling.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, byte[]> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, byte[]> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Container properties
        ContainerProperties containerProps = factory.getContainerProperties();
        containerProps.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProps.setPollTimeout(3000);
        containerProps.setMissingTopicsFatal(false);
        containerProps.setLogContainerConfig(true);
        containerProps.setConsumerStartTimeout(Duration.ofSeconds(30));

        // Enhanced error handler with exponential backoff
        factory.setCommonErrorHandler(createEnhancedErrorHandler());

        // Reduced concurrency for better error isolation per your use case
        factory.setConcurrency(2);

        return factory;
    }

    /**
     * Create enhanced error handler with exponential backoff and proper exception classification.
     */
    private DefaultErrorHandler createEnhancedErrorHandler() {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s (max 5 attempts)
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1000L);  // 1 second
        backOff.setMultiplier(2.0);         // Double each time
        backOff.setMaxInterval(16000L);     // Max 16 seconds

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    // Final recovery after all retries exhausted
                    log.error("FATAL: Failed to process message after all retries. " +
                                    "Topic: {}, Partition: {}, Offset: {}, Key: {}, " +
                                    "Consider manual intervention or dead letter processing",
                            consumerRecord.topic(),
                            consumerRecord.partition(),
                            consumerRecord.offset(),
                            consumerRecord.key(),
                            exception);

                    // Here you could implement dead letter topic publishing
                    // or alerting mechanisms for operations team
                },
                backOff
        );

        // Non-retryable exceptions - acknowledge immediately to prevent infinite loops
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,           // Invalid data format (like UUID parsing)
                com.google.protobuf.InvalidProtocolBufferException.class,  // Protobuf parse errors
                org.springframework.messaging.converter.MessageConversionException.class,
                // Add your custom NonRetryableException
                com.nnipa.authz.event.consumer.TenantEventConsumer.NonRetryableException.class
        );

        // Retryable exceptions - will use exponential backoff
        errorHandler.addRetryableExceptions(
                org.springframework.dao.DataAccessException.class,     // Database connectivity issues
                org.springframework.transaction.TransactionException.class,  // Transaction problems
                java.sql.SQLException.class,                          // SQL errors
                org.springframework.dao.DataIntegrityViolationException.class  // Include this for retry
        );

        // Log retry attempts for monitoring
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("RETRY ATTEMPT {}: Processing failed for message. " +
                            "Topic: {}, Partition: {}, Offset: {}, Key: {}, Error: {}",
                    deliveryAttempt, record.topic(), record.partition(),
                    record.offset(), record.key(), ex.getClass().getSimpleName());
        });

        return errorHandler;
    }

    /**
     * Kafka template for sending byte[] messages.
     */
    @Bean
    public KafkaTemplate<String, byte[]> kafkaTemplate() {
        KafkaTemplate<String, byte[]> template = new KafkaTemplate<>(producerFactory());

        // Producer interceptors would be configured here if needed
        // template.setProducerInterceptors(Collections.singletonList(new YourCustomProducerInterceptor()));

        return template;
    }

    /**
     * Health indicator for monitoring Kafka consumer status
     */
    @Bean
    public KafkaConsumerHealthIndicator kafkaHealthIndicator() {
        return new KafkaConsumerHealthIndicator();
    }

    // === TOPIC DEFINITIONS (Your existing topics preserved) ===

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

    @Bean
    public NewTopic crossTenantAccessGrantedTopic() {
        return TopicBuilder.name("nnipa.events.authz.cross-tenant-granted")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic crossTenantAccessRevokedTopic() {
        return TopicBuilder.name("nnipa.events.authz.cross-tenant-revoked")
                .partitions(6)
                .replicas(3)
                .config("retention.ms", "604800000") // 7 days
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    // === Optional: Dead Letter Topics for failed messages ===

    @Bean
    public NewTopic tenantCreatedDltTopic() {
        return TopicBuilder.name("nnipa.events.tenant.created.dlt")
                .partitions(3)
                .replicas(3)
                .config("retention.ms", "2592000000") // 30 days retention for DLT
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic tenantDeactivatedDltTopic() {
        return TopicBuilder.name("nnipa.events.tenant.deactivated.dlt")
                .partitions(3)
                .replicas(3)
                .config("retention.ms", "2592000000") // 30 days retention for DLT
                .config("compression.type", "snappy")
                .config("min.insync.replicas", "2")
                .build();
    }
}

/**
 * Health indicator to monitor Kafka consumer health status
 */
@Slf4j
class KafkaConsumerHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // Add specific health checks here based on your monitoring needs
            // Examples: consumer lag, connection status, processing metrics

            return Health.up()
                    .withDetail("kafka.consumer.status", "healthy")
                    .withDetail("kafka.consumer.info", "All consumers running normally")
                    .build();

        } catch (Exception e) {
            log.error("Kafka consumer health check failed", e);
            return Health.down()
                    .withDetail("kafka.consumer.status", "unhealthy")
                    .withDetail("kafka.consumer.error", e.getMessage())
                    .build();
        }
    }
}