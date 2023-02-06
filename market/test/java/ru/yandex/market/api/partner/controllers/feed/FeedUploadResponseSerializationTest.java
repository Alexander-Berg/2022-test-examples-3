package ru.yandex.market.api.partner.controllers.feed;

import org.junit.Test;

import ru.yandex.market.api.partner.controllers.serialization.BaseJaxbSerializationTest;
import ru.yandex.market.core.feed.dto.FeedUploadResponse;

/**
 * @author fbokovikov
 */
public class FeedUploadResponseSerializationTest extends BaseJaxbSerializationTest {

    private static final FeedUploadResponse OK_RESPONSE = new FeedUploadResponse(123L);
    private static final String EXPECTED_JSON = "{\"uploadId\":123}";
    private static final String EXPECTED_XML =
            "<feed-upload-response>" +
                    "<upload-id>123</upload-id>" +
                    "</feed-upload-response>";

    @Test
    public void testOkResponse() {
        testSerialization(OK_RESPONSE, EXPECTED_JSON, EXPECTED_XML);
    }
}
