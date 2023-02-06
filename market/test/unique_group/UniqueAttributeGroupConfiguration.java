package ru.yandex.market.jmf.metadata.test.unique_group;

import org.springframework.context.annotation.Bean;

import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.test.MetadataTestHelper;
import ru.yandex.market.jmf.metadata.test.SimpleConfiguration;

public class UniqueAttributeGroupConfiguration extends SimpleConfiguration {

    protected UniqueAttributeGroupConfiguration(String metadataResource) {
        super(metadataResource);
    }

    @Bean
    public MetadataAttributeTypeInitializer testUniqueType() {
        return MetadataTestHelper.strategy("testUnique", true);
    }

    @Bean
    public MetadataAttributeTypeInitializer testNotUniqueType() {
        return MetadataTestHelper.strategy("testNotUnique", false);
    }
}
