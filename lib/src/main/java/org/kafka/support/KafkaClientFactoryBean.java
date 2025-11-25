package org.kafka.support;

import java.lang.reflect.Proxy;
import lombok.Setter;
import org.kafka.annotation.KafkaClient;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.stream.function.StreamBridge;

@Setter
public class KafkaClientFactoryBean<T> implements FactoryBean<T> {

    private Class<T> interfaceType;
    private StreamBridge streamBridge;

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() {
        KafkaClient annotation = interfaceType.getAnnotation(KafkaClient.class);
        String bindingName = annotation.value();

        return (T) Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class[]{interfaceType}, // 여기서 T 타입을 구현하도록 강제했으므로 안전함
            new KafkaClientProxy(streamBridge, bindingName)
        );
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceType;
    }
}