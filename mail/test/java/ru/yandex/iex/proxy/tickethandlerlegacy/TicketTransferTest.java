package ru.yandex.iex.proxy.tickethandlerlegacy;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class TicketTransferTest extends TestBase {
    @Test
    public void testAeroexpressDeparture() {
        String city = "МОСКВА";
        String time = "2018-07-10T01:55:00+03:00";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, true);
        Assert.assertEquals(TicketTransfer.AEROEXPRESS, result);
    }

    @Test
    public void testAeroexpressArrival() {
        String city = "Moscow";
        String time = "0.0.0 5:2:0";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, false);
        Assert.assertEquals(TicketTransfer.AEROEXPRESS, result);
    }

    @Test
    public void testTimeWithoutDate() {
        String city = "moscow";
        String time = "00:00:00";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, true);
        Assert.assertEquals(TicketTransfer.AEROEXPRESS, result);
    }

    @Test
    public void testNotAeroexpress() {
        String city = "Москва";
        String time = "2018.07.10 03:55:00+03:00";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, true);
        Assert.assertEquals(TicketTransfer.YTAXI, result);
    }

    @Test
    public void testYtaxiRu() {
        String city = "Нижний Новгород";
        String time = "";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, true);
        Assert.assertEquals(TicketTransfer.YTAXI, result);
    }

    @Test
    public void testYtaxiEn() {
        String city = "Rostov-on-Don";
        String time = "2018-01-01 1:1:1";
        String result =
            TicketTransfer.getInstance().getTransferType(city, time, true);
        Assert.assertEquals(TicketTransfer.YTAXI, result);
    }

    @Test
    public void testTaxi() {
        String city = "Oslo";
        String result =
            TicketTransfer.getInstance().getTransferType(city, null, true);
        Assert.assertEquals(TicketTransfer.TAXI, result);
    }

    @Test
    public void testYtaxiEkaterinburg() {
        String city = "Ekaterinburg";
        String result =
            TicketTransfer.getInstance().getTransferType(city, null, true);
        Assert.assertEquals(TicketTransfer.YTAXI, result);

        city = "Yekaterinburg";
        result = TicketTransfer.getInstance().getTransferType(city, null, true);
        Assert.assertEquals(TicketTransfer.YTAXI, result);
    }
}
