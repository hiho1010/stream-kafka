package org.kafka.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

public class CommonKafkaErrorConfig {
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
        KafkaTemplate<Object, Object> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, ex) -> {
            // 원한다면 여기서 에러 종류에 따라 다른 토픽으로 보내는 로직 커스텀 가능
            // 현재는 기본값(토픽명 + .DLT) 사용
            return new TopicPartition(record.topic() + ".DLT", record.partition());
        });
    }

    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        // 1초 간격으로 최대 2번 더 시도 (최초 시도 포함 총 3번 처리)
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ListenerContainerCustomizer<AbstractMessageListenerContainer<?, ?>> customizer(DefaultErrorHandler errorHandler) {
        return (container, destinationName, group) -> container.setCommonErrorHandler(errorHandler);
    }
}
