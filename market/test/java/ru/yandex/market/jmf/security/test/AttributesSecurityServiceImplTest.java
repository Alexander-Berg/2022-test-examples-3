package ru.yandex.market.jmf.security.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeStoreConf;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStore;
import ru.yandex.market.jmf.metadata.metaclass.AttributeStoreInitializer;
import ru.yandex.market.jmf.metadata.metaclass.AttributeType;
import ru.yandex.market.jmf.security.AttributesSecurityService;
import ru.yandex.market.jmf.security.MetaclassPermissionContext;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockSecurityDataService;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@SpringJUnitConfig(classes = AttributesSecurityServiceImplTest.Configuration.class)
@TestPropertySource({"classpath:/do_not_require_getters_for_all_attributes.properties"})
public class AttributesSecurityServiceImplTest {
    @Inject
    private AttributesSecurityService securityService;
    @Inject
    private MetadataService metadataService;
    @Inject
    private MockSecurityDataService mockSecurityDataService;
    @Inject
    private MockAuthRunnerService mockAuthRunnerService;

    @BeforeEach
    public void setUp() {
        mockSecurityDataService.setCurrentUserProfiles("admin");
        mockAuthRunnerService.setCurrentUserSuperUser(false);
    }

    @Test
    public void testAllowedIfAllowedByDefaultMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attr2"));
    }

    @Test
    public void testDisallowedIfDisallowedByDefaultMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test")));
        Assertions.assertFalse(securityService.hasEditPermission(context, "attr2"));
    }

    @Test
    public void testAllowedIfAllowedByContainingMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attr1"));
    }

    @Test
    public void testAllowedInChildIfAllowedByContainingMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$a")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attr1"));
    }

    @Test
    public void testChildAttributeAllowedIfAllowedByContainingMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$a")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attr1"));
    }

    @Test
    public void testAllowedInChildIfAllowedByDefaultMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$b")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attrB1"));
    }

    @Test
    public void testDisallowedInChildIfDisallowedByDefaultMarker() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$b")));
        Assertions.assertFalse(securityService.hasEditPermission(context, "attrB1"));
    }

    @Test
    public void testDisallowedIfDisallowedInChild() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$aa")));
        Assertions.assertFalse(securityService.hasEditPermission(context, "attrA1"));
    }

    @Test
    public void testAllowedInChildIfAllowedByDefaultMarker2() {
        final var context = new MetaclassPermissionContext(metadataService.getMetaclassOrError(Fqn.of("test$aa")));
        Assertions.assertTrue(securityService.hasViewPermission(context, "attrAA1"));
    }

    @org.springframework.context.annotation.Configuration
    @Import({
            SecurityTestConfiguration.class
    })
    public static class Configuration extends AbstractModuleConfiguration {
        protected Configuration() {
            super("security/test");
        }

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
}
