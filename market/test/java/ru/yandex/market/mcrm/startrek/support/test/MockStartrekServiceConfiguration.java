package ru.yandex.market.mcrm.startrek.support.test;

import javax.annotation.Nonnull;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.mcrm.startrek.support.StartrekService;

@Configuration
@PropertySource("classpath:/test/startrek/support/test_startrek.properties")
public class MockStartrekServiceConfiguration {
    @Bean
    @Conditional(BeanExist.class)
    public StartrekService startrekServiceMock() {
        return Mockito.mock(StartrekService.class, invocation -> {
            throw new UnsupportedOperationException("Mock do not implement operation");
        });
    }

    private static class BeanExist implements Condition {
        @Override
        public boolean matches(ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
            return !context.getRegistry().containsBeanDefinition("startrekServiceImpl");
        }
    }
}
