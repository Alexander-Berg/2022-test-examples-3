package ru.yandex.market.logshatter.parser.delivery;

import java.util.Date;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logshatter.parser.LogParserChecker;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 29/10/15
 */
public class DeliveryEventResourceLogParserTest {

    @Test
    public void testParse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new DeliveryEventResourceLogParser());
        checker.check(
            "2015-10-22\t1445514376\t5605\tрегистрация\tpromo_request.created",
            new Date(1445514376000L), 5605, "регистрация", "promo_request.created", new Date(0)
        );

        checker.check(
            "2015-10-22\t1445515641\t5608\tрегистрация\tpromo_request.created\t2014-05-21",
            new Date(1445515641000L), 5608, "регистрация", "promo_request.created", new Date(1400616000000L)
        );
    }

    @Test
    public void testNoneDateParsing() throws Exception {
        LogParserChecker checker = new LogParserChecker(new DeliveryEventResourceLogParser());

        checker.check(
            "2017-07-07 07:26:17\t1499401577\t3314\tзаказ отправлен\torder.shipped\t0000-00-00",
            new Date(1499401577000L), 3314, "заказ отправлен", "order.shipped", new Date(0)
        );
    }
}
