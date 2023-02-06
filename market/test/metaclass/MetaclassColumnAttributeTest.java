package ru.yandex.market.jmf.attributes.test.metaclass;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.metaclass.MetaclassEqOrAncestorOf;
import ru.yandex.market.jmf.attributes.metaclass.MetaclassEqOrDescendantOf;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;

@ContextConfiguration(classes = MetaclassColumnAttributeTest.Configuration.class)
public class MetaclassColumnAttributeTest extends AbstractAttributeTest {

    private static final Fqn TEST_CLASS_FQN = Fqn.of("testMetaclassClass");
    private static final Fqn TEST_TYPE_FQN_1 = Fqn.of("testMetaclassClass$type1");

    private static final List<String> VALUES = List.of(
            "testMetaclassTypeValue1",
            "testMetaclassTypeValue2",
            "testMetaclassTypeValue3",
            "testMetaclassTypeValue4",
            "testMetaclassTypeValue5",
            "testMetaclassTypeValue6"
    );

    private int valueIndex;

    @BeforeEach
    public void initValueIndex() {
        valueIndex = 0;
    }

    @Override
    protected Object randomAttributeValue() {
        String value = VALUES.get(valueIndex++);
        if (valueIndex >= VALUES.size()) {
            initValueIndex();
        }
        return Fqn.of(value);
    }

    @Override
    protected Object getNullValue() {
        return null;
    }

    @Test
    public void metaclassEqOrDescendantOfFilterOnLinkedObject() {
        Entity entity3 = createPersistedLinkedEntity(TEST_TYPE_FQN_1);

        Filter filter = new MetaclassEqOrDescendantOf(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                TEST_CLASS_FQN
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    @Test
    public void metaclassEqOrAncestorOfFilterOnLinkedObject() {
        Entity entity3 = createPersistedLinkedEntity(TEST_CLASS_FQN);

        Filter filter = new MetaclassEqOrAncestorOf(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                TEST_TYPE_FQN_1
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:metaclass_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
