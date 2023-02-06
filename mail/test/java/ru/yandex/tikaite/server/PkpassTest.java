package ru.yandex.tikaite.server;

import org.junit.Test;

import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.util.Json;

public class PkpassTest extends TestBase {
    private static final String CONTENT_TYPE_META = "Content-Type:";
    private static final String PKPASS = "application/vnd.apple.pkpass";
    private static final String DATA =
        "{\"locations\":[{\"latitude\":55.790349912308493,\"longitude\":37"
        + ".531068542327844}],\"ignoresTimeZone\":\"false\",\"relevantDate"
        + "\":\"2016-05-08T16:30:00Z\",\"organizationName\":"
        + "\"Рамблер-Касса\",\"groupingIdentifier\":\"pass.ru.rambler."
        + "kassa.site.movie\",\"passTypeIdentifier\":\"pass.ru.rambler."
        + "kassa.site\",\"formatVersion\":1,\"serialNumber\":\"23686870\","
        + "\"description\":\"Билет Рамблер Кассы. «Первый мститель: "
        + "Противостояние» в 07:30. Не опаздывайте!\\\"\",\"teamIdentifier"
        + "\":\"UM246WFXVS\",\"logoText\":\"\",\"foregroundColor\":\""
        + "#000000\",\"backgroundColor\":\"#333333\",\"eventTicket\":{\""
        + "headerFields\":[{\"key\":\"time\",\"label\":\"08 май\",\"value"
        + "\":\"19:30\"}],\"primaryFields\":[{\"key\":\"number\",\"label\""
        + ":\"Номер эл. билета\",\"value\":\"WMLFXCF\"}],\"secondaryFields"
        + "\":[{\"key\":\"event\",\"label\":\"Каро Sky 17 Авиапарк\",\""
        + "value\":\"Первый мститель: Противостояние (16+)\"}],\""
        + "auxiliaryFields\":[{\"key\":\"tickets\",\"label\":\"Зал 15\",\""
        + "value\":\"ряд 4, места 9, 10, 11, 12 (4 билета)\"},{\"key\":\""
        + "price\",\"label\":\"Цена\",\"value\":\"1892 ₽\"}],\"backFields"
        + "\":[{\"key\":\"instructions\",\"label\":\"ИНСТРУКЦИИ\",\"value"
        + "\":\"Для получения билета предъявите номер электронного билета "
        + "в кассе кинотеатра или введите его в терминале по выдаче "
        + "билетов.\"},{\"key\":\"help\",\"label\":\"ПОМОЩЬ\",\"value\":\""
        + "По всем вопросам обращайтесь в службу поддержки по телефонам +7"
        + " (495) 785-17-03\\n8 800 700-29-03\"}]},\"barcode\":{\"format\""
        + ":\"PKBarcodeFormatQR\",\"message\":\"WMLFXCF\",\""
        + "messageEncoding\":\"UTF-8\",\"altText\":\"\"}}";
    private static final String PASS_STRINGS =
        "\"\\\"hall\\\" = \\\"ЗАЛ\\\";\\n"
        + "\\\"row\\\" = \\\"РЯД\\\";\\n"
        + "\\\"seats\\\" = \\\"МЕСТА\\\";\\n"
        + "\\\"ticket_num\\\" = \\\"НОМЕР БИЛЕТА\\\";\\n"
        + "\\\"price\\\" = \\\"ЦЕНА\\\";\\n"
        + "\\\"scan_instructions\\\" = \\\"ИНСТРУКЦИЯ ПО ИСПОЛЬЗОВАНИЮ\\\";\\n"
        + "\\\"ticket_refund\\\" = \\\"ВОЗВРАТ БИЛЕТА\\\";\\n"
        + "\\\"complaints\\\" = \\\"В СЛУЧАЕ ВОЗНИКНОВЕНИЯ ВОПРОСОВ\\\";\\n"
        + "\\\"complaints_value\\\" = "
        + "\\\"8 800 555 5176\\\\ntickets@support.yandex.ru\\\";\\n"
        + "\\\"venue_name\\\" = \\\"КИНОМАКС-МОЗАИКА, МОСКВА\\\";\\n"
        + "\\\"event_name\\\" = \\\"Алиса в Зазеркалье\\\";\\n"
        + "\\\"start_date\\\" = \\\"28 MAY\\\";\\n"
        + "\\\"scan_instructions_value\\\" = \\\"Просканируйте QR-код билета в"
        + " специальном терминале у входа в зал кинотеатра.\\\";\\n"
        + "\\\"ticket_refund_value\\\" = \\\"Для возврата билета свяжитесь с "
        + "нашей службой поддержки либо перейдите по ссылке "
        + "https://ya.cc/t/R8c7ofpA0B5Ve\\\";\\n"
        + "\\\"ticket_refund_attributed_value\\\" = \\\"Для возврата билета "
        + "свяжитесь с нашей службой поддержки либо перейдите по ссылке "
        + "https://ya.cc/t/R8c7ofpA0B5Ve\\\";\"";
    private static final String PASS_STRINGS_DATA =
        "{\"formatVersion\":1,\"passTypeIdentifier\":\"pass.ru.yandex.mobile."
        + "tickets\",\"description\":\"Алиса в Зазеркалье\",\"teamIdentifier\""
        + ":\"477EAT77S3\",\"organizationName\":\"Yandex, LLC\",\"serialNumber"
        + "\":\"1464445769177@6f5e06f0-aa2e-431f-90fc-aa8645f836c5\",\""
        + "foregroundColor\":\"#000000\",\"backgroundColor\":\"#f6f5f3\",\""
        + "labelColor\":\"#666666\",\"barcode\":{\"format\":\""
        + "PKBarcodeFormatQR\",\"message\":\"205801668448\",\"messageEncoding"
        + "\":\"iso-8859-1\"},\"suppressStripShine\":true,\"relevantDate\":\""
        + "2016-05-28T22:15+03:00\",\"ignoresTimeZone\":false,\"eventTicket\":"
        + "{\"headerFields\":[{\"__type\":\"textField\",\"key\":\"start-date\""
        + ",\"label\":\"start_date\",\"value\":\"22:15\"}],\"primaryFields\":["
        + "],\"secondaryFields\":[{\"__type\":\"textField\",\"key\":\""
        + "event-venue\",\"label\":\"venue_name\",\"value\":\"event_name\"}],"
        + "\"auxiliaryFields\":[{\"__type\":\"textField\",\"key\":\"row\",\""
        + "label\":\"row\",\"value\":\"10\"},{\"__type\":\"textField\",\"key\""
        + ":\"seats\",\"label\":\"seats\",\"value\":\"15, 16, 17\"},{\"__type"
        + "\":\"textField\",\"key\":\"hall\",\"label\":\"hall\",\"value\":\""
        + "Зал IMAX\"}],\"backFields\":[{\"__type\":\"textField\",\"key\":\""
        + "scan-instructions\",\"label\":\"scan_instructions\",\"value\":\""
        + "scan_instructions_value\"},{\"__type\":\"textField\",\"key\":\""
        + "ticket-refund\",\"label\":\"ticket_refund\",\"attributedValue\":\""
        + "ticket_refund_attributed_value\",\"value\":\"ticket_refund_value\"}"
        + ",{\"__type\":\"textField\",\"key\":\"complaints\",\"label\":\""
        + "complaints\",\"value\":\"complaints_value\"},{\"__type\":\""
        + "textField\",\"key\":\"ticket-num\",\"label\":\"ticket_num\",\""
        + "dataDetectorTypes\":[],\"value\":\"205801668448\"},{\"__type\":\""
        + "floatField\",\"key\":\"price\",\"label\":\"price\",\"value\":2250.0"
        + ",\"numberStyle\":\"PKNumberStyleDecimal\",\"currencyCode\":\"RUB\"}"
        + "]},\"pass.strings\":{\"en.lproj\":" + PASS_STRINGS
        + ",\"ru.lproj\":" + PASS_STRINGS.replace("MAY", "МАЯ") + "}}";

    @Test
    public void testPkpassUtf16BeBom() throws Exception {
        Json json = new Json(
            PKPASS,
            DATA,
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + PKPASS));
        DiskHandlerTest.testJson("utf-16be-bom.pkpass", json);
    }

    @Test
    public void testPkpassUtf16Be() throws Exception {
        Json json = new Json(
            PKPASS,
            DATA,
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + PKPASS));
        DiskHandlerTest.testJson("utf-16be.pkpass", json);
    }

    @Test
    public void testPassStrings() throws Exception {
        Json json = new Json(
            PKPASS,
            PASS_STRINGS_DATA,
            true,
            null,
            new Json.Contains(CONTENT_TYPE_META + PKPASS));
        DiskHandlerTest.testJson("pass.strings.pkpass", json);
    }
}

