package ru.yandex.market.jmf.metadata.test;

import java.util.Collection;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.ResourcePatternResolver;

import ru.yandex.market.jmf.metadata.AttributeExtensionInitializer;
import ru.yandex.market.jmf.metadata.AttributeStoreInitializationService;
import ru.yandex.market.jmf.metadata.DefaultMetadataProvider;
import ru.yandex.market.jmf.metadata.MetaclassExtensionInitializer;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.conf.metaclass.Config;
import ru.yandex.market.jmf.metadata.impl.MetadataAttributeTypeInitializerFactory;
import ru.yandex.market.jmf.metadata.impl.MetadataExtensionInitializerFactory;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.MetaInfoStorageService;
import ru.yandex.market.jmf.metainfo.MockMetaInfoStorageServiceConfiguration;
import ru.yandex.market.jmf.utils.UtilsConfiguration;
import ru.yandex.market.jmf.utils.XmlUtils;
import ru.yandex.market.jmf.utils.html.SafeUrlService;

@Import({
        UtilsConfiguration.class,
        MockMetaInfoStorageServiceConfiguration.class,
        TestAttributeStoreConfiguration.class
})
public abstract class SimpleConfiguration {

    private final String metadataResource;

    protected SimpleConfiguration(String metadataResource) {
        this.metadataResource = metadataResource;
    }

    @Bean
    @Primary
    public MetadataInitializer metadataInitializer(
            Collection<MetadataProvider> providers,
            MetadataAttributeTypeInitializerFactory attributeTypeFactory,
            MetadataExtensionInitializerFactory extensionInitializerFactory,
            ConfigurableBeanFactory beanFactory,
            MetaInfoStorageService metaInfoStorageService,
            AttributeStoreInitializationService attributeStoreInitializationService) {
        return new MetadataInitializer(providers, attributeTypeFactory, beanFactory, List.of(x -> new Config()),
                extensionInitializerFactory, metaInfoStorageService, attributeStoreInitializationService,
                List.of());
    }

    @Bean
    public SafeUrlService mockSafeUrlService() {
        return Mockito.mock(SafeUrlService.class);
    }

    @Bean
    public MetadataProvider provider(ResourcePatternResolver resolver, XmlUtils xmlUtils) {
        return new DefaultMetadataProvider(metadataResource, resolver, xmlUtils);
    }

    @Bean
    public MetadataAttributeTypeInitializerFactory metadataAttributeTypeInitializerFactory(Collection<MetadataAttributeTypeInitializer> strategies) {
        return new MetadataAttributeTypeInitializerFactory(strategies);
    }

    @Bean
    public MetadataExtensionInitializerFactory metadataExtensionInitializerFactory(
            Collection<MetaclassExtensionInitializer> strategies,
            Collection<AttributeExtensionInitializer> attributeStrategires
    ) {
        return new MetadataExtensionInitializerFactory(strategies, attributeStrategires);
    }

    @Bean
    public MetadataAttributeTypeInitializer string() {
        return MetadataTestHelper.strategy("test");
    }
}
