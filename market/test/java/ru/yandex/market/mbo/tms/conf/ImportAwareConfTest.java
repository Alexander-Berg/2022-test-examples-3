package ru.yandex.market.mbo.tms.conf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.type.AnnotationMetadata;

import javax.annotation.Resource;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class ImportAwareConfTest {

    private static final Logger log = LogManager.getLogger(ru.yandex.market.mbo.tms.MboTmsMain.class);

    @Configuration
    @Import({
        AConfig.class,
        BConfig.class,
    })
    public static class MixedConfig {

        @Resource(name = "a")
        private A a;

        @Resource(name = "b")
        private B b;

        @Bean
        public C c() {
            return new C(a, b);
        }
    }

    @Configuration
    public static class AConfig {
        @Bean
        public A a() {
            return new A("test-data-a");
        }
    }

    @Configuration
    @Import(AConfig.class)
    public static class BConfig implements ImportAware {

        private final AConfig aConfig;

        @Autowired
        public BConfig(AConfig aConfig) {
            this.aConfig = aConfig;
        }

        @Bean
        public B b() {
            return new B("test-data-3");
        }

        @Override
        public void setImportMetadata(AnnotationMetadata importMetadata) {

        }
    }

    @Test
    public void testInjectImports() {
        try (GenericApplicationContext context = new AnnotationConfigApplicationContext(MixedConfig.class)) {
            context.start();

            Assertions.assertThat(context.getBean("a")).isNotNull();
            Assertions.assertThat(context.getBean("b")).isNotNull();
            Assertions.assertThat(context.getBean("c")).isNotNull();
        }
    }

    public static class A {
        private String data;

        public A(String data) {
            this.data = data;
        }
    }

    public static class B {
        private String data;

        public B(String data) {
            this.data = data;
        }
    }

    public static class C {
        private final A a;
        private final B b;

        public C(A a,
                 B b) {
            this.a = a;
            this.b = b;
        }
    }

}

