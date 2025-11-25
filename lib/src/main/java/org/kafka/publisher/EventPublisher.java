package org.kafka.publisher;


import org.kafka.event.BaseEvent;

public interface EventPublisher {
    /**
     * 이벤트를 발행합니다.
     * @param bindingName application.yml에 설정된 바인딩 이름 (예: orders-out-0)
     * @param event 전송할 이벤트 객체
     */
    void publish(String bindingName, BaseEvent event);
}