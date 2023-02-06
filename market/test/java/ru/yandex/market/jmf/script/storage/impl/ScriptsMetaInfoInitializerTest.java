package ru.yandex.market.jmf.script.storage.impl;

import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.util.Dates;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.ScriptType;
import ru.yandex.market.jmf.script.model.ScriptDependency;
import ru.yandex.market.jmf.script.storage.ScriptConfsProvider;
import ru.yandex.market.jmf.script.storage.ScriptsIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.jmf.metainfo.ReloadType.INSTANCE_MODIFICATION;
import static ru.yandex.market.jmf.script.model.ScriptDependencyType.EXPLICIT;
import static ru.yandex.market.jmf.script.model.ScriptDependencyType.HEURISTIC;

@SpringJUnitConfig(classes = ScriptsMetaInfoInitializerTest.Configuration.class)
class ScriptsMetaInfoInitializerTest {

    @Inject
    private ScriptsMetaInfoInitializer initializer;

    private ScriptsIndex index;

    @BeforeEach
    void setUp() {
        index = initializer.build(null, INSTANCE_MODIFICATION, ReloadAttributes.EMPTY);
    }

    @Test
    void testScriptDependenciesInitialization() {
        Script script = index.getOrError("groovyWithDependencies");
        assertThat(script.getType()).isEqualTo(ScriptType.DEFAULT);
        assertThat(script.getVersion()).isEqualTo(Dates.parseDateTime("2011-12-03T10:15:30+01:00"));
        assertThat(script.getDependencies())
                .filteredOn(dependency -> dependency.type() == EXPLICIT).hasSize(2)
                .extracting(ScriptDependency::code).containsExactlyInAnyOrder("metaDependency", "commonDependency");
        assertThat(script.getDependencies())
                .filteredOn(dependency -> dependency.type() == HEURISTIC).hasSize(7)
                .extracting(ScriptDependency::code).containsExactlyInAnyOrder(
                        "commonDependency",
                        "optionalDependency",
                        "requiredDependency",
                        "lineBreakDependency1",
                        "lineBreakDependency2",
                        "lineBreakDependency3",
                        "lineBreakDependency4"
                );
    }

    public static class Configuration {

        @Bean
        public ScriptConfsProvider scriptConfsProvider(ResourcePatternResolver resolver) {
            return new ScriptConfsProviderImpl(List.of(new BodyScriptsProvider(
                    "classpath:/script/storage/test/groovyWithDependencies.groovy",
                    resolver
            )));
        }

        @Bean
        public ScriptsMetaInfoInitializer initializer(ScriptConfsProvider confsProvider) {
            return new ScriptsMetaInfoInitializer(confsProvider);
        }
    }
}
