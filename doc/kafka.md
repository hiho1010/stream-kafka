Common Kafka Library (MSA Support)
이 라이브러리는 Spring Cloud Stream을 기반으로, MSA 환경에서 카프카 메시징을 쉽고 표준화된 방식으로 사용할 수 있도록 돕는 공용 모듈입니다.

복잡한 KafkaTemplate 설정이나 StreamBridge 구현 없이, **인터페이스 선언(@KafkaClient)**만으로 메시지를 발행할 수 있으며, 자동 재시도, DLQ(Dead Letter Queue), 분산 추적(Tracing) 기능이 기본 내장되어 있습니다.

🛠 Architecture
컴포넌트	역할 및 동작 원리
@KafkaClient	Producer용 마커 어노테이션. bindingName을 속성으로 가짐.
KafkaClientProxy	인터페이스 메서드 호출을 가로채서 StreamBridge.send()를 대신 실행하는 프록시.
KafkaClientRegistrar	컴포넌트 스캔을 통해 @KafkaClient 인터페이스를 찾아 스프링 빈으로 등록.
@EnableKafkaClients	위 Registrar를 작동시키는 스위치 (Main Application에 부착).
CommonKafkaErrorConfig	재시도(3회) & DLQ 자동 설정. 실패 시 .DLT 토픽으로 이동.
CommonKafkaTracingConfig	Micrometer Tracing 자동 적용. traceId가 카프카 헤더를 통해 전파됨.
CommonKafkaConfig	ObjectMapper (Java 8 날짜 지원) 및 JSON 변환기 표준화.
🚀 Getting Started
1. 설치 (Installation)

서비스의 build.gradle에 의존성을 추가합니다.

Groovy
dependencies {
implementation project(':common-kafka') // 공용 라이브러리 모듈
}
2. 활성화 (Configuration)

메인 애플리케이션 클래스에 @EnableKafkaClients 어노테이션을 추가하여 기능을 활성화합니다.

Java
import com.delivery.common.kafka.annotation.EnableKafkaClients;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableKafkaClients // 라이브러리 활성화
public class OrderApplication {
public static void main(String[] args) {
SpringApplication.run(OrderApplication.class, args);
}
}
📖 Usage Guide
📤 1. 메시지 보내기 (Producer)

구현체를 직접 만들 필요가 없습니다. 인터페이스를 정의하고 @KafkaClient를 붙이면, 런타임에 자동으로 구현체가 생성됩니다.

인터페이스 작성

Java
package com.delivery.orders.client;

import com.delivery.common.kafka.annotation.KafkaClient;
import com.delivery.orders.dto.OrderCreatedEvent;

// value는 application.yml의 bindings 이름과 일치해야 함
@KafkaClient("orders-out-0")
public interface OrderEventProducer {

    // 메서드 이름은 자유, 파라미터로 전송할 객체를 전달
    void sendOrderCreated(OrderCreatedEvent event);
}
서비스 로직에서 사용

Java
@Service
@RequiredArgsConstructor
public class OrderService {

    // 별도의 구현 없이 인터페이스를 바로 주입받아 사용 (FeignClient와 유사)
    private final OrderEventProducer eventProducer;

    public void createOrder(OrderDto dto) {
        // ... 주문 로직 ...
        
        // 메서드 호출 시 카프카 메시지 자동 발행
        eventProducer.sendOrderCreated(new OrderCreatedEvent(dto.getId()));
    }
}
📥 2. 메시지 받기 (Consumer)

메시지를 수신할 때는 java.util.function.Consumer를 Bean으로 등록합니다.

Java
@Configuration
public class OrderConsumerConfig {

    @Bean
    public Consumer<OrderCreatedEvent> orderCreatedListener(OrderService orderService) {
        return event -> {
            // 비즈니스 로직 작성
            // * 에러 발생 시: 3회 재시도 -> 실패 시 DLQ 토픽으로 자동 이동
            orderService.process(event);
        };
    }
}
⚙️ Configuration (application.yml)
코드에서 사용한 이름(binding-name)과 실제 카프카 토픽을 매핑합니다.

YAML
spring:
cloud:
function:
# Consumer 함수 이름 등록 (Producer는 등록 불필요)
definition: orderCreatedListener

    stream:
      kafka:
        binder:
          brokers: localhost:9092 # 카프카 브로커 주소
          
      bindings:
        # [Producer] 인터페이스의 @KafkaClient("orders-out-0")와 매핑
        orders-out-0:
          destination: delivery-orders # 실제 카프카 토픽 이름
          content-type: application/json

        # [Consumer] 빈 이름(orderCreatedListener) + -in-0
        orderCreatedListener-in-0:
          destination: delivery-orders # 구독할 토픽
          group: order-group # 컨슈머 그룹 (필수)
          content-type: application/json
🎁 Features (내장 기능)
이 라이브러리를 사용하면 별도 설정 없이 아래 기능이 자동으로 적용됩니다.

1. Reliability (안정성)

Retry: 메시지 처리 중 예외 발생 시 1초 간격으로 최대 3회 재시도합니다.

DLQ (Dead Letter Queue): 재시도 후에도 실패하면, 메시지를 버리지 않고 [원본토픽명].DLT 토픽으로 자동 이동시킵니다.

2. Observability (분산 추적)

Micrometer Tracing: 메시지 발행/수신 시 TraceId가 카프카 헤더에 자동으로 포함됩니다.

Log Integration: 각 서비스 로그에 [Service, traceId, spanId]가 표기되어, MSA 전체 흐름을 쉽게 추적할 수 있습니다.

3. Standardization (표준화)

JSON Serialization: ObjectMapper 설정을 내장하여, Java 8 LocalDateTime 등의 날짜 타입도 ISO 포맷(2024-11-20T10:00:00)으로 문제없이 처리됩니다.

❓ FAQ
Q. 인터페이스 구현체(Impl 클래스)를 만들지 않았는데 에러가 나지 않나요?

A. 네, 라이브러리가 애플리케이션 시작 시점에 Dynamic Proxy 기술을 이용해 가짜 구현체를 생성하여 Spring Bean으로 등록해 줍니다. Spring Cloud OpenFeign과 동일한 원리입니다.

Q. 운영 환경에서 토픽 이름을 바꾸고 싶어요.

A. 소스 코드를 수정할 필요가 없습니다. application.yml (또는 Config Server)의 destination 값만 변경하고 재배포하면 됩니다.