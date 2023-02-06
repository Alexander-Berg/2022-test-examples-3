package ru.yandex.market.jmf.metadata.test;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.metadata.AttributeFqn;
import ru.yandex.market.jmf.metadata.ConfigurationError;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metadata.impl.AttributeDependenciesInitializer;
import ru.yandex.market.jmf.metadata.impl.AttributeDependenciesInitializer.AttributeDependenciesIndex;
import ru.yandex.market.jmf.metadata.impl.MetaclassData;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metainfo.MetaInfoService;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.script.Script;
import ru.yandex.market.jmf.script.model.ScriptDependency;
import ru.yandex.market.jmf.script.model.ScriptDependencyType;
import ru.yandex.market.jmf.script.storage.ScriptsIndex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.jmf.metainfo.ReloadType.INSTANCE_MODIFICATION;
import static ru.yandex.market.jmf.script.model.ScriptDependencyType.HEURISTIC;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringJUnitConfig(DependentAttributesTest.Configuration.class)
public class DependentAttributesTest {

    @Inject
    private MetadataInitializer metadataInitializer;
    @Inject
    private AttributeDependenciesInitializer dependenciesInitializer;

    private MetaclassData metaclassData;

    private AttributeDependenciesIndex index;

    @BeforeEach
    public void setUp() {
        metaclassData = metadataInitializer.build(null, INSTANCE_MODIFICATION, ReloadAttributes.EMPTY);
    }

    @Test
    public void logicDependentAttr() {
        Set<String> dependencies = indexWithScriptDeps(
                "depends_on_logic_attribute",
                "dep0"
        ).getDependencies(AttributeFqn.of("testEntity@attr0"));
        assertThat(dependencies).containsOnly("dep0");
    }

    @Test
    public void entityDependentAttr() {
        Set<String> dependencies = indexWithScriptDeps(
                "depends_on_current_entity_attribute",
                "dep1"
        ).getDependencies(AttributeFqn.of("testEntity@attr1"));
        assertThat(dependencies).containsOnly("dep1");
    }

    @Test
    public void entityChildDependentAttrs() {
        Set<String> dependencies = indexWithScriptDeps(
                "depends_on_parent_and_current_entities_attributes",
                "dep1", "dep2"
        ).getDependencies(AttributeFqn.of("testEntityChild@attr2"));
        assertThat(dependencies).containsOnly("dep1", "dep2");
    }

    @Test
    public void entityChildDependentAttrsOverride() {
        Set<String> dependencies = indexWithScriptDeps(
                "depends_on_current_overridden_entity_attribute",
                "dep3"
        ).getDependencies(AttributeFqn.of("testEntityChild@attr3"));
        assertThat(dependencies).containsOnly("dep3");
    }

    @Test
    public void shouldThrowErrorIfDependencyAttrNotExists() {
        assertThatExceptionOfType(ConfigurationError.class).isThrownBy(() -> indexWithScriptDeps(
                        "depends_on_not_existent_attribute",
                        "notExistent"
                )
        ).withMessage("Failed to construct attribute dependencies of metaclass testEntityWithNotExistentDependency");
    }

    @Test
    public void shouldThrowErrorOnCircularDependency() {
        assertThatExceptionOfType(ConfigurationError.class).isThrownBy(() -> indexWithScriptDeps(
                        "depends_on_self",
                        "attr"
                )
        ).withMessage("Failed to construct attribute dependencies of metaclass testEntityWithCircularDependency");
    }

    @Test
    public void shouldNotThrowErrorIfDependencyAttrFromObjectTemplateNotExists() {
        Set<String> dependencies = indexWithScriptDeps(
                "depends_on_not_existent_attribute",
                HEURISTIC,
                "notExistent"
        ).getDependencies(AttributeFqn.of("testEntityWithNotExistentDependency@attr"));
        assertThat(dependencies).isEmpty();
    }

    private AttributeDependenciesIndex indexWithScriptDeps(String scriptCode, String... dependencyCodes) {
        return indexWithScriptDeps(scriptCode, ScriptDependencyType.EXPLICIT, dependencyCodes);
    }

    private AttributeDependenciesIndex indexWithScriptDeps(String scriptCode, ScriptDependencyType dependencyType, String... dependencyCodes) {
        ScriptsIndex scriptsIndex = mock(ScriptsIndex.class);
        when(scriptsIndex.get(anyString())).thenReturn(Optional.of(mock(Script.class)));
        Script script = mock(Script.class);
        Set<ScriptDependency> dependencies = Stream.of(dependencyCodes)
                .map(code -> new ScriptDependency(code, dependencyType))
                .collect(Collectors.toSet());
        doReturn(dependencies).when(script).getDependencies();
        when(scriptsIndex.get(eq(scriptCode))).thenReturn(Optional.of(script));
        MetaInfoService metaInfoService = mock(MetaInfoService.class);
        when(metaInfoService.get(MetaclassData.class)).thenReturn(metaclassData);
        when(metaInfoService.get(ScriptsIndex.class)).thenReturn(scriptsIndex);
        return dependenciesInitializer.build(metaInfoService, INSTANCE_MODIFICATION, ReloadAttributes.EMPTY);
    }

    @Import({
            MetadataTestConfiguration.class,
            LockServiceTestConfiguration.class,
            TestAttributeStoreConfiguration.class
    })
    static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:dependent_attr_metadata.xml");
        }

        @Bean
        public MetadataAttributeTypeInitializer string() {
            return MetadataTestHelper.strategy("string");
        }

        @Bean
        @Primary
        public SessionFactory hibernateProperties() {
            return mock(SessionFactory.class);
        }
    }

}
