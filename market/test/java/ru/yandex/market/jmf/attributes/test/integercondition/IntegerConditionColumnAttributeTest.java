package ru.yandex.market.jmf.attributes.test.integercondition;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.integercondition.IntegerCondition;

@ContextConfiguration(classes = IntegerConditionColumnAttributeTest.Configuration.class)
public class IntegerConditionColumnAttributeTest extends AbstractAttributeTest {
    @Override
    protected Object randomAttributeValue() {
        return new IntegerCondition(IntegerCondition.Condition.LESS, Randoms.longValue());
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:integer_condition_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
