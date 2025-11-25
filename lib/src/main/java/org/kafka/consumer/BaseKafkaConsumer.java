package org.kafka.consumer;

import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseKafkaConsumer<T> {

    protected abstract void consume(T event);

    public Consumer<T> kafkaListener() {
        return event -> {
            try {
                log.info("메시지 수신 시작: {}", event);
                consume(event);
                log.info("메시지 처리 완료");
            } catch (Exception e) {
                log.error("메시지 처리 중 에러 발생", e);
                throw e;
            }
        };
    }
}