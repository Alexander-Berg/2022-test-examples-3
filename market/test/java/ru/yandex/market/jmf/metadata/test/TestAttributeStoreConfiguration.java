package ru.yandex.market.jmf.metadata.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.jmf.metadata.AttributeStoreInitializationService;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeStoreConf;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStore;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStoreInitializer;

@Configuration
public class TestAttributeStoreConfiguration {

    @Bean
    public AttributeStoreInitializationService testAttributeStoreInitializationService() {
        return new AttributeStoreInitializationService() {

            @Override
            public AttributeStore initialize(AttributeStoreConf storeConf) {
                return new TestAttributeStore();
            }
        };
    }

    @Bean
    public AttributeStoreInitializer<TestAttributeStore, TestAttributeStoreConf> defaultAttributeStoreInitializer() {
        return new AttributeStoreInitializer<>() {
            @Override
            public Class<TestAttributeStoreConf> getType() {
                return null;
            }

            @Override
            public TestAttributeStore initialize(TestAttributeStoreConf conf) {
                return new TestAttributeStore();
            }
        };
    }

    private class TestAttributeStore implements AttributeStore {

        @Override
        public boolean isPersistable() {
            return true;
        }

        @Override
        public boolean isCopyable() {
            return true;
        }

        @Override
        public boolean isEditable() {
            return true;
        }

        @Override
        public boolean isFilterable() {
            return true;
        }

        @Override
        public boolean isSortable() {
            return true;
        }
    }

    private class TestAttributeStoreConf extends AttributeStoreConf {
    }
}
