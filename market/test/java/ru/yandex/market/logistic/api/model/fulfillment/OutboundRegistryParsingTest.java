package ru.yandex.market.logistic.api.model.fulfillment;

import java.io.IOException;
import java.util.Set;

import javax.validation.ConstraintViolation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.utils.ParsingTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

class OutboundRegistryParsingTest extends ParsingTest<OutboundRegistry> {
    OutboundRegistryParsingTest() {
        super(OutboundRegistry.class, "fixture/entities/fulfillment/outbound_registry.xml");
    }

    @Test
    void testValidation() throws IOException {
        String fileContent = getFileContent("fixture/entities/fulfillment/outbound_registry.xml");
        OutboundRegistry registry = getMapper().readValue(fileContent, OutboundRegistry.class);

        Set<ConstraintViolation<OutboundRegistry>> constraintViolations = VALIDATOR.validate(registry);

        assertThat(constraintViolations).isEmpty();
    }
}
