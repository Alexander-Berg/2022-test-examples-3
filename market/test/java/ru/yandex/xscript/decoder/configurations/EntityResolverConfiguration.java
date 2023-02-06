package ru.yandex.xscript.decoder.configurations;

import org.junit.experimental.theories.DataPoints;
import org.xml.sax.EntityResolver;
import ru.yandex.xscript.decoder.XscriptContext;
import ru.yandex.xscript.decoder.resolver.entity.CPEntityResolver;
import ru.yandex.xscript.decoder.resolver.entity.FilePathEntityResolver;

public enum EntityResolverConfiguration {
    CLASS_PATH_ENTITY_RESOLVER,
    FILE_ENTITY_RESOLVER;

    @DataPoints
    public static EntityResolverConfiguration[] configValues = EntityResolverConfiguration.values();

    public static EntityResolver getResolver(EntityResolverConfiguration config) {
        switch (config) {
            case FILE_ENTITY_RESOLVER:
                return new FilePathEntityResolver(System.getProperty("src_test_resources_path") + "/");
            case CLASS_PATH_ENTITY_RESOLVER:
                return new CPEntityResolver(XscriptContext.class.getClassLoader());
            default:
                throw new IllegalArgumentException("Unknown config");
        }
    }
}
