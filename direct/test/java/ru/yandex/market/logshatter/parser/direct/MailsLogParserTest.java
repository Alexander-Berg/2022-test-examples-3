package ru.yandex.market.logshatter.parser.direct;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static java.util.Collections.singletonList;

public class MailsLogParserTest {

    private LogParserChecker checker = new LogParserChecker(new MailsLogParser());
    private SimpleDateFormat dateFormat = new SimpleDateFormat(MailsLogParser.DATE_PATTERN);

    @Test
    public void testParse() throws Exception {
        String jsonLine =
            "{\"service\":\"direct.intapi\"," +
                "\"method\":\"Notification\"," +
                "\"log_time\":\"2019-10-31 17:40:38\"," +
                "\"data\":{" +
                    "\"subject\":\"Яндекс.Директ subject\"," +
                    "\"template_name\":\"notify_order_money_in\"," +
                    "\"email\":\"at-intapi-test@yandex.ru\"," +
                    "\"content\":\"To: at-intapi-test@yandex.ru\\ncontent\\n\\n\"," +
                    "\"client_id\":\"78436284\"" +
                "}," +
                "\"log_hostname\":\"direct-ts-1-2.man.yp-c.yandex.net\"," +
                "\"uid\":0," +
                "\"log_type\":\"mails\"," +
                "\"reqid\":\"1234390362965273815\"}";

        Date date = dateFormat.parse("2019-10-31 17:40:38");
        Object[] expect = new Object[]{
            "direct-ts-1-2.man.yp-c.yandex.net",
            "direct.intapi",
            "Notification",
            1234390362965273815L,

            "at-intapi-test@yandex.ru",
            "notify_order_money_in",
            "Яндекс.Директ subject",
            "To: at-intapi-test@yandex.ru\ncontent\n\n",
            78436284L
        };

        checker.check(jsonLine,
            singletonList(date),
            singletonList(expect)
        );
    }

    @Test
    public void testArrayParse() throws Exception {
        String jsonLine =
            "{\"service\":\"direct.intapi\"," +
                "\"method\":\"Notification\"," +
                "\"log_time\":\"2019-10-31 17:40:38\"," +
                "\"data\":[{" +
                "\"subject\":\"Яндекс.Директ subject\"," +
                "\"template_name\":\"notify_order_money_in\"," +
                "\"email\":\"at-intapi-test@yandex.ru\"," +
                "\"content\":\"To: at-intapi-test@yandex.ru\\ncontent\\n\\n\"," +
                "\"client_id\":\"78436284\"" +
                "}]," +
                "\"log_hostname\":\"direct-ts-1-2.man.yp-c.yandex.net\"," +
                "\"uid\":0," +
                "\"log_type\":\"mails\"," +
                "\"reqid\":\"1234390362965273815\"}";

        Date date = dateFormat.parse("2019-10-31 17:40:38");
        Object[] expect = new Object[]{
            "direct-ts-1-2.man.yp-c.yandex.net",
            "direct.intapi",
            "Notification",
            1234390362965273815L,

            "at-intapi-test@yandex.ru",
            "notify_order_money_in",
            "Яндекс.Директ subject",
            "To: at-intapi-test@yandex.ru\ncontent\n\n",
            78436284L
        };

        checker.check(jsonLine,
            singletonList(date),
            singletonList(expect)
        );
    }
}
