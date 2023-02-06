package ru.yandex.market.javaframework.internal.beanfactory.mapbyqualifier;

import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class MapByQualifierBeanFactoryTest {

    @Test
    public void mapByQualifierTest() {
        final AnnotationConfigApplicationContext applicationContext =
            new AnnotationConfigApplicationContext(new MapByQualifierBeanFactory());
        applicationContext.register(QualifiedBeansConfiguration.class);
        applicationContext.refresh();

        final DummyMapWrapper dummyMapWrapper = applicationContext.getBean(DummyMapWrapper.class);
        final Map<String, Dummy> dummyMap = dummyMapWrapper.getDummyMap();

        assertThat(dummyMap.size()).isEqualTo(2);
        assertThat(dummyMap.get(QualifiedBeansConfiguration.QUALIFIER_1)).isNotNull();
        assertThat(dummyMap.get(QualifiedBeansConfiguration.QUALIFIER_2)).isNotNull();
    }

    protected static class Dummy {

    }

    protected static class DummyMapWrapper {
        private final Map<String, Dummy> dummyMap;

        public DummyMapWrapper(Map<String, Dummy> dummyMap) {
            this.dummyMap = dummyMap;
        }

        public Map<String, Dummy> getDummyMap() {
            return dummyMap;
        }
    }

    @Configuration
    protected static class QualifiedBeansConfiguration {

        public static final String QUALIFIER_1 = "qualifier_1";
        public static final String QUALIFIER_2 = "qualifier_2";

        @Qualifier(QUALIFIER_1)
        @Bean
        public Dummy dummy1() {
            return new Dummy();
        }

        @Qualifier(QUALIFIER_2)
        @Bean
        public Dummy dummy2() {
            return new Dummy();
        }

        @Bean
        public DummyMapWrapper dummyMapWrapper(@MappedByQualifier Map<String, Dummy> dummyMap) {
            return new DummyMapWrapper(dummyMap);
        }
    }
}
