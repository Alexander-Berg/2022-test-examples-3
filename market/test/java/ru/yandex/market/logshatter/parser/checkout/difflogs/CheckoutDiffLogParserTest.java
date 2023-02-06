package ru.yandex.market.logshatter.parser.checkout.difflogs;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;


/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 * date: 23/03/2017
 */
public class CheckoutDiffLogParserTest {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    @Test
    public void parse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogParser());
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        checker.check(
            line,
            dateFormat.parse("2017-03-23 08:57:09,633"),
            "hostname.test",
            "1594210808935/c001a685310041bca7a3b4740c1150aa/14/11",
            "CART_DIFF",
            "ITEM_PRICE",
            "REPORT",
            "CHECK_ORDER",
            1390D,
            250D,
            1,
            -1,
            false,
            10214454L,
            "",
            1,
            false,
            207959744L
        );
    }


    @Test
    public void parse2() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogParser());
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(7).toString();
        checker.check(
            line,
            dateFormat.parse("2017-03-23 11:22:09,482"),
            "hostname.test",
            "",
            "CART_DIFF",
            "ITEM_COUNT",
            "",
            "MARKET",
            7490D,
            -1D,
            2,
            1,
            false,
            10204985L,
            "",
            1,
            false,
            397586590L
        );
    }

    @Test
    public void parseRgb() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogParser());
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json-rgb.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        checker.check(
            line,
            dateFormat.parse("2018-02-16 10:33:25,549"),
            "hostname.test",
            "",
            "CART_DIFF",
            "ITEM_PRICE",
            "",
            "MARKET",
            420.0,
            600.0,
            1,
            -1,
            false,
            10247981L,
            "GREEN",
            1,
            false,
            466839847L
        );
    }

    @Test
    public void parseWarehouse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogParser());
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json-warehouse.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        checker.check(
            line,
            dateFormat.parse("2018-02-16 10:33:25,549"),
            "hostname.test",
            "",
            "CART_DIFF",
            "ITEM_PRICE",
            "",
            "MARKET",
            420.0,
            600.0,
            1,
            -1,
            false,
            10247981L,
            "BLUE",
            147,
            false,
            466839847L
        );
    }


    @Test
    public void parseAtSupplierWarehouse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogParser());
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json-at-supplier" +
            "-warehouse.log");
        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        checker.check(
            line,
            dateFormat.parse("2019-11-19 14:57:58,010"),
            "hostname.test",
            "1574164677766/a55a28d076aa677776ac6fc6b1970500/7",
            "CART_DIFF",
            "ITEM_DELIVERY",
            "",
            "MARKET",
            1890.0,
            -1.0,
            1,
            -1,
            false,
            431782L,
            "BLUE",
            47828,
            true,
            982325297L
        );
    }
}
