package ru.yandex.market.logshatter.parser.marketindexer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import ru.yandex.market.logshatter.parser.LogParserChecker;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * @author Dmitry Senin <a href="mailto:kairos@yandex-team.ru"></a>
 * @date 18/04/2018
 */
public class NginxTskvClickParserTest {

    LogParserChecker checker = new LogParserChecker(new NginxTskvClickParser());
    URL resource = getClass().getClassLoader().getResource("market-indexer-nginx-tskv.log");
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss Z");

    @Test
    public void parse() throws Exception {
        List<String> lines = FileUtils.readLines(new File(resource.toURI()));

        String api_line = lines.get(0);
        String cart_line = lines.get(1);
        String ping_line = lines.get(2);
        String checkout_line = lines.get(3);
        String routes_line = lines.get(4);
        String blue_cart_line = lines.get(5);
        String blue_checkout_line = lines.get(6);
        String checkouter_actualize_line = lines.get(7);
        String checkout_actualize_line = lines.get(8);

        checker.checkEmpty(api_line);
        checker.checkEmpty(cart_line);
        checker.checkEmpty(ping_line);
        checker.checkEmpty(checkout_line);
        checker.checkEmpty(routes_line);
        checker.checkEmpty(blue_cart_line);
        checker.checkEmpty(blue_checkout_line);

        checker.check(
            checkouter_actualize_line,
            dateFormat.parse("2018-04-28T00:25:04 +0300"),
            checker.getHost(),
            "NGINX",
            "CHECKOUTER",
            "MARKET",
            0,
            0,
            0,
            0,
            false,
            0,
            "UNKNOWN",
            1
        );

        checker.check(
            checkout_actualize_line,
            dateFormat.parse("2018-04-28T00:30:48 +0300"),
            checker.getHost(),
            "NGINX",
            "CHECKOUT",
            "MARKET",
            0,
            0,
            0,
            0,
            false,
            0,
            "UNKNOWN",
            1
        );
    }
}
