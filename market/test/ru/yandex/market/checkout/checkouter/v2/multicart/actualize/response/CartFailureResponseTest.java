package ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig.objectMapperPrototype;
import static ru.yandex.market.checkout.checkouter.v2.multicart.actualize.MultiCartV2ResponseMapper.buildCartResponse;

public class CartFailureResponseTest {

    private static final ObjectMapper MAPPER = objectMapperPrototype(new SimpleFilterProvider()
            .setFailOnUnknownId(false));

    @Test
    void shouldSerializeCartFailure() throws JsonProcessingException {
        var cart = OrderProvider.getBlueOrder();
        var json = MAPPER.writeValueAsString(CartFailureResponse.builder()
                .withCart(buildCartResponse(cart))
                .withErrorCode(OrderFailure.Code.UNKNOWN_ERROR)
                .withErrorReason(OrderFailure.Reason.UNKNOWN_VALUE)
                .withErrorDetails("some details")
                .withErrorDevDetails("some details")
                .build());

        assertThat(json, not(containsString("\"cart\"")));
        assertThat(json, containsString("\"label\":"));
        assertThat(json, containsString("\"shopId\":"));
        assertThat(json, containsString("\"error\":\"UNKNOWN_ERROR\""));
        assertThat(json, containsString("\"errorReason\":\"UNKNOWN_VALUE\""));
        assertThat(json, containsString("\"errorDetails\":\"some details\""));
        assertThat(json, containsString("\"errorDevDetails\":\"some details\""));
    }

    @Test
    void shouldDeserializeCartFailure() throws IOException {
        var cartFailureResponse = MAPPER.readValue("{" +
                "\"label\":\"some label\"," +
                "\"shopId\":123," +
                "\"items\":[{\"offerId\":\"some offer\",\"feedId\":123,\"buyerPrice\":100.50}]," +
                "\"error\":\"UNKNOWN_ERROR\"," +
                "\"errorReason\":\"UNKNOWN_VALUE\"," +
                "\"errorDetails\":\"some details\"," +
                "\"errorDevDetails\":\"some details\"" +
                "}", CartFailureResponse.class);

        assertThat(cartFailureResponse.getErrorCode(), is(OrderFailure.Code.UNKNOWN_ERROR));
        assertThat(cartFailureResponse.getErrorReason(), is(OrderFailure.Reason.UNKNOWN_VALUE));
        assertThat(cartFailureResponse.getErrorDetails(), is("some details"));
        assertThat(cartFailureResponse.getErrorDevDetails(), is("some details"));
        assertThat(cartFailureResponse.getCart(), notNullValue());
        assertThat(cartFailureResponse.getCart().getLabel(), is("some label"));
        assertThat(cartFailureResponse.getCart().getShopId(), comparesEqualTo(123L));
        assertThat(cartFailureResponse.getCart().getItems(), notNullValue());
        assertThat(cartFailureResponse.getCart().getItems(), hasItem(allOf(
                hasProperty("offerId", is("some offer")),
                hasProperty("feedId", comparesEqualTo(123L)),
                hasProperty("buyerPrice", comparesEqualTo(BigDecimal.valueOf(100.50)))
        )));
    }
}
