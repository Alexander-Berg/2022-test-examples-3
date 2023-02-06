package ru.yandex.market.delivery.mdbapp.integration.converter;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.market.delivery.entities.common.KoroByte;
import ru.yandex.market.delivery.entities.common.Parcel;
import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.mdbapp.exception.ParcelKorobyteException;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class ParcelConverterTest {

    private ParcelConverter parcelConverter = new ParcelConverter();

    @Test
    public void convertShipmentParcelToCommonParcel() {
        assertThat(parcelConverter.convert(getCheckouterParcel())).as("Checkouter parcel converted")
                .isEqualTo(getExpectedParcel());
    }

    @Test(expected = ParcelKorobyteException.class)
    public void convertShipmentParcelToCommonParcelFailed() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel parcel = getCheckouterParcel();
        parcel.setHeight(null);
        parcelConverter.convert(parcel);
    }

    private ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel getCheckouterParcel() {
        ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel parcel =
                new ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel();

        parcel.setId(123L);
        parcel.setWidth(100L);
        parcel.setHeight(200L);
        parcel.setDepth(300L);
        parcel.setWeight(1000L);
        parcel.setParcelItems(Collections.emptyList());

        return parcel;
    }

    private Parcel getExpectedParcel() {
        ResourceId parcelId = new ResourceId();
        parcelId.setYandexId("123");
        KoroByte koroByte = new KoroByte();
        koroByte.setWidth(100);
        koroByte.setHeight(200);
        koroByte.setLength(300);
        koroByte.setWeightGross(BigDecimal.valueOf(1.0));

        Parcel parcel = new Parcel(parcelId, koroByte, Collections.emptyList());

        return parcel;
    }
}
