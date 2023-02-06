package ru.yandex.market.wms.radiator.test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.CisHandleMode;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.logistic.api.utils.DateTime;

public class TestReferenceItemsData {

    public static final long VENDOR_ID = 1559L;
    public static final String M_SKU_AUTO_GET_STOCKS_TEST = "AUTO_GET_STOCKS_TEST";
    public static final String M_SKU_REF_MULTIBOX = "REF_MULTIBOX";
    public static final String M_SKU_REF_IDENTITIES = "REF_IDENTITES";

    public static ItemReference mSku100_01() {
        return itemReference(TestData.M_SKU_100_01);
    }

    public static ItemReference mSku100_02() {
        return itemReference(TestData.M_SKU_100_02);
    }

    public static ItemReference mSku100_03() {
        return itemReference(TestData.M_SKU_100_03);
    }

    public static ItemReference mSku100_04() {
        return itemReference(TestData.M_SKU_100_04);
    }

    public static ItemReference mSkuAutoGetStocksTest() {
        var unitId = new UnitId(M_SKU_AUTO_GET_STOCKS_TEST, VENDOR_ID, M_SKU_AUTO_GET_STOCKS_TEST);

        // korobyte created when hasWeightGross && hasHeight && hasWidth && hasLength
        Korobyte korobyte = new Korobyte(
                23, // <- PACK.WIDTHUOM3
                34, // <- PACK.HEIGHTUOM3
                12, // <- PACK.LENGTHUOM3
                new BigDecimal("3.0"),  // <- SKU.STDGROSSWGT
                new BigDecimal("3.0"),  // <- SKU.STDNETWGT
                new BigDecimal("0.0")   // <- SKU.TARE
        );

        Barcode barCode = barCode("GETSTOCKSTEST");

        return new ItemReference(
                unitId,
                korobyte,
                365,    // <- SKU.SHELFLIFEINDICATOR="Y" (TRACK_LIFETIME) && SKU.TOEXPIREDAYS="365"
                Set.of(barCode),   // <- ALTSKU.ALTSKU
                new Item.ItemBuilder("AUTO_GET_STOCKS_TEST", 0, BigDecimal.ZERO)
                        .setUnitId(unitId)
                        .setHasLifeTime(true)
                        .setLifeTime(365)
                        .setKorobyte(korobyte)
                        .setBarcodes(List.of(barCode))
                        .setRemainingLifetimes(
                                new RemainingLifetimes(
                                        new ShelfLives(new ShelfLife(90), new ShelfLife(50)),
                                        new ShelfLives(new ShelfLife(180), new ShelfLife(25))
                                )
                        )
//                        .setUpdatedDateTime(
//                                DateTime.fromOffsetDateTime(OffsetDateTime.parse("2007-12-01T10:15:30+03:00"))
//                        )
                        .build()
        );
    }

    public static ItemReference mSkuRefMultibox() {
        var unitId = new UnitId(M_SKU_REF_MULTIBOX, VENDOR_ID, M_SKU_REF_MULTIBOX);

        // korobyte created when hasWeightGross && hasHeight && hasWidth && hasLength
        Korobyte korobyte = new Korobyte(
                20, // <- PACK.WIDTHUOM3
                30, // <- PACK.HEIGHTUOM3
                10, // <- PACK.LENGTHUOM3
                new BigDecimal("4.4"),  // <- SKU.STDGROSSWGT
                new BigDecimal("4.4"),  // <- SKU.STDNETWGT
                new BigDecimal("0.0")   // <- SKU.TARE
        );

        Barcode barCode = barCode("REF_MULTIBOX");

        return new ItemReference(
                unitId,
                korobyte,
                null,
                Set.of(barCode),   // <- ALTSKU.ALTSKU
                new Item.ItemBuilder(null, 0, BigDecimal.ZERO)
                        .setUnitId(unitId)
                        .setHasLifeTime(false)
                        .setKorobyte(korobyte)
                        .setBarcodes(List.of(barCode))
                        .setBoxCount(2)
                        .build()
        );
    }

    public static ItemReference mSkuRefIdentities() {
        var unitId = new UnitId(M_SKU_REF_IDENTITIES, VENDOR_ID, M_SKU_REF_IDENTITIES);

        // korobyte created when hasWeightGross && hasHeight && hasWidth && hasLength
        Korobyte korobyte = new Korobyte(
                23, // <- PACK.WIDTHUOM3
                34, // <- PACK.HEIGHTUOM3
                12, // <- PACK.LENGTHUOM3
                new BigDecimal("3.0"),  // <- SKU.STDGROSSWGT
                new BigDecimal("3.0"),  // <- SKU.STDNETWGT
                new BigDecimal("0.0")   // <- SKU.TARE
        );

        Barcode barCode = barCode("IDENTITIES_BARCODE");

        return new ItemReference(
                unitId,
                korobyte,
                365,    // <- SKU.SHELFLIFEINDICATOR="Y" (TRACK_LIFETIME) && SKU.TOEXPIREDAYS="365"
                Set.of(barCode),   // <- ALTSKU.ALTSKU
                new Item.ItemBuilder(M_SKU_REF_IDENTITIES, 0, BigDecimal.ZERO)
                        .setUnitId(unitId)
                        .setHasLifeTime(true)
                        .setLifeTime(365)
                        .setKorobyte(korobyte)
                        .setBarcodes(List.of(barCode))
                        .setRemainingLifetimes(
                                new RemainingLifetimes(
                                        new ShelfLives(new ShelfLife(90), new ShelfLife(50)),
                                        new ShelfLives(new ShelfLife(180), new ShelfLife(25))
                                )
                        )
//                        .setUpdatedDateTime(
//                                DateTime.fromOffsetDateTime(OffsetDateTime.parse("2007-12-01T10:15:30+03:00"))
//                        )
                        .setCargoTypes(new CargoTypes.CargoTypesBuilder()
                                .setCargoTypes(Arrays.asList(CargoType.create(950), CargoType.create(980)))
                                .build())
                        .setCheckImei(0)
                        .setImeiMask("^(\\\\d{15}|\\\\d{17})$")
                        .setCheckSn(1)
                        .setSnMask("^((?:[sS]?[\\\\dA-Za-z\\\\/]{10,12})|([\\\\dA-Za-z\\\\/]{14,16})|[\\\\dA-Za-z\\\\/]{18,20})$")
                        .setCisHandleMode(CisHandleMode.create(2))
                        .build()
        );
    }

    public static Barcode barCode(String code) {
        return new Barcode.BarcodeBuilder(code).setSource(BarcodeSource.UNKNOWN).build();
    }


    private static ItemReference itemReference(String mSku) {
        var unitId = TestData.unitId(mSku);
        Korobyte korobyte = new Korobyte(0, 0, 0, new BigDecimal("0.0"), new BigDecimal("0.0"), new BigDecimal("0.0"));
        return new ItemReference(
                unitId,
                korobyte,
                null,
                null,
                new Item.ItemBuilder(null, 0, BigDecimal.ZERO)
                        .setUnitId(unitId)
                        .setHasLifeTime(false)
                        .setKorobyte(korobyte)
                        .build()
        );
    }
}
