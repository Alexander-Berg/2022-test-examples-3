package ru.yandex.market.api.partner.controllers.feed;

import org.junit.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;
import ru.yandex.market.core.feed.dto.FeedValidationResponse;

/**
 * @author fbokovikov
 */
public class FeedValidationResponseSerializationTest extends BaseJaxbSerializationTest {

    private static final FeedValidationResponse OK_RESPONSE = new FeedValidationResponse(123L);
    private static final String EXPECTED_JSON =
            "{\"validationId\":123}";
    private static final String EXPECTED_XML =
            "<feed-validation-response>" +
                    "<validation-id>123</validation-id>" +
                    "</feed-validation-response>";

    @Test
    public void testOkResponse() {
        testSerialization(OK_RESPONSE, EXPECTED_JSON, EXPECTED_XML);
    }
}
