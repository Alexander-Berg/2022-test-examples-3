package ru.yandex.market.logistic.api.model.delivery;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class MovementRegistryParsingTest extends ParsingTest<MovementRegistry> {
    MovementRegistryParsingTest() {
        super(MovementRegistry.class, "fixture/entities/delivery/movement_registry.xml");
    }

    @Test
    void testValidation() throws IOException {
        String fileContent = getFileContent("fixture/entities/delivery/movement_registry.xml");
        MovementRegistry registry = getMapper().readValue(fileContent, MovementRegistry.class);

        Set<ConstraintViolation<MovementRegistry>> constraintViolations = VALIDATOR.validate(registry);

        assertThat(constraintViolations).isEmpty();
    }
}
