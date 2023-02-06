package ru.yandex.market.logshatter.parser.checkout.difflogs;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CheckoutDiffLogBusinessParserTest {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");
    private final LogParserChecker checker = new LogParserChecker(new CheckoutDiffLogBusinessParser());

    @Test
    public void testBusinessLine() throws Exception {
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json-business.log");
        assertNotNull(resource);

        String line = FileUtils.readLines(new File(resource.toURI())).get(0).toString();
        checker.check(
            line,
            dateFormat.parse("2017-03-23 08:57:09,633"),
            "hostname.test", //host
            "1594210808935/c001a685310041bca7a3b4740c1150aa/14/11", //marketRequestId
            "CART_DIFF", //logType
            "ITEM_PRICE", //event
            "REPORT", //additionalLoggingInfo.changeReason
            "CHECK_ORDER", //cart.context
            207959744L, //cart.buyer.uid
            96L, //cart.buyer.regionId
            123456789L, //cart.buyer.businessBalanceId
            "Чехлы для планшетов Untamo Чехол UIPAD2WH для iPad 2 (белый)", //item.offerName
            12345L, //item.msku
            "54321", //item.shopSku
            1, //item.count
            0, //additionalLoggingInfo.actualCartItemCount
            false, //cart.global
            10214454L, //cart.shopId
            123, //item.warehouseId
            false//item.atSupplierWarehouse
        );
    }

    @Test
    public void testPersonLine() throws Exception {
        URL resource = getClass().getClassLoader().getResource("market-checkouter-cart-diff-json-business.log");
        assertNotNull(resource);

        String line = FileUtils.readLines(new File(resource.toURI())).get(1).toString();
        checker.checkEmpty(line);
    }
}
