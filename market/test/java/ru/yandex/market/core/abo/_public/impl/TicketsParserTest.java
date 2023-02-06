package ru.yandex.market.core.abo._public.impl;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.xml.sax.SAXException;

import ru.yandex.market.core.AbstractParserTest;
import ru.yandex.market.core.abo._public.Ticket;

import static ru.yandex.market.core.abo._public.Ticket.Status;

/**
 * @author zoom
 */
public class TicketsParserTest extends AbstractParserTest {

    private static final String ORDER_ID_421 = "421";

    private final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MILLI_OF_SECOND, 1)
            .toFormatter();

    private final DateTimeFormatter DATE_TIME_MIN_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('.')
            .appendValue(ChronoField.YEAR, 4)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .toFormatter();

    @Test
    public void shouldParseWell() throws IOException, SAXException {
        try (InputStream in = getContentStream("OK-result.xml")) {
            List<Ticket> actualTickets = new TicketsParser("/data/abuse-info/ticket").parseStream(in);
            assertEquals(2, actualTickets.size());

            Ticket expectedTicket = new Ticket();

            expectedTicket.setShopId(155);
            expectedTicket.setId(2799092);
            expectedTicket.setErrorCode(2);
            expectedTicket.setStatus(Status.valueOf(4));
            expectedTicket.setErrorText("цена на Маркете отличается от цены на сайте магазина при пересчете валют");
            expectedTicket.setErrorFoundTime(LocalDateTime.parse("2016-01-30 10:25:38.0", DATE_TIME_MILLIS_FORMATTER));
            expectedTicket.setFeedTime(LocalDateTime.parse("05.12.2013 16:51", DATE_TIME_MIN_FORMATTER));
            expectedTicket.setCheckMethod(null);
            expectedTicket.setOfferURL("https://ссылка_на_магазин1.ru");

            assertEquals(expectedTicket, actualTickets.get(0));

            expectedTicket.setShopId(156);
            expectedTicket.setId(2799091);
            expectedTicket.setErrorCode(17);
            expectedTicket.setStatus(Status.valueOf(2));
            expectedTicket.setErrorText("по указанным контактам не удается связаться с магазином");
            expectedTicket.setErrorFoundTime(LocalDateTime.parse("2016-01-28 04:51:49.0", DATE_TIME_MILLIS_FORMATTER));
            expectedTicket.setFeedTime(null);
            expectedTicket.setCheckMethod(null);
            expectedTicket.setOfferURL("http://ссылка_на_магазин.ru");

            assertEquals(expectedTicket, actualTickets.get(1));


        }
    }

    @Test
    public void testParseArchiveTickets() throws Exception {
        try (InputStream in = getContentStream("OK-archive-result.xml")) {
            List<Ticket> acutalTickets = new TicketsParser("/data/history-abuse-info/ticket").parseStream(in);

            Ticket expectedTicket1 = new Ticket();
            expectedTicket1.setShopId(155);
            expectedTicket1.setId(2799090);
            expectedTicket1.setErrorCode(54);
            expectedTicket1.setStatus(Status.valueOf(4));
            expectedTicket1.setErrorText("публикация одинаковых предложений, в том числе отличающихся написанием (тиражирование)");
            expectedTicket1.setErrorFoundTime(LocalDateTime.parse("2016-01-23 18:15:42.0", DATE_TIME_MILLIS_FORMATTER));
            expectedTicket1.setFeedTime(null);
            expectedTicket1.setCheckMethod(Ticket.CheckMethod.BY_PHONE);
            expectedTicket1.setOfferURL("https://ссылка_на_магазин.ru");
            expectedTicket1.setOrderId(ORDER_ID_421);

            assertEquals(expectedTicket1, acutalTickets.get(0));

            Ticket expectedTicket2 = new Ticket();
            expectedTicket2.setShopId(155);
            expectedTicket2.setId(2799079);
            expectedTicket2.setErrorCode(111);
            expectedTicket2.setStatus(Status.valueOf(4));
            expectedTicket2.setErrorText("неверно указаны цена и сроки доставки");
            expectedTicket2.setErrorFoundTime(LocalDateTime.parse("2016-01-14 18:46:46.0", DATE_TIME_MILLIS_FORMATTER));
            expectedTicket2.setFeedTime(LocalDateTime.parse("05.12.2013 18:51", DATE_TIME_MIN_FORMATTER));
            expectedTicket2.setCheckMethod(null);
            expectedTicket2.setOfferURL("http://ссылка_на_магазин1.ru");

            assertEquals(expectedTicket2, acutalTickets.get(1));

        }
    }

    @Test
    @DisplayName("Неверный формат check-method. Ожидается исключение")
    public void testBadCheckMethodFormat() {
        try (InputStream in = getContentStream("ERROR-check-method.xml")) {
            new TicketsParser("/data/history-abuse-info/ticket").parseStream(in);
            Assert.fail();
        } catch (Exception ignore) {
            // ok
        }
    }

}
