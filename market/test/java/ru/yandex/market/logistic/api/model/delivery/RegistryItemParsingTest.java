package ru.yandex.market.logistic.api.model.delivery;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class RegistryItemParsingTest extends ParsingTest<RegistryItem> {
    RegistryItemParsingTest() {
        super(RegistryItem.class, "fixture/entities/delivery/registry_item_full.xml");
    }

    @Test
    void testValidation() throws IOException {
        String fileContent = getFileContent("fixture/entities/delivery/registry_item_full.xml");
        RegistryItem item = getMapper().readValue(fileContent, RegistryItem.class);

        Set<ConstraintViolation<RegistryItem>> constraintViolations = VALIDATOR.validate(item);

        assertThat(constraintViolations).isEmpty();
    }
}
