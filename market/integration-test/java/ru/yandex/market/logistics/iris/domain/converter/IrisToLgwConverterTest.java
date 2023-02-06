package ru.yandex.market.logistics.iris.domain.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.converter.IrisToLgwConverter;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.ReferenceIndexerTestFactory;
import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.CargoTypes;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.Dimensions;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes;
import ru.yandex.market.logistics.iris.core.index.complex.Urls;
import ru.yandex.market.logistics.iris.core.index.complex.VendorCodes;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.service.system.SystemPropertyService;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class IrisToLgwConverterTest {

    private static final ZonedDateTime DEFAULT_DATE_TIME = ZonedDateTime.of(
            LocalDateTime.of(2012, 12, 3, 11, 15, 30), ZoneId.of("-05:00")
    );

    private SystemPropertyService systemPropertyService = Mockito.mock(SystemPropertyService.class);
    private ChangeTrackingReferenceIndexer referenceIndexer = ReferenceIndexerTestFactory.getIndexer();
    private IrisToLgwConverter converter = new IrisToLgwConverter(systemPropertyService);

    @Test
    public void shouldSuccessfulConverted() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndex();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> {
            assertions.assertThat(result.getName()).isEqualTo("Item1");

            assertions.assertThat(result.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(result.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(result.getCheckSn()).isEqualTo(1);
            assertions.assertThat(result.getCheckImei()).isEqualTo(1);

            assertions.assertThat(result.getUnitId()).isEqualTo(new UnitId(null, 1L, "partner_sku"));

            assertions.assertThat(result.getBoxCapacity()).isEqualTo(10);
            assertions.assertThat(result.getBoxCount()).isEqualTo(100);
            assertions.assertThat(result.getCount()).isEqualTo(0);
            assertions.assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0));

            assertions.assertThat(result.getHasLifeTime()).isEqualTo(false);
            assertions.assertThat(result.getVendorCodes()).isEqualTo(ImmutableList.of("vendorCode1"));
            assertions.assertThat(result.getUrls()).isEqualTo(ImmutableList.of("url1"));
            assertions.assertThat(result.getCargoTypes()).isEqualTo(ImmutableList.of(CargoType.VALUABLE, CargoType.JEWELRY));
            assertions.assertThat(result.getBarcodes()).isEqualTo(
                    ImmutableList.of(
                            new ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode(
                                    "code",
                                    "type",
                                    ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.SUPPLIER)
                    )
            );

            assertions.assertThat(result.getKorobyte()).isNotNull();
            assertions.assertThat(result.getKorobyte().getWidth()).isEqualByComparingTo(10);
            assertions.assertThat(result.getKorobyte().getHeight()).isEqualByComparingTo(20);
            assertions.assertThat(result.getKorobyte().getLength()).isEqualByComparingTo(30);
            assertions.assertThat(result.getKorobyte().getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(1.210));
            assertions.assertThat(result.getKorobyte().getWeightNet()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));
            assertions.assertThat(result.getKorobyte().getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));

            assertions.assertThat(result.getRemainingLifetimes()).isNull();

            assertions.assertThat(result.getUpdated()).isNotNull();
            assertions.assertThat(result.getUpdated()).isEqualTo(new DateTime("2012-12-21T16:15:30+00:00"));
        });
    }

    @Test
    public void shouldConvertWithEmptyBarcodesAndVendorCodes() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndexWithoutBarcodesAndVendorCodes();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> {
            assertions.assertThat(result.getName()).isEqualTo("Item1");

            assertions.assertThat(result.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(result.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(result.getCheckSn()).isEqualTo(1);
            assertions.assertThat(result.getCheckImei()).isEqualTo(1);

            assertions.assertThat(result.getUnitId()).isEqualTo(new UnitId(null, 1L, "partner_sku"));

            assertions.assertThat(result.getBoxCapacity()).isEqualTo(10);
            assertions.assertThat(result.getBoxCount()).isEqualTo(100);
            assertions.assertThat(result.getCount()).isEqualTo(0);
            assertions.assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0));

            assertions.assertThat(result.getHasLifeTime()).isEqualTo(false);
            assertions.assertThat(result.getVendorCodes()).isEmpty();
            assertions.assertThat(result.getUrls()).isEmpty();
            assertions.assertThat(result.getBarcodes()).isEmpty();


            assertions.assertThat(result.getKorobyte()).isNotNull();
            assertions.assertThat(result.getKorobyte().getWidth()).isEqualByComparingTo(10);
            assertions.assertThat(result.getKorobyte().getHeight()).isEqualByComparingTo(20);
            assertions.assertThat(result.getKorobyte().getLength()).isEqualByComparingTo(30);
            assertions.assertThat(result.getKorobyte().getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(1.001));
            assertions.assertThat(result.getKorobyte().getWeightNet()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));
            assertions.assertThat(result.getKorobyte().getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));

            assertions.assertThat(result.getRemainingLifetimes()).isNull();

            assertions.assertThat(result.getUpdated()).isNotNull();
            assertions.assertThat(result.getUpdated()).isEqualTo(new DateTime("2012-12-03T16:15:30+00:00"));
        });
    }

    @Test
    public void shouldConvertWithNullKorobyte() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndexWithoutDimensionsAndWeightGross();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> {
            assertions.assertThat(result.getName()).isEqualTo("Item1");

            assertions.assertThat(result.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(result.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(result.getCheckSn()).isEqualTo(1);
            assertions.assertThat(result.getCheckImei()).isEqualTo(1);

            assertions.assertThat(result.getUnitId()).isEqualTo(new UnitId(null, 1L, "partner_sku"));

            assertions.assertThat(result.getBoxCapacity()).isEqualTo(10);
            assertions.assertThat(result.getBoxCount()).isEqualTo(100);
            assertions.assertThat(result.getCount()).isEqualTo(0);
            assertions.assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(0));

            assertions.assertThat(result.getHasLifeTime()).isEqualTo(false);
            assertions.assertThat(result.getVendorCodes()).isEqualTo(ImmutableList.of("vendorCode1"));
            assertions.assertThat(result.getUrls()).isEqualTo(ImmutableList.of("url1"));
            assertions.assertThat(result.getBarcodes()).isEqualTo(
                    ImmutableList.of(
                            new ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode(
                                    "code",
                                    "type",
                                    ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource.SUPPLIER)
                    )
            );

            assertions.assertThat(result.getKorobyte()).isNotNull();
            assertions.assertThat(result.getKorobyte().getWidth()).isEqualByComparingTo(0);
            assertions.assertThat(result.getKorobyte().getHeight()).isEqualByComparingTo(0);
            assertions.assertThat(result.getKorobyte().getLength()).isEqualByComparingTo(0);
            assertions.assertThat(result.getKorobyte().getWeightGross()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));
            assertions.assertThat(result.getKorobyte().getWeightNet()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));
            assertions.assertThat(result.getKorobyte().getWeightTare()).isEqualByComparingTo(BigDecimal.valueOf(0).setScale(1));

            assertions.assertThat(result.getRemainingLifetimes()).isNull();

            assertions.assertThat(result.getUpdated()).isNotNull();
            assertions.assertThat(result.getUpdated()).isEqualTo(new DateTime("2012-12-03T16:15:30+00:00"));
        });
    }

    @Test
    public void shouldConvertWithFullRemainingLifetimes() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndexWithRemainingLifetimes();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> {
            assertions.assertThat(result.getUnitId()).isEqualTo(new UnitId(null, 1L, "partner_sku"));

            assertions.assertThat(result.getRemainingLifetimes()).isNotNull();

            assertions.assertThat(result.getRemainingLifetimes()).isNotNull();
            assertions.assertThat(result.getRemainingLifetimes().getInbound()).isNotNull();
            assertions.assertThat(result.getRemainingLifetimes().getInbound().getDays().getValue()).isEqualTo(15);
            assertions.assertThat(result.getRemainingLifetimes().getInbound().getPercentage().getValue()).isEqualTo(25);

            assertions.assertThat(result.getRemainingLifetimes().getOutbound()).isNotNull();
            assertions.assertThat(result.getRemainingLifetimes().getOutbound().getDays().getValue()).isEqualTo(14);
            assertions.assertThat(result.getRemainingLifetimes().getOutbound().getPercentage().getValue()).isEqualTo(20);

            assertions.assertThat(result.getUpdated()).isNotNull();
            assertions.assertThat(result.getUpdated()).isEqualTo(new DateTime("2012-12-03T16:15:30+00:00"));
        });
    }

    @Test
    public void shouldConvertWithRemainingLifetimes() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndexWithNullRemainingLifetimes();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> {
            assertions.assertThat(result.getUnitId()).isEqualTo(new UnitId(null, 1L, "partner_sku"));

            assertions.assertThat(result.getRemainingLifetimes()).isNotNull();

            assertions.assertThat(result.getRemainingLifetimes().getInbound()).isNotNull();
            assertions.assertThat(result.getRemainingLifetimes().getInbound().getDays().getValue()).isEqualTo(15);
            assertions.assertThat(result.getRemainingLifetimes().getInbound().getPercentage()).isNull();

            assertions.assertThat(result.getRemainingLifetimes().getOutbound()).isNotNull();
            assertions.assertThat(result.getRemainingLifetimes().getOutbound().getDays()).isNull();
            assertions.assertThat(result.getRemainingLifetimes().getOutbound().getPercentage().getValue()).isEqualTo(20);

            assertions.assertThat(result.getUpdated()).isNotNull();
            assertions.assertThat(result.getUpdated()).isEqualTo(new DateTime("2012-12-03T16:15:30+00:00"));
        });
    }

    @Test
    public void shouldReturnNullRemainingLifetimesIfInboundLifetimesAreNotPresent() {
        ChangeTrackingReferenceIndex index = mockChangeTrackingReferenceIndexWithInboundRemainingLifetimesNull();
        ItemIdentifier identifier = ItemIdentifier.of("1", "partner_sku");

        Item result = converter.toItem(identifier, Collections.emptyList(), index);

        assertSoftly(assertions -> assertions.assertThat(result.getRemainingLifetimes()).isNull());
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndex() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.NAME_FIELD, "Item1", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.SN_MASK_FIELD, "SN273648", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.IMEI_MASK_FIELD, "356938035643809", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_SN_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_IMEI_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_CAPACITY_FIELD, 10, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_COUNT_FIELD, 100, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.VENDOR_CODES_FIELD, VendorCodes.of(ImmutableList.of("vendorCode1")), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.URLS_FIELD, Urls.of(ImmutableList.of("url1")), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.HAS_LIFETIME_FIELD, false, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.DIMENSIONS, createDimensions(100, 200, 300), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.WEIGHT_GROSS, createDimension(1210.0), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BARCODES,
                Barcodes.of(ImmutableList.of(new Barcode("code", "type", BarcodeSource.SUPPLIER))),
                ZonedDateTime.of(
                        LocalDateTime.of(2012, 12, 21, 11, 15, 30),
                        ZoneId.of("-05:00"))
        );
        index.set(PredefinedFields.CARGO_TYPES, CargoTypes.of(List.of(40, 80)), DEFAULT_DATE_TIME);
        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexWithoutBarcodesAndVendorCodes() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.NAME_FIELD, "Item1", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.SN_MASK_FIELD, "SN273648", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.IMEI_MASK_FIELD, "356938035643809", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_SN_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_IMEI_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_CAPACITY_FIELD, 10, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_COUNT_FIELD, 100, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.HAS_LIFETIME_FIELD, false, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.DIMENSIONS, createDimensions(100, 200, 300), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.WEIGHT_GROSS, createDimension(1000.1), DEFAULT_DATE_TIME);
        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexWithoutDimensionsAndWeightGross() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.NAME_FIELD, "Item1", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.SN_MASK_FIELD, "SN273648", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.IMEI_MASK_FIELD, "356938035643809", DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_SN_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.CHECK_IMEI_FIELD, 1, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_CAPACITY_FIELD, 10, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BOX_COUNT_FIELD, 100, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.VENDOR_CODES_FIELD, VendorCodes.of(ImmutableList.of("vendorCode1")), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.URLS_FIELD, Urls.of(ImmutableList.of("url1")), DEFAULT_DATE_TIME);
        index.set(PredefinedFields.HAS_LIFETIME_FIELD, false, DEFAULT_DATE_TIME);
        index.set(PredefinedFields.BARCODES, Barcodes.of(ImmutableList.of(new Barcode("code", "type", BarcodeSource.SUPPLIER))), DEFAULT_DATE_TIME);
        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexWithRemainingLifetimes() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(15, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(
                        RemainingLifetime.of(25, 86400000),
                        RemainingLifetime.of(20, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(14, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(20, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.HAS_LIFETIME_FIELD, true, DEFAULT_DATE_TIME);

        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexWithNullRemainingLifetimes() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(15, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(null),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(null),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(20, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.HAS_LIFETIME_FIELD, true, DEFAULT_DATE_TIME);
        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexWithInboundRemainingLifetimesNull() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(null), DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(null),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(14, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(20, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.HAS_LIFETIME_FIELD, true, DEFAULT_DATE_TIME);

        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexRemainingLifetimesWithHasLifetimeIsFalse() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(15, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(
                        RemainingLifetime.of(25, 86400000),
                        RemainingLifetime.of(20, 86400000))
                ),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(14, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(20, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.HAS_LIFETIME_FIELD, false, DEFAULT_DATE_TIME);
        return index;
    }

    private ChangeTrackingReferenceIndex mockChangeTrackingReferenceIndexRemainingLifetimesWithoutHasLifetime() {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(10, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(
                        RemainingLifetime.of(15, 86400000),
                        RemainingLifetime.of(20, 86400000))
                ),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(25, 86400000))),
                DEFAULT_DATE_TIME);

        index.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(30, 86400000))),
                DEFAULT_DATE_TIME);

        return index;
    }

    private Dimensions createDimensions(double wight, double height, double length) {
        return new Dimensions(
                createDimension(wight),
                createDimension(height),
                createDimension(length));
    }

    private Dimension createDimension(double value) {
        return Dimension.of(BigDecimal.valueOf(value));
    }
}
