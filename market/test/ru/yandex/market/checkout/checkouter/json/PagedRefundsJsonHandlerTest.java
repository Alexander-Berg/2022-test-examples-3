package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.pay.PagedRefunds;
import ru.yandex.market.checkout.common.rest.Pager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class PagedRefundsJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void deserialize() throws IOException {
        String json = "{\n" +
                "  \"pager\": {\n" +
                "    \"from\": 1,\n" +
                "    \"to\": 10,\n" +
                "    \"page\": 1,\n" +
                "    \"pageSize\": 10\n" +
                "  },\n" +
                "  \"refunds\": [\n" +
                "    {\n" +
                "      \"id\": 123,\n" +
                "      \"orderId\": 456,\n" +
                "      \"paymentId\": 789,\n" +
                "      \"trustRefundId\": \"trustRefundId\",\n" +
                "      \"hasReceipt\": true,\n" +
                "      \"currency\": \"RUR\",\n" +
                "      \"amount\": 12.34,\n" +
                "      \"orderRemainder\": 56.78,\n" +
                "      \"comment\": \"comment\",\n" +
                "      \"status\": \"SUCCESS\",\n" +
                "      \"substatus\": \"REFUND_FAILED\",\n" +
                "      \"createdBy\": 987,\n" +
                "      \"createdByRole\": \"SHOP\",\n" +
                "      \"shopManagerId\": 654,\n" +
                "      \"creationDate\": \"21-12-5490 08:31:51\",\n" +
                "      \"updateDate\": \"11-12-9011 14:03:42\",\n" +
                "      \"statusUpdateDate\": \"17-02-107599 00:55:33\",\n" +
                "      \"statusExpiryDate\": \"02-11-142808 08:14:04\",\n" +
                "      \"reason\": \"ORDER_CHANGED\",\n" +
                "      \"fake\": true\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        PagedRefunds pagedRefunds = read(PagedRefunds.class, json);

        Assertions.assertNotNull(pagedRefunds.getPager());
        Assertions.assertEquals(1, pagedRefunds.getPager().getFrom().intValue());
        Assertions.assertEquals(10, pagedRefunds.getPager().getTo().intValue());
        Assertions.assertEquals(1, pagedRefunds.getPager().getCurrentPage().intValue());
        Assertions.assertEquals(10, pagedRefunds.getPager().getPageSize().intValue());

        Assertions.assertNotNull(pagedRefunds.getItems());
        assertThat(pagedRefunds.getItems(), hasSize(1));
    }

    @Test
    public void serialize() throws IOException, ParseException {
        PagedRefunds pagedRefunds = new PagedRefunds();
        pagedRefunds.setPager(Pager.atPage(1, 10));
        pagedRefunds.setItems(Collections.singletonList(EntityHelper.getRefund()));

        String json = write(pagedRefunds);

        checkJson(json, "$.pager", JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Refund.REFUNDS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Refund.REFUNDS, hasSize(1));
    }
}
