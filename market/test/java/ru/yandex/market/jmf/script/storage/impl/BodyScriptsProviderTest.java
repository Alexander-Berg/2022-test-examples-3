package ru.yandex.market.jmf.script.storage.impl;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.metainfo.LStringUtils;
import ru.yandex.market.jmf.metainfo.MetaInfoSource;
import ru.yandex.market.jmf.script.ScriptType;
import ru.yandex.market.jmf.script.storage.ScriptsProvider;
import ru.yandex.market.jmf.script.storage.ScriptsProviders;
import ru.yandex.market.jmf.script.storage.conf.ScriptConf;
import ru.yandex.market.jmf.script.storage.conf.Scripts;
import ru.yandex.market.jmf.script.test.ScriptSupportTestConfiguration;

@SpringJUnitConfig(classes = BodyScriptsProviderTest.Configuration.class)
public class BodyScriptsProviderTest {

    @Inject
    ResourcePatternResolver resolver;

    @Test
    public void checkMeta() {
        ScriptsProvider provider = new BodyScriptsProvider("classpath:/script/storage/test/groovyWithMeta.groovy",
                resolver);

        Scripts result = provider.get();

        Assertions.assertEquals(1, result.getScript().size(), "По указанному пити находится только один скрипт");
        ScriptConf script = result.getScript().get(0);
        Assertions.assertEquals("groovyWithMeta", script.getCode());
        Assertions.assertEquals("Тестирования распаршивания метаинформации о скрипте из его тела",
                LStringUtils.get(script.getTitle(), "ru"));
        Assertions.assertEquals(ScriptType.DEFAULT, script.getType());
        Assertions.assertEquals(Dates.parseDateTime("2011-12-03T10:15:30+01:00"), script.getVersion());
    }

    @Test
    public void checkMetaWithoutType() {
        ScriptsProvider provider = new BodyScriptsProvider("classpath:/script/storage/test/groovyWithMetaWithoutType" +
                ".groovy",
                resolver);

        Scripts result = provider.get();

        Assertions.assertEquals(1, result.getScript().size(), "По указанному пити находится только один скрипт");
        ScriptConf script = result.getScript().get(0);
        Assertions.assertEquals("groovyWithMetaWithoutType", script.getCode());
        Assertions.assertEquals(ScriptType.DEFAULT, script.getType(), "Если тип не указан явно, то используем DEFAULT");
        Assertions.assertEquals(Dates.parseDateTime("2011-12-03T10:15:30+01:00"), script.getVersion());
    }

    @Test
    public void checkMetaWithoutVersion() {
        ScriptsProvider provider = new BodyScriptsProvider("classpath:/script/storage/test" +
                "/groovyWithMetaWithoutVersion.groovy",
                resolver);

        Scripts result = provider.get();

        Assertions.assertEquals(1, result.getScript().size(), "По указанному пити находится только один скрипт");
        ScriptConf script = result.getScript().get(0);
        Assertions.assertEquals("groovyWithMetaWithoutVersion", script.getCode());
        Assertions.assertNull(script.getVersion());
    }

    @Import({
            ScriptSupportTestConfiguration.class,
    })
    public static class Configuration {

        @Bean
        public ScriptsProviders scriptsProviders() {
            return new ScriptsProviders() {
                @Override
                public ScriptsProvider ofXml(String resourceName) {
                    return new ScriptsProvider() {
                        @Override
                        public MetaInfoSource source() {
                            return MetaInfoSource.SYSTEM;
                        }

                        @Override
                        public Scripts get() {
                            return new Scripts();
                        }
                    };
                }

                @Override
                public ScriptsProvider of(String pattern) {
                    return ofXml(pattern);
                }
            };
        }
    }
}
