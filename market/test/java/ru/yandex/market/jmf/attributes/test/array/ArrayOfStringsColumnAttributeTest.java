package ru.yandex.market.jmf.attributes.test.array;

import java.util.Collection;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.utils.ValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ContextConfiguration(classes = ArrayOfStringsColumnAttributeTest.Configuration.class)
public class ArrayOfStringsColumnAttributeTest extends AbstractArrayAttributeTest<String> {

    public ArrayOfStringsColumnAttributeTest() {
        super("FROM e1 e WHERE true = array_overlap(array_of_text(:v), e.attr)");
    }

    @Override
    protected Collection<String> randomAttributeValue() {
        return List.of(Randoms.string(), Randoms.string());
    }

    @Test
    public void testStringMaxSize() {
        var thrown = assertThrows(ValidationException.class, () -> getEntity(randomAttributeValue(), "attr2"));
        assertThat(thrown.getMessage(), CoreMatchers.containsString("Превышено допустимое кол-во значений " +
                "Array of strings attribute. Сейчас 2 (макс 1)"));
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:array_of_strings_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
