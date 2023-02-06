package ru.yandex.market.logistic.api.client.model.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.client.LogisticApiClientFactory;
import ru.yandex.market.logistic.api.model.fulfillment.response.CancelInboundResponse;
import ru.yandex.market.logistic.api.model.validation.ResponseTypeValidator;
import ru.yandex.market.logistic.api.model.validation.ValidResponseType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.logistic.api.TestUtils.VALIDATOR;

/**
 * Тест проверяет работу аннотации {@link ValidResponseType} и валидатора {@link ResponseTypeValidator}.
 */
class ResponseTypeValidatorTest {

    private static final XmlMapper XML_MAPPER = LogisticApiClientFactory.createXmlMapper();

    @Test
    void testSuccessful() throws Exception {
        CancelInboundResponse cancelInboundResponse =
            XML_MAPPER.readValue("<response type=\"cancelInbound\"/>", CancelInboundResponse.class);
        assertTrue(VALIDATOR.validate(cancelInboundResponse).isEmpty());
    }

    @Test
    void testValidationError() throws Exception {
        CancelInboundResponse cancelInboundResponse =
            XML_MAPPER.readValue("<response type=\"sdfdsfdsf\"/>", CancelInboundResponse.class);

        Set<ConstraintViolation<CancelInboundResponse>> constraintViolations =
            VALIDATOR.validate(cancelInboundResponse);

        assertFalse(constraintViolations.isEmpty());

        boolean hasExceptedMessage = constraintViolations
            .stream()
            .map(ConstraintViolation::getMessage)
            .anyMatch(message -> message.equals("Wrong response type"));

        assertTrue(hasExceptedMessage);
    }

    @Test
    void testNamespacesIgnored() throws Exception {
        CancelInboundResponse cancelInboundResponse =
            XML_MAPPER.readValue("<response xsi:type=\"cancelInboundResponse\" type=\"cancelInbound\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>", CancelInboundResponse.class);
        assertTrue(VALIDATOR.validate(cancelInboundResponse).isEmpty());

        cancelInboundResponse =
            XML_MAPPER.readValue("<response type=\"cancelInbound\" xsi:type=\"cancelInboundResponse\" " +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>", CancelInboundResponse.class);
        assertTrue(VALIDATOR.validate(cancelInboundResponse).isEmpty());

        cancelInboundResponse =
            XML_MAPPER.readValue("<response xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                "type=\"cancelInbound\" xsi:type=\"cancelInboundResponse\"/>", CancelInboundResponse.class);
        assertTrue(VALIDATOR.validate(cancelInboundResponse).isEmpty());
    }
}
