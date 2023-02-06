package ru.yandex.market.checkout.checkouter.json;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.order.IncreaseExpirationTimeResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;

public class IncreaseExpirationTimeResponseJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws Exception {
        String json = "{ \"failedOrderIds\": [ 1, 2, 3 ]}";

        IncreaseExpirationTimeResponse response = read(IncreaseExpirationTimeResponse.class, json);

        assertThat(response.getFailedOrderIds(), hasItems(1L, 2L, 3L));
    }

    @Test
    public void serialize() throws Exception {
        IncreaseExpirationTimeResponse increaseExpirationTimeResponse = new IncreaseExpirationTimeResponse();
        increaseExpirationTimeResponse.setFailedOrderIds(Arrays.asList(1L, 2L, 3L));

        String json = write(increaseExpirationTimeResponse);

        checkJson(json, "$.failedOrderIds", JsonPathExpectationsHelper::assertValueIsArray);
        checkJson(json, "$.failedOrderIds[0]", 1);
        checkJson(json, "$.failedOrderIds[1]", 2);
        checkJson(json, "$.failedOrderIds[2]", 3);
    }
}
