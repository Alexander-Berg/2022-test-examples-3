package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;


@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/checkouter-serialization.xml"}
)
public class OrderSearchRequestJsonDeserializerTest {

    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldProcessEmptyOptionsWell() throws IOException {
        String expected = "" +
                "{\"orderIds\":null," +
                "\"userId\":12312," +
                "\"shopId\":774," +
                "\"statuses\":[\"PROCESSING\",\"PICKUP\",\"PENDING\"]," +
                "\"substatuses\":null," +
                "\"fake\":false," +
                "\"contexts\":[\"MARKET\",\"SELF_CHECK\"]," +
                "\"fromDate\":null," +
                "\"toDate\":1496833421," +
                "\"pageInfo\":{\"total\":null,\"from\":30,\"to\":50,\"pageSize\":null,\"pagesCount\":null," +
                "\"currentPage\":null}," +
                "\"paymentId\":null,\"notStatuses\":null,\"notSubstatuses\":null," +
                "\"excludeABO\":false,\"paymentType\":null,\"paymentMethod\":null,\"statusUpdateFromDate\":null," +
                "\"statusUpdateToDate\":null,\"statusUpdateFromTimestamp\":null," +
                "\"statusUpdateToTimestamp\":1496833421,\"buyerPhone\":\"+504346248\"," +
                "\"buyerEmail\":\"somebody@something.somewhere\"," +
                "\"buyerEmailSubstring\":\"somebody\",\"shopOrderIds\":null,\"acceptMethod\":\"PUSH_API\"," +
                "\"lastStatusRole\":\"SHOP\",\"noAuth\":false,\"userGroups\":null}";
        HttpInputMessage inputMessage = new MockHttpInputMessage(expected.getBytes());
        OrderSearchRequest request = (OrderSearchRequest) converter.read(OrderSearchRequest.class, inputMessage);

    }

}
