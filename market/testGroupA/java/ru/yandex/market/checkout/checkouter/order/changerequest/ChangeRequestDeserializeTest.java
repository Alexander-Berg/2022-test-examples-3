package ru.yandex.market.checkout.checkouter.order.changerequest;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;

/**
 * @author mmetlov
 */
public class ChangeRequestDeserializeTest extends AbstractServicesTestBase {

    private static final String NEW_CHANGE_REQUEST = "  {\n" +
            "    \"authorRole\": \"SYSTEM\",\n" +
            "    \"payload\": {\n" +
            "      \"qwe\": 123,\n" +
            "      \"@class\": \"ru.yandex.market.checkout.checkouter.order.changerequest" +
            ".SomeNewChangeRequestPayload\"\n" +
            "    },\n" +
            "    \"createdAt\": \"2020-02-17T11:42:21.062013Z\",\n" +
            "    \"status\": \"APPLIED\",\n" +
            "    \"type\": \"SOME_NEW_TYPE\",\n" +
            "    \"orderId\": 14375759,\n" +
            "    \"id\": 1017199\n" +
            "  }";

    @Autowired
    private ObjectMapper checkouterAnnotationObjectMapper;

    @Test
    public void shouldDeserializeNewChangeRequestToUnknown() throws IOException {
        ChangeRequest changeRequest = checkouterAnnotationObjectMapper.readValue(NEW_CHANGE_REQUEST,
                ChangeRequest.class);
        Assertions.assertTrue(changeRequest.getPayload() instanceof UnknownChangeRequestPayload);
    }
}
