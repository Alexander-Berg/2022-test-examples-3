package ru.yandex.market.crm.platform.mappers;

import java.nio.charset.StandardCharsets;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.platform.models.EmailContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author apershukov
 */
class EmailContextMapperTest {

    private final EmailContextMapper mapper = new EmailContextMapper("market");

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
        assertEquals(317153, fact.getCampaignId());
        assertEquals(641115, fact.getLetterId());

        assertEquals(
                "<20220110144502.540217.dcc797761c334ca9b7c375d0af1d9600@api-9.production.ysendercloud>",
                fact.getMessageId()
        );

        var expectedContext = "{\"color\":\"BLUE\",\"sendr\":{\"loaded_data\":null," +
                "\"generators\":[],\"account_name\":\"market\"},\"is_tinkoff_credit\":false," +
                "\"fromDate\":\"11 января\",\"delivery_price\":\"25\",\"order_number\":\"87655937\"," +
                "\"delivery_date\":\"14.01.22\"}";

        assertEquals(expectedContext, fact.getContext());
    }

    /**
     * Строка без контекста пропускается
     */
    @Test
    void testSkipLineWithoutContext() {
        var line = "tskv\t" +
                "tskv_format=sendr-delivery-log\t" +
                "unixtime=1641825903\t" +
                "full_timestamp=2022-01-10 17:45:03.923686+0300\t" +
                "pid=585352\t" +
                "account=market\t" +
                "campaign_id=317153\t" +
                "campaign_name=UNPAID white market\t" +
                "campaign_type=transact\tchannel=email\t" +
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
        assertNull(fact);
    }

    /**
     * Строка в контексте которой лежит некорректный json, не позволяющий адекватно вытащить значение поля,
     * пропускается
     */
    @Test
    void testSkipLineWithMalformedContextFieldValue() {
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
                ".qloud-c.yandex.net\",\"context\":{\"color\":\"BLUE\"\t" +
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
        assertNull(fact);
    }

    /**
     * Строки, принадлежащие чужим аккаунтам, пропускаются
     */
    @Test
    void testSkipLinesOfForeignAccounts() {
        var line = "tskv\t" +
                "tskv_format=sendr-delivery-log\t" +
                "unixtime=1641825903\t" +
                "full_timestamp=2022-01-10 17:45:03.923686+0300\t" +
                "pid=585352\t" +
                "account=taxi\t" +
                "campaign_id=317153\t" +
                "campaign_name=UNPAID white market\t" +
                "campaign_type=transact\tchannel=email\t" +
                "context={\"to_email\":\"kasatkinasvetlanaspb@gmail" +
                ".com\",\"task_id\":\"40192254-c72f-4e4a-a9f9-04d8678d703d\",\"cc\":[],\"bcc\":[]," +
                "\"to\":[{\"name\":\"\",\"email\":\"kasatkinasvetlanaspb@gmail.com\"}],\"host\":\"sas1-58dc3f2e2086" +
                ".qloud-c.yandex.net\",\"context\":{\"color\":\"BLUE\",\"sendr\":{\"loaded_data\":null," +
                "\"generators\":[],\"account_name\":\"market\"},\"is_tinkoff_credit\":false," +
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
        assertNull(fact);
    }

    /**
     * Сообщения которые не удалось отправить пропускаются
     */
    @Test
    void testSkipFailedEmail() {
        var line = "tskv\ttskv_format=sendr-delivery-log\tunixtime=1643341109\tfull_timestamp=2022-01-28 " +
                "06:38:29.428786+0300\tpid=707171\taccount=market\tcampaign_id=658480\t" +
                "campaign_name=Категорийное массовое начисление (итерация 4)\tcampaign_type=simple\tchannel=email\t" +
                "context={\"host\":\"myt6-56c7327b6df2.qloud-c.yandex.net\",\"to_email\":\"agurov31@yandex.ru\"," +
                "\"task_id\":\"946bf73b-1e27-4ca7-a4fa-e564663d191a\",\"context\":{\"data\":" +
                "\"{\\\\\"variant\\\\\":\\\\\"reg_all_em_kategorijnoe_nachislenie_d" +
                "\\\\\",\\\\\"user\\\\\":{\\\\\"eh\\\\\":\\\\\"f48336fb83b3a954b4d33f44d8b6d8c3\\\\\"," +
                "\\\\\"name\\\\\":\\\\\"\\\\\",\\\\\"unsubscribe\\\\\":\\\\\"https://market.yandex" +
                ".ru/my/unsubscribe?utm_campaign\\=reg_all_em_kategorijnoe_nachislenie_d&utm_source\\=email" +
                "&utm_medium\\=regular_mailing&utm_referrer\\=627&eh\\=f48336fb83b3a954b4d33f44d8b6d8c3&ecid" +
                "\\=reg_all_em_kategorijnoe_nachislenie%3A1&clid\\=627&action" +
                "\\=74696d653d3136343333343037303833383626747970653d3226656d61696c3d616775726f7633314079616e6465782e" +
                "7275267569643d31343031323838353538&sk\\=f4c1a883efd65694c6af7269d03d1aaaedac1415f4fa85a5378f861e2ee5899a\\\\\"}" +
                ",\\\\\"u_vars\\\\\":{},\\\\\"blocks\\\\\":[{\\\\\"id\\\\\":\\\\\"info_1\\\\\",\\\\\"type\\\\\":\\\\\"INFO\\\\\"}]}\"," +
                "\"email\":\"agurov31@yandex.ru\"}}\tfor_testing=False\tis_emc=False\tletter_code=A\t" +
                "letter_id=666159\trecepient=agurov31@yandex.ru\tresults={\"error\":{\"text\":null,\"code\":200}}\t" +
                "status=200\ttags=[\"reg_all_em_kategorijnoe_nachislenie:1\"]";

        var fact = map(line);
        assertNull(fact);
    }

    /**
     * Строка, являющаяся продуктом слияния двух других, пропускается
     */
    @Test
    void testSkipMalformedLine() {
        var line = "tskv\ttskv_format=sendr-delivery-log\tunixtime=1643350959\t" +
                "full_timestamp=2022-01-28 09:22:39.088545+0300\tpid=175106\taccount=market\tcampaign_id=650835\t" +
                "campaign_name=Брошенная корзина Ребрендинг\tcampaign_type=transact\tchannel=email\t" +
                "context={\"to_email\":\"klin7984@yandex.ru\",\"task_id\":\"273fa2d3-ad61-4728-b9ff-82160d7256af\"," +
                "\"cc\":[],\"bcc\":[],\"to\":[{\"name\":\"\",\"email\":\"klin7984@yandex.ru\"}],\"host\":\"iva8-3dc7a9ada2e2.qloud-c" +
                ".yandex.net\",\"context\":{\"models\":[{\"name\":\"\\\\u0410\\\\u043b\\\\u044e\\\\u043c\\\\u0438" +
                "\\\\u043d\\\\u0438\\\\u0435\\\\u0432" +
                "\\\\u0430\\\\u044f \\\\u043a\\\\u043b\\\\u0435\\\\u0439\\\\u043a\\\\u0430\\\\u044f " +
                "\\\\u043b\\\\u0435\\\\u043d\\\\u0442\\\\u0430 \\\\u0441\\\\u0430\\\\u043c\\\\u043e\\\\" +
                "u043a\\\\u043b\\\\u0435\\\\u044f\\\\u0449\\\\u0430\\\\u044f\\\\u0441\\\\u044f 48\\\\u043c\\\\u043c " +
                "\\\\u0445 10\\\\u043c | \\\\u0430\\\\u043b\\\\u044e\\\\u043c\\\\u0438\\\\u043d\\\\u0438\\\\u0435" +
                "\\\\u0432\\\\u044b\\\\u0439 \\\\u0441\\\\u043a\\\\u043e\\\\u0442\\\\u0447 |\",\"img\":" +
                "\"https://avatars.mds.yandex.net/get-mpic/4413406/img_id835831912579585909" +
                ".jpeg/600x600\",\"price\":\"192\",\"onStock\":true,\"link\":\"https://pokupki.market.yandex" +
                ".ru/product/101487864573?offerId\\=WmDxswKjKJeRTwDmaOYkFw\",\"hid\":15140556,\"type\":\"OFFER\"," +
                "\"id\":\"WmDxswKjKJeRTwDmaOYkFw\"}],\"sendr\":{\"loaded_data\":null,\"generators\":[]," +
                "\"account_name\":\"market\"}," +
                "\"data\":{\"utm_campaign\":\"abandoned_cart_2\",\"blocks\":[{\"type\":\"BANNER\",\"id\":\"creative\"}," +
                "{\"type\":\"INFO\",\"id\":\"var_model_1\"},{\"models\":[{\"name\":\"\\\\u0410\\\\u043b\\\\u044e\\\\u043c\\\\u0438" +
                "\\\\u043d\\\\u0438\\\\u0435\\\\u0432\\\\u0430\\\\u044f \\\\u043a\\\\u043b\\\\u04" +
                "35\\\\u0439\\\\u043a\\\\u0430\\\\u044f \\\\u043b\\\\u0435\\\\u043d\\\\u0442\\\\u0430 " +
                "\\\\u0441\\\\u0430\\\\u043c\\\\u043e\\\\u043a\\\\u043b\\\\u0435\\\\u044f\\\\u0449\\\\u0430\\\\u044f" +
                "\\\\u0441\\\\u044f 48\\\\u043c\\\\u043c \\\\u0445 10\\\\u043c | \\\\u0430" +
                "\\\\u043b\\\\u044e\\\\u043c\\\\u0438\\\\u043d\\\\u0438\\\\u0435\\\\u0432\\\\u044b\\\\u0439 " +
                "\\\\u0441\\\\u043a\\\\u043e\\\\u0442\\\\u0447 |\",\"img\":\"https://avatars.mds.yandex" +
                ".net/get-mpic/4413406/img_id835831912579585909.jpeg/9hq\",\"pr" +
                "ice\":\"192\",\"onStock\":true,\"hid\":15140556,\"type\":\"SKU\",\"id\":\"101487864573\"}," +
                "{\"name\":\"\\\\u0421\\\\u0432\\\\u0435\\\\u0440\\\\u0445\\\\u0441\\\\u0438\\\\u043b\\\\u044c" +
                "\\\\u043d\\\\u0430\\\\u044f \\\\u043a\\\\u043b\\\\u0435\\\\u0439\\\\u" +
                "043a\\\\u0430\\\\u044f \\\\u043b\\\\u0435\\\\u043d\\\\u0442\\\\u0430 / " +
                "\\\\u0410\\\\u0440\\\\u043c\\\\u0438\\\\u0440\\\\u043e\\\\u0432\\\\u0430\\\\u043d\\\\u043d\\\\u0430" +
                "\\\\u044f \\\\u0441\\\\u0432\\\\u0435\\\\u0440\\\\u0445\\\\u043f\\\\u0440\\\\u043e\\\\u04" +
                "47\\\\u043d\\\\u0430\\\\u044f \\\\u0441\\\\u0430\\\\u043c\\\\u043e\\\\u043a\\\\u043b\\\\u0435" +
                "\\\\u044f\\\\u0449\\\\u0430\\\\u044f\\\\u0441\\\\u044f \\\\u043b\\\\u0435\\\\u043d\\\\u0442\\\\u0430" +
                " / Flex Tape / \\\\utskv\ttskv_format=sendr-delivery-log";

        var fact = map(line);
        assertNull(fact);
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

    @Nullable
    private EmailContext map(@Nonnull String line) {
        var result = mapper.apply(line.getBytes(StandardCharsets.UTF_8));
        return result.isEmpty() ? null : result.get(0);
    }
}
