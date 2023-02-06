package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.models.Email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SenderDeliveryLogEmailMapperTest {

    private final SenderDeliveryLogEmailMapper mapper = new SenderDeliveryLogEmailMapper("market");


    @Test
    void testParseMessage() {
        var line = "tskv\t" +
                "tskv_format=sendr-delivery-log\t" +
                "unixtime=1641825903\t" +
                "full_timestamp=2022-01-10 17:45:03.923686+0300\t" +
                "pid=585352\t" +
                "account=market\t" +
                "campaign_id=317153\t" +
                "campaign_name=UNPAID white market\t" +
                "campaign_type=transact\t" +
                "channel=email\t" +
                "context={\"to_email\":\"kasatkinasvetlanaspb@gmail" +
                ".com\",\"task_id\":\"40192254-c72f-4e4a-a9f9-04d8678d703d\",\"cc\":[],\"bcc\":[]," +
                "\"to\":[{\"name\":\"\",\"email\":\"kasatkinasvetlanaspb@gmail.com\"}],\"host\":\"sas1-58dc3f2e2086" +
                ".qloud-c.yandex.net\",\"context\":{\"color\":\"BLUE\",\"sendr\":{\"loaded_data\":null," +
                "\"generators\":[],\"account_name\":\"market\"},\"is_tinkoff_credit\":false,\"fromDate\":\"11 " +
                "\\\\u044f\\\\u043d\\\\u0432\\\\u0430\\\\u0440\\\\u044f\"," +
                "\"delivery_price\":\"25\",\"order_number\":\"87655937\",\"delivery_date\":\"14.01.22\"}}\t" +
                "for_testing=False\t" +
                "is_emc=False\t" +
                "letter_code=GB\t" +
                "letter_id=641115\t" +
                "message-id=<20220110144502.540217.dcc797761c334ca9b7c375d0af1d9600@api-9.production.ysendercloud>\t" +
                "recepient=user@yandex.ru\t" +
                "results={\"smtp\":{\"response\":{\"text\":\"2.0.0 Ok: queued on iva4-4ce3b18c8342.qloud-c.yandex.net " +
                "1641825903-vZRGQY9mfP-j3hGX671\"," +
                "\"code\":250}}}\t" +
                "status=0\t" +
                "tags=[\"order\"]";

        var fact = map(line);

        assertNotNull(fact);
        assertEquals("user@yandex.ru", fact.getUid().getStringValue());
        assertEquals(317153, fact.getSenderInfo().getCampaignId());
        assertEquals(1641825903000L, fact.getSenderInfo().getSendTimestamp());
        assertEquals(1641825903000L, fact.getTimestamp());
        assertEquals(
                "<20220110144502.540217.dcc797761c334ca9b7c375d0af1d9600@api-9.production.ysendercloud>",
                fact.getMessageId()
        );
    }

    /**
     * Строка с некорректным message-id (слишком большим) пропускается
     */
    @Test
    void testSkipLineWithIncorrectMessageId() {
        var incorrectMessageId = "<20220110144502.540217.dcc797761c334ca9@api-9.production.ysendercloud>;"
                .repeat(10);
        var line = "tskv\t" +
                "tskv_format=sendr-delivery-log\t" +
                "unixtime=1641825903\t" +
                "full_timestamp=2022-01-10 17:45:03.923686+0300\t" +
                "pid=585352\t" +
                "account=market\t" +
                "campaign_id=317153\t" +
                "campaign_name=UNPAID white market\t" +
                "campaign_type=transact\tchannel=email\t" +
                "context={\"to_email\":\"kasatkinasvetlanaspb@gmail" +
                ".com\",\"task_id\":\"40192254-c72f-4e4a-a9f9-04d8678d703d\",\"cc\":[],\"bcc\":[]," +
                "\"to\":[{\"name\":\"\",\"email\":\"kasatkinasvetlanaspb@gmail.com\"}],\"host\":\"sas1-58dc3f2e2086" +
                ".qloud-c.yandex.net\",\"context\":{\"color\":\"BLUE\",\"sendr\":{\"loaded_data\":null," +
                "\"generators\":[],\"account_name\":\"market\"},\"is_tinkoff_credit\":false,\"fromDate\":\"11 " +
                "\\\\u044f\\\\u043d\\\\u0432\\\\u0430\\\\u0440\\\\u044f\"," +
                "\"delivery_price\":\"25\",\"order_number\":\"87655937\",\"delivery_date\":\"14.01.22\"}}\t" +
                "for_testing=False\t" +
                "is_emc=False\t" +
                "letter_code=GB\t" +
                "letter_id=641115\t" +
                "message-id=" + incorrectMessageId + "\t" +
                "recepient=user@yandex.ru\t" +
                "results={\"smtp\":{\"response\":{\"text\":\"2.0.0 Ok: queued on iva4-4ce3b18c8342.qloud-c.yandex.net " +
                "1641825903-vZRGQY9mfP-j3hGX671\"," +
                "\"code\":250}}}\t" +
                "status=0\t" +
                "tags=[\"order\"]";

        var fact = map(line);
        assertNull(fact);
    }

    /**
     * Строка с некорректным email (слишком большим) пропускается
     */
    @Test
    void testSkipLineWithIncorrectEmail() {
        var incorrectEmail = "1234567890".repeat(30) + "@ya.ru";
        var line = "tskv\t" +
                "tskv_format=sendr-delivery-log\t" +
                "unixtime=1641825903\t" +
                "full_timestamp=2022-01-10 17:45:03.923686+0300\t" +
                "pid=585352\t" +
                "account=market\t" +
                "campaign_id=317153\t" +
                "campaign_name=UNPAID white market\t" +
                "campaign_type=transact\t" +
                "channel=email\t" +
                "context={\"to_email\":\"kasatkinasvetlanaspb@gmail" +
                ".com\",\"task_id\":\"40192254-c72f-4e4a-a9f9-04d8678d703d\",\"cc\":[],\"bcc\":[]," +
                "\"to\":[{\"name\":\"\",\"email\":\"kasatkinasvetlanaspb@gmail.com\"}],\"host\":\"sas1-58dc3f2e2086" +
                ".qloud-c.yandex.net\",\"context\":{\"color\":\"BLUE\",\"sendr\":{\"loaded_data\":null," +
                "\"generators\":[],\"account_name\":\"market\"},\"is_tinkoff_credit\":false,\"fromDate\":\"11 " +
                "\\\\u044f\\\\u043d\\\\u0432\\\\u0430\\\\u0440\\\\u044f\"," +
                "\"delivery_price\":\"25\",\"order_number\":\"87655937\",\"delivery_date\":\"14.01.22\"}}\t" +
                "for_testing=False\t" +
                "is_emc=False\t" +
                "letter_code=GB\t" +
                "letter_id=641115\t" +
                "message-id=<20220110144502.540217.dcc797761c334ca9@api-9.production.ysendercloud>\t" +
                "recepient=" + incorrectEmail + "\t" +
                "results={\"smtp\":{\"response\":{\"text\":\"2.0.0 Ok: queued on iva4-4ce3b18c8342.qloud-c.yandex.net " +
                "1641825903-vZRGQY9mfP-j3hGX671\"," +
                "\"code\":250}}}\t" +
                "status=0\t" +
                "tags=[\"order\"]";

        var fact = map(line);
        assertNull(fact);
    }

    @Nullable
    private Email map(@Nonnull String line) {
        var result = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        return result.isEmpty() ? null : result.get(0);
    }
}
