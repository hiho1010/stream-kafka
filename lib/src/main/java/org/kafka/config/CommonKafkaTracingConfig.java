package org.kafka.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;

@AutoConfiguration
@ConditionalOnBean(ObservationRegistry.class)
public class CommonKafkaTracingConfig {

    @Bean
    public ContainerCustomizer<Object, Object, AbstractMessageListenerContainer<Object, Object>> tracingContainerCustomizer() {
        return container -> container.getContainerProperties().setObservationEnabled(true);
    }
}