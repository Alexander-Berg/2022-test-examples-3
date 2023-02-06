package ru.yandex.market.billing.checkout;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.RequestMatcher;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;

/**
 * @author vbudnev
 */
interface ResourceUtilitiesMixin extends ResourceUtilities {

    default List<OrderHistoryEvent> events(String jsonPath) throws IOException {
        return new ArrayList<>(
                getObjectMapper().readValue(
                        getResourceAsInputStream(jsonPath),
                        OrderHistoryEvents.class
                ).getContent()
        );
    }

    ObjectMapper getObjectMapper();
}
