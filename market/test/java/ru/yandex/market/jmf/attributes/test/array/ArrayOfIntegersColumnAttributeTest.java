package ru.yandex.market.jmf.attributes.test.array;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.utils.ValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(classes = ArrayOfIntegersColumnAttributeTest.Configuration.class)
public class ArrayOfIntegersColumnAttributeTest extends AbstractArrayAttributeTest<Long> {

    public ArrayOfIntegersColumnAttributeTest() {
        super("FROM e1 e WHERE true = array_overlap(array(:v), e.attr)");
    }

    @Override
    protected List<Long> randomAttributeValue() {
        return List.of(Randoms.longValue(), Randoms.longValue());
    }

    @Test
    public void testIntegerMaxSize() {
        var thrown = assertThrows(ValidationException.class, () -> getEntity(randomAttributeValue(), "attr2"));
        assertThat(thrown.getMessage(), CoreMatchers.containsString("Превышено допустимое кол-во значений " +
                "Array of integers attribute. Сейчас 2 (макс 1)"));
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:array_of_integers_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
