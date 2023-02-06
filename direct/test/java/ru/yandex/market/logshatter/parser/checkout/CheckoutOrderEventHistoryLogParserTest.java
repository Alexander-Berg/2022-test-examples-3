package ru.yandex.market.logshatter.parser.checkout;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.logshatter.parser.LogParserChecker;
import ru.yandex.market.logshatter.parser.ParserContext;
import ru.yandex.market.logshatter.parser.checkout.events.CheckoutOrderEventHistoryLogParser;
import ru.yandex.market.logshatter.reader.file.BufferedFileLogReader;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;

/**
 * @author Nicolai Iusiumbeli <mailto:armor@yandex-team.ru>
 *         date: 10/03/2017
 */
public class CheckoutOrderEventHistoryLogParserTest {
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    @Test
    public void parse() throws Exception {
        URL resource = getClass().getClassLoader().getResource("order_events_history.log");
        BufferedFileLogReader reader = new BufferedFileLogReader(new File(resource.toURI()).toPath());
        CheckoutOrderEventHistoryLogParser parser = new CheckoutOrderEventHistoryLogParser();
        ParserContext context = Mockito.mock(ParserContext.class);

        Set<Long> originalEventIds = new HashSet<>();
        originalEventIds.addAll(Arrays.asList(
                36719191111111L, 3671920L, 3671921L, 3671922L, 3671923L, 3671924L, 3671925L,
                3671926L, 3671927L, 3671928L
        ));
        Set<Long> originalOrderIds = new HashSet<>();
        originalOrderIds.addAll(Arrays.asList(
                1135122L, 1135121L, 1135107L, 1135111L
        ));

        Set<Long> resultEventIds = new HashSet<>();
        Set<Long> resultOrderIds = new HashSet<>();
        Mockito.doAnswer(inv -> {
            resultEventIds.add((Long) inv.getArguments()[2]);
            resultOrderIds.add((Long) inv.getArguments()[4]);
            return null;
        }).when(context).write(any(Date.class), anyVararg());
        parser.parse(reader.readLine(), context);

        assertTrue(resultEventIds.equals(originalEventIds));
        assertTrue(resultOrderIds.equals(originalOrderIds));
    }

    @Test
    public void logCheckerTest() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history.log");

        Object[] result = getDefaut();

        checker.check(line, dateFormat.parse("10-03-2017 12:31:32"), result);

    }

    // Test для формата, где пишутся события по одному
    @Test
    public void logCheckerTestSingle() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single.log");
        Object[] expected = getDefaut();
        expected[30] = Arrays.asList(300);

        checker.check(
            line,
            dateFormat.parse("10-03-2017 12:31:32"),
            expected
        );
    }

    @Test
    public void logCheckerTestSingleWithSubstatus() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single_substatus.log");
        Object[] expected = getDefaut();
        expected[0] = "gravicapa02ht";
        expected[1] = 36719191111111L;
        expected[3] = 1135111L;
        expected[8] = "PROCESSING";
        expected[9] = "CANCELLED";
        expected[10] = expected[11] = "PICKUP";
        expected[12] = expected[13] = "YANDEX";
        expected[14] = "CHECK_ORDER";
        expected[17] = "USER_CHANGED_MIND";
        expected[18] = 10214453L;
        expected[19] = 213L;
        expected[23] = 207959744L;
        expected[24] = "ENplRt@mail.ru";
        expected[25] = "+78436415573";
        expected[29] = "SHOP";
        expected[30] = Arrays.asList(300, 900);

        checker.check(
            line,
            dateFormat.parse("10-03-2017 12:30:32"),
            expected
        );
    }

    @Test
    public void logCheckerTestSingleWithRgb() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single_rgb.log");
        Object[] expected = getDefaut();
        expected[1] = 157348309L;
        expected[3] = 2123821L;
        expected[10] = "PICKUP";
        expected[11] = "PICKUP";
        expected[12] = "CASH_ON_DELIVERY";
        expected[13] = "CASH_ON_DELIVERY";
        expected[18] = 10210930L;
        expected[19] = 54L;
        expected[20] = "GREEN";
        expected[23] = 12088758L;
        expected[24] = "Anton0xf@yandex.ru";
        expected[25] = "+79222087729";

        checker.check(
            line,
            dateFormat.parse("13-02-2018 07:23:14"),
            expected
        );
    }

    @Test
    public void logCheckerTestSingleWithPreorder() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single_preorder.log");
        Object[] expected = getDefaut();
        expected[0] = "gravicapa04e";
        expected[1] = 35565994L;
        expected[2] = "NEW_PAYMENT";
        expected[3] = 3820690L;
        expected[8] = expected[9] = "UNPAID";
        expected[10] = expected[11] = "DELIVERY";
        expected[12] = expected[13] = "YANDEX";
        expected[18] = 431782L;
        expected[19] = 213L;
        expected[20] = "BLUE";
        expected[21] = true;
        expected[22] = "100307940933";
        expected[23] = 15159639L;
        expected[24] = "Gelya23@yandex.ru";
        expected[25] = "79035146325";
        expected[26] = "DESKTOP";
        expected[27] = true;

        checker.check(
            line,
            dateFormat.parse("18-07-2018 16:19:17"),
            expected
        );
    }

    @Test
    public void logCheckerTestSingleWithNoFulfilment() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single_no_fulfilment.log");
        Object[] expected = getDefaut();
        expected[0] = "gravicapa04e";
        expected[1] = 35565994L;
        expected[2] = "NEW_PAYMENT";
        expected[3] = 3820690L;
        expected[8] = expected[9] = "UNPAID";
        expected[10] = expected[11] = "DELIVERY";
        expected[12] = expected[13] = "YANDEX";
        expected[18] = 431782L;
        expected[19] = 213L;
        expected[20] = "BLUE";
        expected[21] = false;
        expected[22] = "100307940933";
        expected[23] = 15159639L;
        expected[24] = "Gelya23@yandex.ru";
        expected[25] = "79035146325";
        expected[26] = "DESKTOP";
        expected[27] = false;

        checker.check(
            line,
            dateFormat.parse("18-07-2018 16:19:17"),
            expected
        );
    }

    @Test
    public void logCheckerTestSingleWithWarehouse() throws Exception {
        LogParserChecker checker = new LogParserChecker(new CheckoutOrderEventHistoryLogParser());
        String line = readLogFile("order_events_history_single_warehouse.log");
        Object[] expected = getDefaut();
        expected[0] = "man2-1218-e40-man-market-prod--74d-29608";
        expected[1] = 42612665L;
        expected[2] = "ORDER_STATUS_UPDATED";
        expected[3] = 4107725L;
        expected[8] = "DELIVERY";
        expected[9] = "DELIVERED";
        expected[10] = expected[11] = "DELIVERY";
        expected[12] = expected[13] = "YANDEX";
        expected[18] = 431782L;
        expected[19] = 193L;
        expected[20] = "BLUE";
        expected[21] = false;
        expected[22] = "171122479";
        expected[23] = 164697233L;
        expected[24] = "unstopah@yandex.ru";
        expected[25] = "79518753517";
        expected[26] = "ANDROID";
        expected[27] = true;
        expected[28] = 145;

        checker.check(
            line,
            dateFormat.parse("13-11-2018 18:52:50"),
            expected
        );
    }

    private Object[] getDefaut() {
        return new Object[] {
            "gravicapa01ht",
            3671928L,
            "ORDER_STATUS_UPDATED",
            1135122L,
            -1L,
            -1L,
            -1L,
            -1L,
            "RESERVED",
            "PROCESSING",
            "POST",
            "POST",
            "CARD_ON_DELIVERY",
            "CARD_ON_DELIVERY",
            "MARKET",
            false,
            "",
            "",
            242102L,
            2L,
            "",
            false,
            "",
            78288L,
            "jkt@yandex-team.ru",
            "+78121234567",
            "",
            false,
            1,
            "YANDEX_MARKET",
            new ArrayList<>()
        };
    }

    private static String readLogFile(String name) throws IOException, URISyntaxException {
        URL resource = CheckoutOrderEventHistoryLogParserTest.class.getClassLoader().getResource(name);
        return FileUtils.readLines(new File(resource.toURI())).get(0);
    }
}
