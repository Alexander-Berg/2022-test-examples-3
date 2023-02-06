package ru.yandex.market.jmf.attributes;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metadata.conf.metaclass.Config;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

@Import({
        AttributesTestConfiguration.class,
})
public abstract class AbstractAttributeTestConfiguration extends AbstractModuleConfiguration {

    private final String[] metadataResources;

    protected AbstractAttributeTestConfiguration(String... metadataResources) {
        super("attributes/test");
        this.metadataResources = metadataResources;
    }

    @Bean
    public MetadataProvider attributesTestMetadataProvider(MetadataProviders providers) {
        return new MetadataProviderWrapper(metadataResources);
    }

    private static class MetadataProviderWrapper implements MetadataProvider {
        private final String[] paths;
        @Inject
        private MetadataProviders providers;

        public MetadataProviderWrapper(String[] paths) {
            this.paths = paths;
        }

        @Override
        public Collection<Config> getAll() {
            return Stream.of(paths)
                    .flatMap(path -> providers.of(path).getAll().stream())
                    .collect(Collectors.toList());
        }
    }
}
