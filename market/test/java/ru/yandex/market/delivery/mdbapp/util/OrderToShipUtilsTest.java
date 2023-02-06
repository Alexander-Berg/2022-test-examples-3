package ru.yandex.market.delivery.mdbapp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import steps.utils.TestableClock;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.order.OrderItem;

public class OrderToShipUtilsTest {

    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.UTC;
    private static final Instant FIXED_TIME = LocalDate.of(2021, 6, 19)
        .atStartOfDay()
        .toInstant(DEFAULT_OFFSET);
    private static final LocalDateTime NOW_TIME_LOCAL_DATE_TIME = LocalDateTime.ofInstant(FIXED_TIME, DEFAULT_OFFSET);
    private static final LocalDateTime SHIPMENT_DATE_TIME = LocalDateTime.of(2020, 6, 30, 10, 0, 0);

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void beforeTest() {
        clock.setFixed(FIXED_TIME, ZoneId.systemDefault());
    }

    @Test
    public void getOrderToShipsParcelDatesNotEmptyItems() {
        var dates = OrderToShipUtils.getOrderToShipsParcelDates(getParcelWithoutDate(), getOrderItems(), true);
        softly.assertThat(dates.getShipmentDate()).isEqualTo(SHIPMENT_DATE_TIME.toLocalDate());

        softly.assertThat(dates.hasSupplierShipmentDate(123L)).isFalse();
        softly.assertThat(dates.hasFFInboundDate(123L)).isFalse();
    }

    @Test
    public void getOrderToShipsParcelDatesNotEmptyItemsAndDates() {
        var p = getParcelWithDate();

        var dates = OrderToShipUtils.getOrderToShipsParcelDates(p, getOrderItems(), true);
        softly.assertThat(dates.getShipmentDate()).isEqualTo(SHIPMENT_DATE_TIME.toLocalDate());

        softly.assertThat(dates.hasSupplierShipmentDate(123L)).isTrue();
        softly.assertThat(dates.hasFFInboundDate(123L)).isTrue();

        softly.assertThat(dates.getSupplierShipmentDate(123L))
            .isEqualTo(CheckouterDateUtils.instantToLocalDate(FIXED_TIME).get());

        softly.assertThat(dates.hasSupplierShipmentDate(234L)).isFalse();
        softly.assertThat(dates.hasFFInboundDate(234L)).isFalse();

        var dates2 = OrderToShipUtils.getOrderToShipsParcelDates(p, getOrderItems(), false);
        softly.assertThat(dates2.hasSupplierShipmentDate(345L)).isTrue();
        softly.assertThat(dates2.hasFFInboundDate(345L)).isTrue();
    }

    HashMap<Long, OrderItem> getOrderItems() {
        OrderItem item123 = new OrderItem();
        item123.setId(123L);
        item123.setAtSupplierWarehouse(true);

        OrderItem item23 = new OrderItem();
        item23.setId(23L);

        OrderItem item345 = new OrderItem();
        item345.setId(345L);

        HashMap<Long, OrderItem> map = new HashMap<Long, OrderItem>();
        map.put(123L, item123);
        map.put(23L, item23);
        map.put(345L, item345);
        return map;
    }

    Parcel getParcelWithDate() {
        Parcel p = new Parcel();
        p.setShipmentDate(SHIPMENT_DATE_TIME.toLocalDate());


        var pi1 = new ParcelItem();
        pi1.setItemId(123L);
        pi1.setShipmentDateTimeBySupplier(NOW_TIME_LOCAL_DATE_TIME);
        pi1.setSupplierShipmentDateTime(FIXED_TIME);

        var pi2 = new ParcelItem();
        pi2.setItemId(234L);

        var pi3 = new ParcelItem();
        pi3.setItemId(345L);
        pi3.setSupplierStartDateTime(SHIPMENT_DATE_TIME.toInstant(DEFAULT_OFFSET));

        pi3.setShipmentDateTimeBySupplier(NOW_TIME_LOCAL_DATE_TIME);
        pi3.setSupplierShipmentDateTime(FIXED_TIME);

        p.setParcelItems(List.of(pi1, pi2, pi3));
        return p;
    }

    Parcel getParcelWithoutDate() {
        Parcel p = new Parcel();
        p.setShipmentDate(SHIPMENT_DATE_TIME.toLocalDate());

        OrderItem item123 = new OrderItem();
        item123.setId(123L);
        OrderItem item234 = new OrderItem();
        item234.setId(234L);

        var pi1 = new ParcelItem();
        pi1.setItemId(123L);
        var pi2 = new ParcelItem();
        pi2.setItemId(234L);

        p.setParcelItems(List.of(pi1, pi2));
        return p;
    }
}
