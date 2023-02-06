package ru.yandex.market.jmf.attributes.test.array;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.email.Email;

@ContextConfiguration(classes = ArrayOfEmailsColumnAttributeTest.Configuration.class)
public class ArrayOfEmailsColumnAttributeTest extends AbstractArrayAttributeTest<String> {

    @Inject
    private ObjectMapper objectMapper;

    public ArrayOfEmailsColumnAttributeTest() {
        super("FROM e1 e WHERE true = array_overlap(array_of_text(:v), e.attr)");
    }

    @Test
    public void invalidEmail() {
        var value = "invalid email";

        var values = List.of(value);
        var entity = getEntity(values);
        persist(entity);

        List result = entity.getAttribute(attributeCode);
        Assertions.assertEquals(1, result.size());

        var email = result.get(0);
        Assertions.assertEquals(value, email);
    }

    @Test
    public void jsonInvalidEmail() throws IOException {
        var value = "test@test.ru";
        String strValue = String.format("[{\"%s\": \"%s\", \"%s\": false},{}]", Email.VALUE, value, Email.IS_NOT_VALID);
        JsonNode jsonNode = objectMapper.readTree(strValue);

        var entity = getEntity(jsonNode);
        persist(entity);

        List result = entity.getAttribute(attributeCode);
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals(value, result.get(0));
        Assertions.assertNull(result.get(1));
    }

    @Override
    protected Collection<String> randomAttributeValue() {
        return List.of(Randoms.email(), Randoms.email());
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:array_of_emails_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
