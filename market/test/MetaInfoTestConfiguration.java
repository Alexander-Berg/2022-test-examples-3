package ru.yandex.market.jmf.metainfo.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import ru.yandex.market.jmf.hibernate.test.HibernateSupportTestConfiguration;
import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.metainfo.MetaInfoConfiguration;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageService;
import ru.yandex.market.jmf.metainfo.SessionFactoryBeanExistsCondition;

@Configuration
@Import({
        MetaInfoConfiguration.class,
        HibernateSupportTestConfiguration.class,
        LockServiceTestConfiguration.class,
})
@ComponentScan("ru.yandex.market.jmf.metainfo.test.impl")
public class MetaInfoTestConfiguration {
    @Bean
    @Conditional(SessionFactoryBeanNotExists.class)
    public MetaInfoStorageService mockMetaInfoStorageService() {
        return Mockito.mock(MetaInfoStorageService.class);
    }

    private static class SessionFactoryBeanNotExists extends SessionFactoryBeanExistsCondition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !super.matches(context, metadata);
        }
    }
}
