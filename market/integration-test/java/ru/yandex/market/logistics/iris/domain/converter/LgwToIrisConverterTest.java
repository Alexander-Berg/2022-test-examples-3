package ru.yandex.market.logistics.iris.domain.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Test;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.converter.LgwToIrisConverter;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.CargoTypes;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.Urls;
import ru.yandex.market.logistics.iris.core.index.complex.VendorCodes;
import ru.yandex.market.logistics.iris.jobs.consumers.reference.dto.FlatItemReference;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class LgwToIrisConverterTest {

    private static LocalDateTime UPDATED_DATE_TIME = LocalDateTime.parse("1970-01-02T00:00:00");

    @Test
    public void shouldTakeValuesFromItemReferenceIfItemIsNull() {
        ItemReference itemReference = new ItemReference(
                new UnitId("", 1L, "sku"),
                new Korobyte(50, 51, 52, BigDecimal.valueOf(11), BigDecimal.valueOf(10), BigDecimal.valueOf(1)),
                100,
                Sets.newHashSet(
                        new Barcode("code", "type", BarcodeSource.SUPPLIER),
                        new Barcode(null, null, null)
                ),
                null
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getUpdatedDateTime()).isEqualTo(updatedDateTime);

            assertions.assertThat(result.getPartnerId()).isEqualTo("1");
            assertions.assertThat(result.getPartnerSku()).isEqualTo("sku");

            assertions.assertThat(result.getWidth()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertions.assertThat(result.getHeight()).isEqualByComparingTo(BigDecimal.valueOf(510));
            assertions.assertThat(result.getLength()).isEqualByComparingTo(BigDecimal.valueOf(520));

            assertions.assertThat(result.getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(11000));
            assertions.assertThat(result.getWeightNett()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertions.assertThat(result.getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(1000));

            assertions.assertThat(result.getLifetime()).isEqualTo(100);
            assertions.assertThat(result.getBarcodes()).isEqualTo(Barcodes.of(ImmutableList.of(
                    new ru.yandex.market.logistics.iris.core.index.complex.Barcode(
                            "code",
                            "type",
                            ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource.SUPPLIER
                    ),
                    new ru.yandex.market.logistics.iris.core.index.complex.Barcode(
                            null,
                            null,
                            ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource.UNKNOWN
                    )
            )));

            assertions.assertThat(result.getName()).isNull();
            assertions.assertThat(result.getSnMask()).isNull();
            assertions.assertThat(result.getImeiMask()).isNull();
            assertions.assertThat(result.getCheckSn()).isNull();
            assertions.assertThat(result.getCheckImei()).isNull();
            assertions.assertThat(result.getHasLifeTime()).isNull();
            assertions.assertThat(result.getBoxCapacity()).isNull();
            assertions.assertThat(result.getVendorCodes()).isNull();
            assertions.assertThat(result.getCargoTypes()).isNull();
            assertions.assertThat(result.getInboundRemainingLifetimesDays()).isNull();
            assertions.assertThat(result.getInboundRemainingLifetimesPercentage()).isNull();
            assertions.assertThat(result.getOutboundRemainingLifetimesDays()).isNull();
            assertions.assertThat(result.getOutboundRemainingLifetimesPercentage()).isNull();
        });
    }

    @Test
    public void shouldTakeValuesFromItemReferenceIfTheyAreAbsentInItem() {
        ItemReference itemReference = new ItemReference(
                new UnitId("", 1L, "sku"),
                new Korobyte(50, 51, 52, BigDecimal.valueOf(11), BigDecimal.valueOf(10), BigDecimal.valueOf(1)),
                100,
                Sets.newHashSet(
                        new Barcode("code", "type", BarcodeSource.SUPPLIER),
                        new Barcode(null, null, null)
                ),
                new Item.ItemBuilder("name", null, null, null, null)
                        .setBarcodes(new ArrayList<>())
                        .setBoxCapacity(2)
                        .setHasLifeTime(true)
                        .setSnMask("SN273648")
                        .setImeiMask("356938035643809")
                        .setCheckSn(1)
                        .setCheckImei(1)
                        .setBoxCount(20)
                        .setVendorCodes(ImmutableList.of("vendorCode1", "vendorCode2"))
                        .setUrls(ImmutableList.of("url1", "url2"))
                        .setRemainingLifetimes(new RemainingLifetimes(
                                new ShelfLives(
                                        new ShelfLife(10),
                                        new ShelfLife(15)
                                ),
                                new ShelfLives(
                                        new ShelfLife(20),
                                        new ShelfLife(25)
                                ))
                        )
                        .setUpdateDateTime(DateTime.fromLocalDateTime(UPDATED_DATE_TIME))
                        .build()
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(LocalDateTime.MIN, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getUpdatedDateTime()).isEqualTo(updatedDateTime);

            assertions.assertThat(result.getPartnerId()).isEqualTo("1");
            assertions.assertThat(result.getPartnerSku()).isEqualTo("sku");

            assertions.assertThat(result.getWidth()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertions.assertThat(result.getHeight()).isEqualByComparingTo(BigDecimal.valueOf(510));
            assertions.assertThat(result.getLength()).isEqualByComparingTo(BigDecimal.valueOf(520));

            assertions.assertThat(result.getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(11000));
            assertions.assertThat(result.getWeightNett()).isEqualByComparingTo(BigDecimal.valueOf(10000));
            assertions.assertThat(result.getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(1000));

            assertions.assertThat(result.getLifetime()).isEqualTo(100);

            assertions.assertThat(result.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(result.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(result.getCheckSn()).isEqualTo(1);
            assertions.assertThat(result.getCheckImei()).isEqualTo(1);

            RemainingLifetime inboundDays = result.getInboundRemainingLifetimesDays().getRemainingLifetimes().get(0);
            assertions.assertThat(inboundDays).isNotNull();
            assertions.assertThat(inboundDays.getValue()).isEqualTo(10);
            assertions.assertThat(inboundDays.getUpdatedTimestamp()).isEqualTo(86400000);

            RemainingLifetime inboundPercentage = result.getInboundRemainingLifetimesPercentage().getRemainingLifetimes().get(0);
            assertions.assertThat(inboundPercentage).isNotNull();
            assertions.assertThat(inboundPercentage.getValue()).isEqualTo(15);
            assertions.assertThat(inboundPercentage.getUpdatedTimestamp()).isEqualTo(86400000);

            RemainingLifetime outboundDays = result.getOutboundRemainingLifetimesDays().getRemainingLifetimes().get(0);
            assertions.assertThat(outboundDays).isNotNull();
            assertions.assertThat(outboundDays.getValue()).isEqualTo(20);
            assertions.assertThat(outboundDays.getUpdatedTimestamp()).isEqualTo(86400000);

            RemainingLifetime outboundPercentage = result.getOutboundRemainingLifetimesPercentage().getRemainingLifetimes().get(0);
            assertions.assertThat(outboundPercentage).isNotNull();
            assertions.assertThat(outboundPercentage.getValue()).isEqualTo(25);
            assertions.assertThat(outboundPercentage.getUpdatedTimestamp()).isEqualTo(86400000);
        });
    }

    @Test
    public void shouldConvertNullVendorCodesToNullReferenceValue() {
        ItemReference itemReference = new ItemReference(
            new UnitId("", 1L, "sku"),
            new Korobyte(50, 51, 52, BigDecimal.valueOf(11), BigDecimal.valueOf(10), BigDecimal.valueOf(1)),
            100,
            Sets.newHashSet(
                new Barcode("code", "type", BarcodeSource.SUPPLIER),
                new Barcode(null, null, null)
            ),
            new Item.ItemBuilder("name", null, null, null, null).build()
        );
        ZonedDateTime updatedDateTime = ZonedDateTime.of(UPDATED_DATE_TIME, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);

        assertSoftly(assertions -> assertions.assertThat(result.getVendorCodes()).isNull());
    }

    @Test
    public void shouldPreferToTakeValuesFromItemIfItemAndValuesInItArePresent() {
        ItemReference itemReference = new ItemReference(
                new UnitId("", 1L, "sku"),
                new Korobyte(50, 51, 52, BigDecimal.valueOf(11), BigDecimal.valueOf(10), BigDecimal.valueOf(1)),
                100,
                Sets.newHashSet(
                        new Barcode("code", "type", BarcodeSource.SUPPLIER),
                        new Barcode(null, null, null)
                ),
                new Item.ItemBuilder("name", null, null, null, null)
                        .setBarcodes(new ArrayList<>())
                        .setBoxCapacity(2)
                        .setHasLifeTime(true)
                        .setSnMask("SN273648")
                        .setImeiMask("356938035643809")
                        .setCheckSn(1)
                        .setCheckImei(1)
                        .setBoxCount(20)
                        .setVendorCodes(ImmutableList.of("vendorCode1", "vendorCode2"))
                        .setUrls(ImmutableList.of("url1", "url2"))
                        .setCargoTypes(List.of(CargoType.VALUABLE, CargoType.JEWELRY))
                        .setUnitId(new UnitId("id", 2L, "skuFromItem"))
                        .setKorobyte(new Korobyte(51, 52, 53, BigDecimal.valueOf(12), BigDecimal.valueOf(13), BigDecimal.valueOf(14)))
                        .setBarcodes(
                                ImmutableList.of(
                                        new Barcode("codeFromItem1", "typeFromItem1", BarcodeSource.PARTNER)
                                )
                        )
                        .setLifeTime(200)
                        .build()
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(UPDATED_DATE_TIME, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getUpdatedDateTime()).isEqualTo(updatedDateTime);

            assertions.assertThat(result.getPartnerId()).isEqualTo("2");
            assertions.assertThat(result.getPartnerSku()).isEqualTo("skuFromItem");

            assertions.assertThat(result.getWidth()).isEqualByComparingTo(BigDecimal.valueOf(510));
            assertions.assertThat(result.getHeight()).isEqualByComparingTo(BigDecimal.valueOf(520));
            assertions.assertThat(result.getLength()).isEqualByComparingTo(BigDecimal.valueOf(530));

            assertions.assertThat(result.getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(12000));
            assertions.assertThat(result.getWeightNett()).isEqualByComparingTo(BigDecimal.valueOf(13000));
            assertions.assertThat(result.getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(14000));

            assertions.assertThat(result.getLifetime()).isEqualTo(200);
            assertions.assertThat(result.getBarcodes()).isEqualTo(Barcodes.of(ImmutableList.of(new ru.yandex.market.logistics.iris.core.index.complex.Barcode("codeFromItem1", "typeFromItem1", ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource.PARTNER))));

            assertions.assertThat(result.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(result.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(result.getCheckSn()).isEqualTo(1);
            assertions.assertThat(result.getCheckImei()).isEqualTo(1);

            assertions.assertThat(result.getName()).isEqualTo("name");
            assertions.assertThat(result.getHasLifeTime()).isTrue();
            assertions.assertThat(result.getBoxCapacity()).isEqualTo(2);
            assertions.assertThat(result.getBoxCount()).isEqualTo(20);
            assertions.assertThat(result.getVendorCodes()).isEqualTo(VendorCodes.of(ImmutableList.of("vendorCode1", "vendorCode2")));
            assertions.assertThat(result.getUrls()).isEqualTo(Urls.of(ImmutableList.of("url1", "url2")));
            assertions.assertThat(result.getCargoTypes()).isEqualTo(CargoTypes.of(List.of(40, 80)));

            assertions.assertThat(result.getInboundRemainingLifetimesDays()).isNull();
            assertions.assertThat(result.getInboundRemainingLifetimesPercentage()).isNull();
            assertions.assertThat(result.getOutboundRemainingLifetimesDays()).isNull();
            assertions.assertThat(result.getOutboundRemainingLifetimesPercentage()).isNull();
        });
    }

    @Test
    public void shouldPreferToTakeValuesFromItemIfRemainingLifetimesPresentPartly() {
        ItemReference itemReference = new ItemReference(
                 new UnitId("", 1L, "sku"),
                null,
                null,
                null,
                new Item.ItemBuilder("name", null, null, null, null)
                        .setRemainingLifetimes(new RemainingLifetimes(
                                new ShelfLives(
                                        null,
                                        new ShelfLife(15)
                                ),
                                null
                                )
                        )
                        .build()
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(UPDATED_DATE_TIME, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getInboundRemainingLifetimesDays().getRemainingLifetimes()).isEmpty();

            RemainingLifetime inboundPercentage = result.getInboundRemainingLifetimesPercentage().getRemainingLifetimes().get(0);
            assertions.assertThat(inboundPercentage).isNotNull();
            assertions.assertThat(inboundPercentage.getValue()).isEqualTo(15);
            assertions.assertThat(inboundPercentage.getUpdatedTimestamp()).isEqualTo(86400000);

            assertions.assertThat(result.getOutboundRemainingLifetimesDays().getRemainingLifetimes()).isEmpty();
            assertions.assertThat(result.getOutboundRemainingLifetimesPercentage().getRemainingLifetimes()).isEmpty();
        });
    }

    @Test
    public void shouldPreferToTakeValuesFromItemIfInboundAndOutboundRemainingLifetimesAreNotPresent() {
        ItemReference itemReference = new ItemReference(
                new UnitId("", 1L, "sku"),
                null,
                null,
                null,
                new Item.ItemBuilder("name", null, null, null, null)
                        .setRemainingLifetimes(new RemainingLifetimes(null, null))
                        .build()
        );

        ZonedDateTime updatedDateTime = ZonedDateTime.of(UPDATED_DATE_TIME, ZoneOffset.UTC);
        LgwToIrisConverter lgwToIrisConverter = new LgwToIrisConverter(new FixedUtcTimestampProvider(updatedDateTime));

        FlatItemReference result = lgwToIrisConverter.toFlatItemReference(itemReference);
        assertSoftly(assertions -> {
            assertions.assertThat(result.getInboundRemainingLifetimesDays().getRemainingLifetimes()).isEmpty();
            assertions.assertThat(result.getInboundRemainingLifetimesPercentage().getRemainingLifetimes()).isEmpty();
            assertions.assertThat(result.getOutboundRemainingLifetimesDays().getRemainingLifetimes()).isEmpty();
            assertions.assertThat(result.getOutboundRemainingLifetimesPercentage().getRemainingLifetimes()).isEmpty();
        });
    }

}
