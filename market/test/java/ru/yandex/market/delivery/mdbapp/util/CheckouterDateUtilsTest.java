package ru.yandex.market.delivery.mdbapp.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.common.util.DateTimeUtils;

public class CheckouterDateUtilsTest {
    final LocalDateTime shipmentDateTime = LocalDateTime.of(2020, 6, 30, 10, 0, 0);
    final Instant shipmentTimestamp = shipmentDateTime.atZone(DateTimeUtils.MOSCOW_ZONE).toInstant();
    final LocalDate shipmentDate = shipmentDateTime.toLocalDate();
    final LocalDate anotherShipmentDate = LocalDate.of(2020, 6, 29);
    final Instant anotherShipmentDateTime = anotherShipmentDate.atStartOfDay(DateTimeUtils.MOSCOW_ZONE).toInstant();

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void testParcelNoShipmentDate() {
        Parcel p = new Parcel();
        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(p)).isEqualTo(Optional.empty());
    }

    @Test
    public void testParcelOldShipmentDate() {
        Parcel p = new Parcel();
        p.setShipmentDate(shipmentDate);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(p)).isEqualTo(Optional.of(shipmentDate));
    }

    @Test
    public void testParcelNewShipmentDate() {
        Parcel p = new Parcel();
        p.setShipmentDateTimeBySupplier(shipmentDateTime);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(p)).isEqualTo(Optional.of(shipmentDate));
    }

    @Test
    public void testParcelBothShipmentDate() {
        Parcel p = new Parcel();
        p.setShipmentDate(anotherShipmentDate);
        p.setShipmentDateTimeBySupplier(shipmentDateTime);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(p)).isEqualTo(Optional.of(shipmentDate));
    }

    @Test
    public void testParcelItemNoShipmentDate() {
        ParcelItem i = new ParcelItem();
        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(i)).isEqualTo(Optional.empty());
    }

    @Test
    public void testParcelItemOldShipmentDate() {
        ParcelItem i = new ParcelItem();
        i.setSupplierShipmentDateTime(shipmentTimestamp);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(i)).isEqualTo(Optional.of(shipmentDate));
    }

    @Test
    public void testParcelItemNewShipmentDate() {
        ParcelItem i = new ParcelItem();
        i.setShipmentDateTimeBySupplier(shipmentDateTime);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(i)).isEqualTo(Optional.of(shipmentDate));
    }

    @Test
    public void testParcelItemBothShipmentDate() {
        ParcelItem i = new ParcelItem();
        i.setSupplierShipmentDateTime(anotherShipmentDateTime);
        i.setShipmentDateTimeBySupplier(shipmentDateTime);

        softly.assertThat(CheckouterDateUtils.getSupplierShipmentDate(i)).isEqualTo(Optional.of(shipmentDate));
    }
}
