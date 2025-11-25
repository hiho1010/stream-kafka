package org.kafka.support;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.kafka.annotation.EnableKafkaClients;
import org.kafka.annotation.KafkaClient;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

public class KafkaClientRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    @Override
    public void setEnvironment(@NotNull Environment environment) {
    }

    @Override
    public void registerBeanDefinitions(@NotNull AnnotationMetadata importingClassMetadata, @NotNull BeanDefinitionRegistry registry) {
        // 1. 스캐너 설정: 인터페이스도 찾을 수 있도록 설정 (기본값은 클래스만 찾음)
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent() && beanDefinition.getMetadata().isInterface();
            }
        };

        // @KafkaClient가 붙은 것만 찾음
        scanner.addIncludeFilter(new AnnotationTypeFilter(KafkaClient.class));

        // 2. 스캔할 패키지 결정 (메인 애플리케이션 패키지 기준)
        Set<String> basePackages = getBasePackages(importingClassMetadata);

        // 3. 스캔 및 빈 등록
        for (String basePackage : basePackages) {
            for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage)) {
                registerKafkaClient(registry, candidate);
            }
        }
    }

    private void registerKafkaClient(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        String interfaceName = candidate.getBeanClassName();

        // KafkaClientFactoryBean을 사용하여 빈 정의 생성
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(KafkaClientFactoryBean.class);
        builder.addPropertyValue("interfaceType", interfaceName);
        builder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE); // StreamBridge 자동 주입

        // 빈 등록 (이름은 인터페이스 이름 그대로 사용)
        assert interfaceName != null;
        registry.registerBeanDefinition(interfaceName, builder.getBeanDefinition());
    }

    private Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(
            EnableKafkaClients.class.getName());
        Set<String> basePackages = new HashSet<>();

        if (attributes != null && attributes.containsKey("basePackages")) {
            String[] packages = (String[]) attributes.get("basePackages");
            if (packages != null && packages.length > 0) {
                Collections.addAll(basePackages, packages);
            }
        }

        // 속성에 아무것도 안 적었으면, 해당 어노테이션을 붙인 클래스의 패키지를 기본값으로 사용
        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }
}