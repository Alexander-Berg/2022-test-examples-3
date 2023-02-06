package ru.yandex.market.jmf.metadata.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.AttributeType;

public class MetadataTestHelper {

    public static MetadataAttributeTypeInitializer object(Fqn fqn) {
        return new MetadataAttributeTypeInitializer() {
            @Override
            public String getCode() {
                return "object";
            }

            @Override
            public Class getType() {
                return AttributeTypeConf.class;
            }

            @Override
            public boolean isApplicable(String typeCode, AttributeTypeConf typeConf) {
                return true;
            }

            @Override
            public Collection<Fqn> getRelated(AttributeType type, Map index) {
                return Collections.singletonList(fqn);
            }

            @Override
            public AttributeType initialize(Attribute attribute, AttributeTypeConf conf) {
                return new AttributeType() {
                    @Override
                    public String getCode() {
                        return "object";
                    }

                    @Override
                    public ImmutableMap<String, Object> properties() {
                        return ImmutableMap.of();
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
            public AttributeTypeConf getVersionTypeConf(Attribute attribute, AttributeType type) {
                return null;
            }
        };
    }

    public static MetadataAttributeTypeInitializer strategy(String code) {
        return strategy(code, false);
    }

    public static MetadataAttributeTypeInitializer strategy(String code, boolean mayUnique) {
        return new MetadataAttributeTypeInitializer() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public Class getType() {
                return AttributeTypeConf.class;
            }

            @Override
            public boolean isApplicable(String typeCode, AttributeTypeConf typeConf) {
                if (null != typeCode && typeCode.equals(getCode())) {
                    return true;
                }
                return null != typeConf && !AttributeTypeConf.class.equals(getType()) && typeConf.equals(getType());
            }

            @Override
            public Collection<Fqn> getRelated(AttributeType type, Map index) {
                return Collections.emptyList();
            }

            @Override
            public AttributeType initialize(Attribute attribute, AttributeTypeConf conf) {
                return new AttributeType() {
                    @Override
                    public String getCode() {
                        return code;
                    }

                    @Override
                    public ImmutableMap<String, Object> properties() {
                        return ImmutableMap.of();
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
                        return mayUnique;
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
            public AttributeTypeConf getVersionTypeConf(Attribute attribute, AttributeType type) {
                return null;
            }
        };
    }
}
