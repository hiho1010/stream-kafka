package org.kafka.support;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;

@Slf4j
public record KafkaClientProxy(StreamBridge streamBridge, String bindingName) implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 1. 메서드의 파라미터(이벤트 객체) 확인
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("@KafkaClient 메서드는 최소 하나의 파라미터(이벤트)가 필요합니다.");
        }
        Object event = args[0];

        // 2. 로그 및 추적 (여기서 공통 로직 추가 가능)
        log.info("KafkaClient Proxy 실행 -> Binding: [{}], Payload: [{}]", bindingName, event);

        // 3. 실제 전송 (StreamBridge)
        boolean sent = streamBridge.send(bindingName, event);

        if (!sent) {
            throw new RuntimeException("메시지 발행 실패: " + bindingName);
        }

        return null; // void 메서드라고 가정
    }
}