package ru.yandex.market.logshatter.parser.checkout;

import java.util.Date;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logshatter.parser.LogParserChecker;

public class PushApiLogParserTest {
    private LogParserChecker logParserChecker;

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(
                "tskv\tshopId=318851\tuser_id=1152921504647666731\tsuccess=1\tsandbox=0\trequest=/cart\turl=http" +
                    "://shopadmin-stub.market.http.yandex" +
                    ".net:33486/318851/cart\tresponseTime=148\teventtime=1508856192882\thost=gravicapa01h.market" +
                    ".yandex.net\trequestMethod=POST\tpartnerInterface=true\tcontext=MARKET\tapiSettings=PRODUCTION" +
                    "\trequestId=1508856192468/370c599be2479da659ef3d32f87b70ca/3/4\n",
                new Date(1508856192882L),
                new Object[]{
                    // shopId
                    318851L,
                    // userId
                    1152921504647666731L,
                    //success
                    true,
                    // sandbox
                    false,
                    // request
                    "/cart",
                    //url
                    "http://shopadmin-stub.market.http.yandex.net:33486/318851/cart",
                    //args
                    "",
                    //responseTime
                    148,
                    "",
                    "",
                    "",
                    "POST",
                    true,
                    "MARKET",
                    "PRODUCTION",
                    "",
                    0L,
                    "1508856192468/370c599be2479da659ef3d32f87b70ca/3/4"
                }
            ),
            Arguments.of(
                "tskv\tshopId=291490\tuser_id=76856312\tsuccess=1\tsandbox=0\trequest=/cart\turl=https://order" +
                    ".corpcentre.ru/ya/21319927/cart\targs=\tresponseTime=361\teventtime=1508856197744\thost" +
                    "=gravicapa01h.market.yandex.net\trequestMethod=POST\tpartnerInterface=false\tcontext=MARKET" +
                    "\tapiSettings=PRODUCTION\trequestId=1508856197268/eca540ce0a4c9e2329116082b46bf3c1/2/3\n",
                new Date(1508856197744L),
                new Object[]{
                    // shopId
                    291490L,
                    // userId
                    76856312L,
                    //success
                    true,
                    // sandbox
                    false,
                    // request
                    "/cart",
                    //url
                    "https://order.corpcentre.ru/ya/21319927/cart",
                    //args
                    "",
                    //responseTime
                    361,
                    "",
                    "",
                    "",
                    "POST",
                    false,
                    "MARKET",
                    "PRODUCTION",
                    "",
                    0L,
                    "1508856197268/eca540ce0a4c9e2329116082b46bf3c1/2/3"
                }
            )
        );
    }

    @BeforeEach
    public void setUp() {
        logParserChecker = new LogParserChecker(new PushApiLogParser());
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void shouldParse(String line, Date timestamp, Object[] result) throws Exception {
        logParserChecker.check(line, timestamp, result);
    }
}
