package ru.yandex.market.jmf.metadata.test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.jmf.lock.LockServiceTestConfiguration;
import ru.yandex.market.jmf.metadata.AttributeFqn;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataAttributeTypeInitializer;
import ru.yandex.market.jmf.metadata.MetadataProvider;
import ru.yandex.market.jmf.metadata.MetadataProviders;
import ru.yandex.market.jmf.metadata.impl.MetadataInitializer;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;
import ru.yandex.market.jmf.metainfo.ReloadAttributes;
import ru.yandex.market.jmf.metainfo.ReloadType;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = MetadataInitializerTest.Configuration.class)
public class MetadataInitializerTest {

    @Inject
    MetadataInitializer initializer;
    private Map<Fqn, Metaclass> index;

    @Test
    public void defaultEditableAttr() {
        Metaclass metaclass = getMetaclass("testEntity2");

        Attribute attr = metaclass.getAttributeOrError("defaultEditableAttr");
        Assertions.assertTrue(attr.isEditable(), "По умолчанию атрибуты редактируемые");
    }

    @Test
    public void editableAttr() {
        Metaclass metaclass = getMetaclass("testEntity2");

        Attribute attr = metaclass.getAttributeOrError("editableAttr");
        Assertions.assertTrue(attr.isEditable(), "Атрибут описан как редактируемый");
    }

    @Test
    public void logicAttribute() {
        Metaclass metaclass = getMetaclass("testEntity1");

        Assertions.assertNotNull(metaclass, "Должны получить метакласс т.к. он описан в фонфигурационном файле");
        Attribute attr0 = metaclass.getAttribute("attr0");
        Assertions.assertNotNull(attr0, "Должны получить аттрибут описанный в применяемой логике");
        Assertions.assertEquals("attr0", attr0.getCode());
        Assertions.assertEquals(AttributeFqn.parse("testEntity1@attr0"), attr0.getFqn());
        Assertions.assertEquals(metaclass, attr0.getMetaclass());
        Assertions.assertEquals(metaclass, attr0.getMetaclassOfDefinition());
    }

    @Test
    public void metaclassAttributeWithLogic() {
        Metaclass metaclass = getMetaclass("testEntity1");

        Assertions.assertNotNull(metaclass, "Должны получить метакласс т.к. он описан в фонфигурационном файле");
        Attribute attr1 = metaclass.getAttribute("attr1");
        Assertions.assertNotNull(attr1, "Должны получить аттрибут описанный в метаклассе");
        Assertions.assertEquals("attr1", attr1.getCode());
        Assertions.assertEquals(AttributeFqn.parse("testEntity1@attr1"), attr1.getFqn());
        Assertions.assertEquals(metaclass, attr1.getMetaclass());
        Assertions.assertEquals(metaclass, attr1.getMetaclassOfDefinition());
    }

    @Test
    public void metaclassTyped_default() {
        Metaclass metaclass = getMetaclass("testEntity1");
        boolean result = metaclass.isTyped();
        Assertions.assertFalse(result, "По умолчанию метаклассы не типизированы");
    }

    @Test
    public void metaclassTyped_false() {
        Metaclass metaclass = getMetaclass("testEntity2");
        boolean result = metaclass.isTyped();
        Assertions.assertFalse(result, "в конфигурации метакласса явно указано, что он не типизирован");
    }

    @Test
    public void metaclassTyped_true() {
        Metaclass metaclass = getMetaclass("typedEntity");
        boolean result = metaclass.isTyped();
        Assertions.assertTrue(result, "в конфигурации метакласса явно указано, что он не типизирован");
    }

    @Test
    public void readonlyAttr() {
        Metaclass metaclass = getMetaclass("testEntity2");

        Attribute attr = metaclass.getAttributeOrError("readonlyAttr");
        Assertions.assertFalse(attr.isEditable(), "Атрибут описан как атрибут только для чтения");
    }

    @Test
    public void descendantsWithSelfFind() {
        Metaclass metaclass = getMetaclass("typedEntity");

        List<String> expectedDescendants = Arrays.asList("typedEntity$childOne");
        Assertions.assertEquals(metaclass.getDescendants().size(), 1);
        for (String expectedFqn : expectedDescendants) {
            boolean find = metaclass.getDescendants().stream()
                    .anyMatch(m -> m.getFqn().toString().equals(expectedFqn));
            Assertions.assertTrue(find);
        }

        List<String> expectedDescendantsWithSelf = Arrays.asList("typedEntity$childOne", "typedEntity");
        Assertions.assertEquals(metaclass.getDescendantsWithSelf().size(), 2);
        for (String expectedFqn : expectedDescendantsWithSelf) {
            boolean find = metaclass.getDescendantsWithSelf().stream()
                    .anyMatch(m -> m.getFqn().toString().equals(expectedFqn));
            Assertions.assertTrue(find);
        }
    }

    @Test
    public void descendantsWithSelfNotFind() {
        Metaclass metaclass = getMetaclass("typedEntityAlone");

        Assertions.assertEquals(metaclass.getDescendants().size(), 0);

        Assertions.assertEquals(metaclass.getDescendantsWithSelf().size(), 1);
        boolean find = metaclass.getDescendantsWithSelf().stream()
                .anyMatch(m -> m.getFqn().toString().equals("typedEntityAlone"));
        Assertions.assertTrue(find);
    }

    @Test
    public void ancestorsWithSelfFind() {
        Metaclass metaclass = getMetaclass("typedEntity$childOne");

        List<String> expectedAncestors = Arrays.asList("typedEntity");
        Assertions.assertEquals(metaclass.getAncestors().size(), 1);
        for (String expectedFqn : expectedAncestors) {
            boolean find = metaclass.getAncestors().stream()
                    .anyMatch(m -> m.getFqn().toString().equals(expectedFqn));
            Assertions.assertTrue(find);
        }

        List<String> expectedAncestorsWithSelf = Arrays.asList("typedEntity$childOne", "typedEntity");
        Assertions.assertEquals(metaclass.getAncestorsWithSelf().size(), 2);
        for (String expectedFqn : expectedAncestorsWithSelf) {
            boolean find = metaclass.getAncestorsWithSelf().stream()
                    .anyMatch(m -> m.getFqn().toString().equals(expectedFqn));
            Assertions.assertTrue(find);
        }
    }

    @Test
    public void ancestorsWithSelfNotFind() {
        Metaclass metaclass = getMetaclass("typedEntityAlone");

        Assertions.assertEquals(metaclass.getAncestors().size(), 0);

        Assertions.assertEquals(metaclass.getAncestorsWithSelf().size(), 1);
        boolean find = metaclass.getAncestorsWithSelf().stream()
                .anyMatch(m -> m.getFqn().toString().equals("typedEntityAlone"));
        Assertions.assertTrue(find);
    }

    @BeforeEach
    public void setUp() {
        index = initializer.build(null, ReloadType.INSTANCE_MODIFICATION, ReloadAttributes.EMPTY).getIndex();
    }

    private Metaclass getMetaclass(String code) {
        return index.get(Fqn.parse(code));
    }

    @Import({
            MetadataTestConfiguration.class,
            LockServiceTestConfiguration.class,
            TestAttributeStoreConfiguration.class
    })
    static class Configuration {
        @Bean
        public MetadataProvider provider(MetadataProviders providers) {
            return providers.of("classpath:simple_metadata.xml");
        }

        @Bean
        public MetadataAttributeTypeInitializer string() {
            return MetadataTestHelper.strategy("string");
        }

        @Bean
        @Primary
        public SessionFactory hibernateProperties() {
            return Mockito.mock(SessionFactory.class);
        }
    }

}
