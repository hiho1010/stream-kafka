package org.kafka.publisher;

import lombok.extern.slf4j.Slf4j;
import org.kafka.event.BaseEvent;
import org.springframework.cloud.stream.function.StreamBridge;

@Slf4j
public record SpringCloudStreamPublisher(StreamBridge streamBridge) implements EventPublisher {

    @Override
    public void publish(String bindingName, BaseEvent event) {
        log.info("이벤트 발행 시작 [Binding: {}, ID: {}]", bindingName, event.getEventId());

        boolean sent = streamBridge.send(bindingName, event);

        if (!sent) {
            log.error("이벤트 발행 실패 [Binding: {}, ID: {}]", bindingName, event.getEventId());
            throw new RuntimeException("이벤트 발행 실패: " + bindingName);
        }

        log.info("이벤트 발행 성공 [Binding: {}, ID: {}]", bindingName, event.getEventId());
    }
}