package ru.yandex.market.logshatter.parser.checkout;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author kukabara
 */
public class CheckoutEventLogParserTest {
    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutEventLogParser());
        checker.check(
            "tskv\tevent_time=18/Jan/2016:14:35:40 +0300\ttimestamp=1453116940253\tevent_type=ORDER_CREATED" +
                "\torder_id=504069\tshop_id=720\tuid=1152921504608314878\tuser_group=DEFAULT\tno_auth=1" +
                "\tfake=0\taccept_method=WEB_INTERFACE" +
                "\tmeta={\"userId\":\"4fa95784bc0a8ed434265b60b443bb2b\",\"counter\":\"657\"," +
                "\"platform\":\"ANDROID\",\"deviceType\":\"SMARTPHONE\"}" +
                "\tcontext=MARKET \temail=nushi808080@mail.ru \tphone=79295874997 \ttotalprice=808.70 \titemcount=1 " +
                "\tsubsidy=10.5 \tgps_lat=10.1 \tgps_lon=10.1",
            new Date(1453116940253L), checker.getHost(), 504069L, 720L, 1152921504608314878L,
            "DEFAULT", true, false, "WEB_INTERFACE", "4fa95784bc0a8ed434265b60b443bb2b", "657", "ANDROID",
            "SMARTPHONE", "",
            "{\"userId\":\"4fa95784bc0a8ed434265b60b443bb2b\",\"counter\":\"657\",\"platform\":\"ANDROID\"," +
                "\"deviceType\":\"SMARTPHONE\"}",
            "nushi808080@mail.ru", "79295874997", 808, 1, 10, 10.1, 10.1, "");

        checker.check(
            "tskv\tevent_time=18/Jan/2016:15:23:49 +0300\ttimestamp=1453119829179\tevent_type=ORDER_CREATED" +
                "\torder_id=504070\tshop_id=704\tuid=307700624\tuser_group=DEFAULT\tno_auth=0\tfake=0\taccept_method" +
                "=PUSH_API" +
                "\tmeta=null\tcontext=MARKET \temail=nushi808080@mail.ru \tphone=79295874997 " +
                "\ttotalprice=808.70 \titemcount=1 \tsubsidy=0 \tgps_lat=10.1 \tgps_lon=10.2",
            new Date(1453119829179L), checker.getHost(), 504070L, 704L, 307700624L,
            "DEFAULT", false, false, "PUSH_API", "", "", "", "", "", "null",
            "nushi808080@mail.ru", "79295874997", 808, 1, 0, 10.1, 10.2, "");

        checker.check(
            "tskv\tevent_time=18/Jan/2016:14:30:04 +0300\ttimestamp=1453116604409\tevent_Type=ORDER_CREATED" +
                "\torder_id=504067\tshop_id=123082\tuid=1152921504608316261\tuser_group=DEFAULT\tno_auth=1\tfake=0" +
                "\taccept_method=PUSH_API" +
                "\tmeta={\"counter\":\"touch\",\"platform\":\"Android\",\"browser\":\"YandexBrowser\"," +
                "\"deviceType\":\"smartphone\"}" +
                " \temail=nushi808080@mail.ru \tphone=79295874997 \ttotalprice=808.70 \titemcount=1 \tsubsidy=10.5" +
                " \tgps_lat=10.1 \tgps_lon=10.1",
            new Date(1453116604409L), checker.getHost(), 504067L, 123082L, 1152921504608316261L,
            "DEFAULT", true, false, "PUSH_API", "", "touch", "ANDROID", "SMARTPHONE", "YandexBrowser",
            "{\"counter\":\"touch\",\"platform\":\"Android\",\"browser\":\"YandexBrowser\"," +
                "\"deviceType\":\"smartphone\"}",
            "nushi808080@mail.ru", "79295874997", 808, 1, 10, 10.1, 10.1, "");

        checker.check(
            "tskv\tevent_time=16/Mar/2017:17:17:21 " +
                "+0300\ttimestamp=1489673841590\tevent_type=ORDER_CREATED\torder_id=1922095\tshop_id=405287\tuid" +
                "=207959744\tuser_group=ABO\tno_auth=0\tfake=1\taccept_method=WEB_INTERFACE\tcontext=CHECK_ORDER " +
                "\temail=nushi808080@mail.ru \tphone=79295874997 \ttotalprice=808.70 \titemcount=1 \tsubsidy=10.5 " +
                " \tgps_lat=10.1 \tgps_lon=10.1\n",
            new Date(1489673841590L),
            checker.getHost(),
            1922095L,
            405287L,
            207959744L,
            "ABO",
            false,
            true,
            "WEB_INTERFACE",
            "", "", "", "", "", "",
            "nushi808080@mail.ru", "79295874997", 808, 1, 10, 10.1, 10.1, ""

        );

        checker.check("tskv\tevent_time=10/Jul/2017:09:31:29 " +
                "+0300\ttimestamp=1499668289286\tevent_type=ORDER_CREATED\torder_id=2454983\tshop_id=413136\tuid" +
                "=1152921504634401006\tuser_group=DEFAULT\tno_auth=1\tfake=0\taccept_method=WEB_INTERFACE\tmeta" +
                "={\\\"counter\\\":\\\"touch\\\",\\\"platform\\\":\\\"iOS\\\",\\\"browser\\\":\\\"MobileSafari\\\"," +
                "\\\"deviceType\\\":\\\"smartphone\\\"}\tcontext=MARKET \temail=nushi808080@mail.ru " +
                "\tphone=79295874997 \ttotalprice=808.70 \titemcount=1 \tsubsidy=10.5",
            new Date(1499668289286L),
            checker.getHost(),
            2454983L,
            413136L,
            1152921504634401006L,
            "DEFAULT",
            true,
            false,
            "WEB_INTERFACE", "",
            "touch", "IOS", "SMARTPHONE", "MobileSafari",
            "{\"counter\":\"touch\",\"platform\":\"iOS\",\"browser\":\"MobileSafari\",\"deviceType\":\"smartphone\"}",
            "nushi808080@mail.ru", "79295874997", 808, 1, 10, 0d, 0d, "");

        // with personal_phone_id
        checker.check("tskv\tevent_time=10/Jul/2017:09:31:29 " +
                "+0300\ttimestamp=1499668289286\tevent_type=ORDER_CREATED\torder_id=2454983\tshop_id=413136\tuid" +
                "=1152921504634401006\tuser_group=DEFAULT\tno_auth=1\tfake=0\taccept_method=WEB_INTERFACE\tmeta" +
                "={\\\"counter\\\":\\\"touch\\\",\\\"platform\\\":\\\"iOS\\\",\\\"browser\\\":\\\"MobileSafari\\\"," +
                "\\\"deviceType\\\":\\\"smartphone\\\"}\tcontext=MARKET \temail=nushi808080@mail.ru " +
                "\tphone=79295874997 \ttotalprice=808.70 \titemcount=1 \tsubsidy=10.5 " +
                "\tpersonal_phone_id=0123456789abcdef0123456789abcdef",
            new Date(1499668289286L),
            checker.getHost(),
            2454983L,
            413136L,
            1152921504634401006L,
            "DEFAULT",
            true,
            false,
            "WEB_INTERFACE", "",
            "touch", "IOS", "SMARTPHONE", "MobileSafari",
            "{\"counter\":\"touch\",\"platform\":\"iOS\",\"browser\":\"MobileSafari\",\"deviceType\":\"smartphone\"}",
            "nushi808080@mail.ru", "79295874997", 808, 1, 10, 0d, 0d, "0123456789abcdef0123456789abcdef");

        // with personal_phone_id, without phone
        checker.check("tskv\tevent_time=10/Jul/2017:09:31:29 " +
                "+0300\ttimestamp=1499668289286\tevent_type=ORDER_CREATED\torder_id=2454983\tshop_id=413136\tuid" +
                "=1152921504634401006\tuser_group=DEFAULT\tno_auth=1\tfake=0\taccept_method=WEB_INTERFACE\tmeta" +
                "={\\\"counter\\\":\\\"touch\\\",\\\"platform\\\":\\\"iOS\\\",\\\"browser\\\":\\\"MobileSafari\\\"," +
                "\\\"deviceType\\\":\\\"smartphone\\\"}\tcontext=MARKET \temail=nushi808080@mail.ru " +
                "\ttotalprice=808.70 \titemcount=1 \tsubsidy=10.5 \tpersonal_phone_id=0123456789abcdef0123456789abcdef",
            new Date(1499668289286L),
            checker.getHost(),
            2454983L,
            413136L,
            1152921504634401006L,
            "DEFAULT",
            true,
            false,
            "WEB_INTERFACE", "",
            "touch", "IOS", "SMARTPHONE", "MobileSafari",
            "{\"counter\":\"touch\",\"platform\":\"iOS\",\"browser\":\"MobileSafari\",\"deviceType\":\"smartphone\"}",
            "nushi808080@mail.ru", "", 808, 1, 10, 0d, 0d, "0123456789abcdef0123456789abcdef");
    }
}
