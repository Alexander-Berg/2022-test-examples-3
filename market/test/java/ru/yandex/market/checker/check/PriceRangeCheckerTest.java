package ru.yandex.market.checker.check;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.TestHelper;
import ru.yandex.market.checker.check.model.Checker;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author artemmz
 * @date 26/06/19.
 */
class PriceRangeCheckerTest extends EscapedSymbolsCheckerTest {
    private static final int PRICE = 1024;
    private static final String HTML = "<!DOCTYPE html><html xml:lang=\"ru\" lang=\"ru\">" +
            "<meta http-equiv=\"Content-Type\"" +
            " content=\"text/html; charset=UTF-8\"/><body><p>blah blah bla</p><p>1023</p></body></html>";

    @Autowired
    private PriceRangeChecker priceRangeChecker;

    @Override
    Checker checker() {
        return priceRangeChecker;
    }

    @Test
    void check() {
        for (int i = (int) ((1 - PriceRangeChecker.DISPERSION) * PRICE + 1); i < (int) ((1 + PriceRangeChecker.DISPERSION) * PRICE); i++) {
            String priceToFind = String.valueOf(i);
            var task = TestHelper.createTask("yandex.ru");
            assertTrue(priceRangeChecker.doCheck(task, HTML.getBytes(), null, priceToFind).isResult());
        }
    }
}
