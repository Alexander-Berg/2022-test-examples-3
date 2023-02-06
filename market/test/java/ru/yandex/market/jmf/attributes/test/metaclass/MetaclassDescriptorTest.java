package ru.yandex.market.jmf.attributes.test.metaclass;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.AttributesTestConfiguration;
import ru.yandex.market.jmf.attributes.metaclass.MetaclassDescriptor;
import ru.yandex.market.jmf.attributes.metaclass.MetaclassType;
import ru.yandex.market.jmf.entity.HasFqn;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(classes = MetaclassDescriptorTest.Configuration.class)
public class MetaclassDescriptorTest {

    private static final String TEST_FQN = "e1";
    private static final String ATTR = "attr";
    private static final String ATTR_WITH_FQN_CLASS = "attrWithFqnClass";
    private static final String ATTR_WITH_FQN_TYPE = "attrWithFqnType";
    private static final String ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY = "attrWithFqnClassAndTypeOnly";

    private static final Fqn TEST_CLASS_FQN = Fqn.of("testMetaclassClass");
    private static final Fqn TEST_TYPE_FQN_1 = Fqn.of("testMetaclassClass$type1");
    private static final Fqn TEST_TYPE_FQN_2 = Fqn.of("testMetaclassClass$type2");

    @Inject
    MetaclassDescriptor descriptor;

    @Inject
    MetadataService metadataService;

    private Metaclass metaclass;

    @BeforeAll
    public void setUp() {
        metaclass = metadataService.getMetaclassOrError(Fqn.of(TEST_FQN));
    }

    @Test
    public void toDtoValue_null() {
        Attribute attribute = metaclass.getAttribute(ATTR);
        Object result = descriptor.toDtoValue(attribute, attribute.getType(), null);
        Assertions.assertNull(result);
    }

    @Test
    public void toDtoValue() {
        Attribute attribute = metaclass.getAttribute(ATTR);
        Object result = descriptor.toDtoValue(attribute, attribute.getType(), Fqn.of("value"));
        Assertions.assertEquals(Fqn.of("value"), result);
    }

    @Test
    public void toString_null() {
        Attribute attribute = metaclass.getAttribute(ATTR);
        Object result = descriptor.toString(attribute, attribute.getType(), null);
        Assertions.assertNull(result);
    }

    @Test
    public void toString_value() {
        Attribute attribute = metaclass.getAttribute(ATTR);
        Object result = descriptor.toString(attribute, attribute.getType(), Fqn.of("value"));
        Assertions.assertEquals("value", result);
    }

    @MethodSource("wrapData")
    @ParameterizedTest(name = "{0}")
    public void wrap(@SuppressWarnings("unused") String testName, String attributeCode, Object rawValue, Fqn expected) {
        Attribute attribute = metaclass.getAttribute(attributeCode);
        MetaclassType attributeType = attribute.getType();

        Object result = descriptor.wrap(attribute, attributeType, rawValue);
        Assertions.assertEquals(expected, result);
    }

    @MethodSource("unwrapData")
    @ParameterizedTest(name = "{0}")
    public void unwrap(@SuppressWarnings("unused") String testName, Fqn value, Class<?> toType, Object expected) {
        Attribute attribute = metaclass.getAttribute(ATTR);
        Object result = descriptor.unwrap(attribute, attribute.getType(), value, toType);
        Assertions.assertEquals(expected, result);
    }

    private static Stream<Arguments> wrapData() {
        return Stream.of(
                arguments("nullValue", ATTR, null, null),
                arguments("fqnValue", ATTR, Fqn.of(TEST_FQN), Fqn.of(TEST_FQN)),
                arguments("stringValue", ATTR, TEST_FQN, Fqn.of(TEST_FQN)),
                arguments("hasFqnValue", ATTR, new HasFqnImpl(Fqn.of(TEST_FQN)), Fqn.of(TEST_FQN)),
                arguments("attrFqnAsClassAndValueClass", ATTR_WITH_FQN_CLASS, TEST_CLASS_FQN, TEST_CLASS_FQN),
                arguments("attrFqnAsClassAndValueType", ATTR_WITH_FQN_CLASS, TEST_TYPE_FQN_1, TEST_TYPE_FQN_1),
                arguments("attrFqnAsTypeAndTypeValue", ATTR_WITH_FQN_TYPE, TEST_TYPE_FQN_1, TEST_TYPE_FQN_1),
                arguments("typeOnlyAndTypeValue", ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY, TEST_TYPE_FQN_1, TEST_TYPE_FQN_1)
        );
    }

    private static Stream<Arguments> unwrapData() {
        return Stream.of(
                arguments("nullValue", null, Object.class, null),
                arguments("fqnValue", Fqn.of("value"), Fqn.class, Fqn.of("value")),
                arguments("stringValue", Fqn.of("value"), String.class, "value"),
                arguments("jsonNodeValue", Fqn.of("value"), JsonNode.class, new TextNode("value"))
        );
    }

    private static class HasFqnImpl implements HasFqn {

        private final Fqn fqn;

        public HasFqnImpl(Fqn fqn) {
            this.fqn = fqn;
        }

        @Nonnull
        @Override
        public Fqn getFqn() {
            return fqn;
        }
    }

    @org.springframework.context.annotation.Configuration
    @Import({AttributesTestConfiguration.class})
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:metaclass_column_attribute_metadata.xml");
        }
    }
}
