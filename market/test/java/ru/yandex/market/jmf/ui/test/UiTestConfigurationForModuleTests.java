package ru.yandex.market.jmf.ui.test;

import javax.annotation.Nonnull;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.crm.environment.Environment;
import ru.yandex.market.crm.environment.EnvironmentResolver;
import ru.yandex.market.jmf.logic.wf.DefaultWfConfigurationProvider;
import ru.yandex.market.jmf.logic.wf.WfConfigurationProvider;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.module.def.test.ModuleDefaultTestConfiguration;
import ru.yandex.market.jmf.script.storage.ScriptsProvider;
import ru.yandex.market.jmf.script.storage.impl.ClassPathBasedScriptsProvider;
import ru.yandex.market.jmf.search.ModuleSearchTestConfiguration;
import ru.yandex.market.jmf.utils.XmlUtils;

@Configuration
@Import({UiTestConfiguration.class,
        ModuleDefaultTestConfiguration.class,
        UiTestConfiguration.class,
        ModuleSearchTestConfiguration.class
})
public class UiTestConfigurationForModuleTests {
    @Bean
    public MetadataProvider moduleUiBulkEditTestMetaclassesProvider(MetadataProviders providers) {
        return providers.of("classpath:bulk_edit_service_metadata.xml");
    }

    @Bean
    public EnvironmentResolver environmentResolver() {
        return new EnvironmentResolver() {
            @Nonnull
            @Override
            public Environment get() {
                return Environment.INTEGRATION_TEST;
            }
        };
    }

    @Bean
    public MetadataProvider moduleUiTestMetaclassesProvider(MetadataProviders providers) {
        return providers.of("classpath:ui_metadata.xml");
    }

    @Bean
    public MetadataProvider moduleUiSuggestTestMetaclassesProvider(MetadataProviders providers) {
        return providers.of("classpath:suggest_metadata.xml");
    }

    @Bean
    public WfConfigurationProvider wfProvider(XmlUtils xmlUtils) {
        return new DefaultWfConfigurationProvider("classpath:wf_test.xml", xmlUtils);
    }

    @Bean
    public MetadataProvider moduleUiTreeMetadataProvider(MetadataProviders providers) {
        return providers.of("classpath:tree_metadata.xml");
    }

    @Bean
    public ScriptsProvider moduleUiSuggestScriptsProvider(XmlUtils xmlUtils) {
        return new ClassPathBasedScriptsProvider("classpath:scripts.xml", xmlUtils);
    }

}
