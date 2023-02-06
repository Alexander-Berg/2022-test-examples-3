package ru.yandex.market.checkout.checkouter.order;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.date.DateUtil.convertToDate;
import static ru.yandex.market.checkout.checkouter.order.ControllerUtils.getStatusUpdateFromDate;
import static ru.yandex.market.checkout.checkouter.order.ControllerUtils.getStatusUpdateToDate;

public class CheckouterControllerUnitTest {

    @Test
    public void toStatusDateTest() {
        assertEquals(convertToDate("2015-10-18 00:00:00"), getStatusUpdateToDate(
                convertToDate("2015-10-18 00:00:00"),
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-18 15:30:30"), getStatusUpdateToDate(
                convertToDate("2015-10-19 00:00:00"),
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-18 15:30:30"), getStatusUpdateToDate(null,
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-17 00:00:00"), getStatusUpdateToDate(
                convertToDate("2015-10-17 00:00:00"), null));
        assertEquals(null, getStatusUpdateToDate(null, null));
    }


    @Test
    public void statusFromDateTest() {
        assertEquals(convertToDate("2015-10-18 15:30:30"), getStatusUpdateFromDate(
                convertToDate("2015-10-18 00:00:00"),
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-19 00:00:00"), getStatusUpdateFromDate(
                convertToDate("2015-10-19 00:00:00"),
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-18 15:30:30"), getStatusUpdateFromDate(null,
                convertToDate("2015-10-18 15:30:30").getTime()));
        assertEquals(convertToDate("2015-10-17 00:00:00"),
                getStatusUpdateFromDate(convertToDate("2015-10-17 00:00:00"), null));
        assertEquals(null, getStatusUpdateFromDate(null, null));
    }


}
