package ru.yandex.market.logistic.api.model.fulfillment;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class InboundRegistryParsingTest extends ParsingTest<InboundRegistry> {
    InboundRegistryParsingTest() {
        super(InboundRegistry.class, "fixture/entities/fulfillment/inbound_registry.xml");
    }

    @Test
    void testValidation() throws IOException {
        String fileContent = getFileContent("fixture/entities/fulfillment/inbound_registry.xml");
        InboundRegistry registry = getMapper().readValue(fileContent, InboundRegistry.class);

        Set<ConstraintViolation<InboundRegistry>> constraintViolations = VALIDATOR.validate(registry);

        assertThat(constraintViolations).isEmpty();
    }
}
