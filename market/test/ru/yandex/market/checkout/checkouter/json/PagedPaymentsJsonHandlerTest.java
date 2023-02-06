package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.PagedPayments;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PagedPaymentsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws IOException, ParseException {
        PagedPayments pagedPayments = new PagedPayments();
        pagedPayments.setPager(Pager.atPage(1, 10));
        pagedPayments.setItems(Collections.singletonList(EntityHelper.getPayment()));

        String json = write(pagedPayments);
        System.out.println(json);

        checkJson(json, "$.pager", JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Payment.PAYMENTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Payment.PAYMENTS, hasSize(1));
    }

    @Test
    public void deserialize() throws IOException {
        String json = "{\n" +
                "  \"pager\": {\n" +
                "    \"from\": 1,\n" +
                "    \"to\": 10,\n" +
                "    \"page\": 1,\n" +
                "    \"pageSize\": 10\n" +
                "  },\n" +
                "  \"payments\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"orderId\": 456,\n" +
                "      \"basketId\": \"basketId\",\n" +
                "      \"status\": \"HOLD\",\n" +
                "      \"substatus\": \"HOLD_FAILED\",\n" +
                "      \"uid\": 789,\n" +
                "      \"currency\": \"USD\",\n" +
                "      \"totalAmount\": 3.45,\n" +
                "      \"creationDate\": \"11-11-2017 15:00:00\",\n" +
                "      \"updateDate\": \"15-11-2017 18:00:00\",\n" +
                "      \"statusUpdateDate\": \"13-11-2017 22:00:00\",\n" +
                "      \"statusExpiryDate\": \"16-11-2017 00:00:00\",\n" +
                "      \"fake\": true,\n" +
                "      \"failReason\": \"failReason\",\n" +
                "      \"prepayType\": \"YANDEX_MARKET\",\n" +
                "      \"balancePayMethodType\": \"BALANCE_PAY_METHOD_TYPE\",\n" +
                "      \"paymentForm\": {\n" +
                "        \"token\": \"token\"\n" +
                "      },\n" +
                "      \"failDescription\": \"failDescription\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        PagedPayments pagedPayments = read(PagedPayments.class, json);

        Assertions.assertNotNull(pagedPayments.getPager());
        Assertions.assertEquals(1, pagedPayments.getPager().getFrom().intValue());
        Assertions.assertEquals(10, pagedPayments.getPager().getTo().intValue());
        Assertions.assertEquals(1, pagedPayments.getPager().getCurrentPage().intValue());
        Assertions.assertEquals(10, pagedPayments.getPager().getPageSize().intValue());

        Assertions.assertNotNull(pagedPayments.getItems());
        assertThat(pagedPayments.getItems(), hasSize(1));
    }
}
