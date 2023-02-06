package ru.yandex.market.jmf.idm.integration.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeStoreConf;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStore;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStoreInitializer;
import ru.yandex.market.jmf.metadata.metaclass.AttributeType;

@Configuration
@Import(IdmIntegrationTestConfiguration.class)
public class InternalIdmIntegrationTestConfiguration {
    @Bean
    public AttributeStoreInitializer defaultAttributeStoreInitializer() {
        return new AttributeStoreInitializer() {
            @Override
            public Class getType() {
                return null;
            }

            @Override
            public AttributeStore initialize(AttributeStoreConf conf) {
                return new AttributeStore() {
                    @Override
                    public boolean isPersistable() {
                        return true;
                    }

                    @Override
                    public boolean isCopyable() {
                        return false;
                    }

                    @Override
                    public boolean isEditable() {
                        return true;
                    }

                    @Override
                    public boolean isFilterable() {
                        return false;
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }
                };
            }
        };
    }

    @Bean
    public MetadataAttributeTypeInitializer defaultMetadataAttributeTypeInitializer() {
        return new MetadataAttributeTypeInitializer() {
            @Override
            public String getCode() {
                return null;
            }

            @Override
            public Class getType() {
                return null;
            }

            @Override
            public boolean isApplicable(String typeCode, AttributeTypeConf typeConf) {
                return true;
            }

            @Override
            public AttributeType initialize(Attribute attribute, AttributeTypeConf conf) {
                return new AttributeType() {
                    @Override
                    public String getCode() {
                        return null;
                    }

                    @Override
                    public Map<String, Object> properties() {
                        return Map.of();
                    }

                    @Override
                    public boolean isCopyable() {
                        return true;
                    }

                    @Override
                    public <T extends AttributeTypeConf> T getAttributeTypeConf() {
                        return null;
                    }

                    @Override
                    public boolean mayNaturalId() {
                        return false;
                    }

                    @Override
                    public boolean mayVersioned() {
                        return false;
                    }

                    @Override
                    public boolean mayUnique() {
                        return false;
                    }

                    @Override
                    public boolean supportsSorting() {
                        return false;
                    }

                    @Override
                    public String sortingPath() {
                        return null;
                    }

                    @Override
                    public boolean mayEditable() {
                        return true;
                    }

                    @Override
                    public boolean mayFilterable() {
                        return true;
                    }
                };
            }

            @Override
            public AttributeType initializeOverride(Attribute attribute,
                                                    AttributeTypeConf conf) {
                return initialize(attribute, conf);
            }

            @Override
            public void postInitialize(AttributeType type, Map index) {

            }

            @Override
            public Collection<Fqn> getRelated(AttributeType type, Map index) {
                return List.of();
            }

            @Override
            public AttributeTypeConf getVersionTypeConf(Attribute attribute, AttributeType type) {
                return null;
            }
        };
    }
}
