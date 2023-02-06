package ru.yandex.market.jmf.logic.def.test;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.AttributeValueException;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.metadata.Fqn;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@SpringJUnitConfig(InternalLogicDefaultTestConfiguration.class)
public class MetaclassAttributeTest {
    private static final String TEST_FQN = "e1";
    private static final String ATTR = "attr";
    private static final String ATTR_WITH_FQN_CLASS = "attrWithFqnClass";
    private static final String ATTR_WITH_FQN_TYPE = "attrWithFqnType";
    private static final String ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY = "attrWithFqnClassAndTypeOnly";
    private static final String ATTR_WITH_TYPE_ONLY = "attrWithTypeOnly";

    private static final Fqn TEST_CLASS_FQN = Fqn.of("testMetaclassClass");
    private static final Fqn TEST_TYPE_FQN_1 = Fqn.of("testMetaclassClass$type1");
    private static final Fqn TEST_TYPE_FQN_2 = Fqn.of("testMetaclassClass$type2");
    private static final Fqn TEST_TYPE2_FQN_1 = Fqn.of("testMetaclassClass2$type1");

    private final BcpService bcpService;

    public MetaclassAttributeTest(BcpService bcpService) {
        this.bcpService = bcpService;
    }

    private static Stream<Arguments> correctValueData() {
        return Stream.of(
                arguments("Атрибут без ограничений [class]", ATTR, TEST_CLASS_FQN),
                arguments("Атрибут без ограничений [type]", ATTR, TEST_TYPE_FQN_1),
                arguments("Только testMetaclassClass [class]", ATTR_WITH_FQN_CLASS, TEST_CLASS_FQN),
                arguments("Только testMetaclassClass [type]", ATTR_WITH_FQN_CLASS, TEST_TYPE_FQN_2),
                arguments("Только testMetaclassClass$type1", ATTR_WITH_FQN_TYPE, TEST_TYPE_FQN_1),
                arguments("Только testMetaclassClass и typeOnly [1]", ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY,
                        TEST_TYPE_FQN_1),
                arguments("Только testMetaclassClass и typeOnly [2]", ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY,
                        TEST_TYPE_FQN_2),
                arguments("Только typeOnly [type]", ATTR_WITH_TYPE_ONLY, TEST_TYPE_FQN_1),
                arguments("Только typeOnly [other hierarchy type]", ATTR_WITH_TYPE_ONLY, TEST_TYPE2_FQN_1)
        );
    }

    private static Stream<Arguments> wrapErrorData() {
        return Stream.of(
                arguments("Только testMetaclassClass [other class]", ATTR_WITH_FQN_CLASS, TEST_FQN),
                arguments("Только testMetaclassClass [other type]", ATTR_WITH_FQN_CLASS, TEST_TYPE2_FQN_1),
                arguments("Только testMetaclassClass$type1 [class]", ATTR_WITH_FQN_TYPE, TEST_CLASS_FQN),
                arguments("Только testMetaclassClass$type1 [incompatible type of same class]", ATTR_WITH_FQN_TYPE,
                        TEST_TYPE_FQN_2),
                arguments("Только testMetaclassClass$type1 [incompatible type of other class]", ATTR_WITH_FQN_TYPE,
                        TEST_TYPE2_FQN_1),
                arguments("Только testMetaclassClass и typeOnly [same class]", ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY,
                        TEST_CLASS_FQN),
                arguments("Только testMetaclassClass и typeOnly [other class]", ATTR_WITH_FQN_CLASS_AND_TYPE_ONLY,
                        TEST_FQN)
        );
    }

    @MethodSource("correctValueData")
    @ParameterizedTest(name = "{0}")
    public void testCorrectValue(String ignored, String attributeCode, Fqn value) {
        Entity entity = bcpService.create(Fqn.of(TEST_FQN), Map.of(
                attributeCode, value
        ));

        Assertions.assertEquals(value, entity.getAttribute(attributeCode));
    }

    @MethodSource("wrapErrorData")
    @ParameterizedTest(name = "{0}")
    public void testIncorrectValue(@SuppressWarnings("unused") String testName, String attributeCode, Object value) {
        Assertions.assertThrows(AttributeValueException.class,
                () -> bcpService.create(Fqn.of(TEST_FQN), Map.of(
                        attributeCode, value
                )));
    }
}
