package ru.yandex.market.logistics.iris.domain.converter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import ru.yandex.market.logistics.iris.client.model.entity.Dimensions;
import ru.yandex.market.logistics.iris.client.model.entity.TrustworthyItem;
import ru.yandex.market.logistics.iris.converter.TrustworthyItemConverter;
import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.CargoTypes;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.FlatStock;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes;
import ru.yandex.market.logistics.iris.core.index.complex.StockLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.StockType;
import ru.yandex.market.logistics.iris.core.index.complex.Urls;
import ru.yandex.market.logistics.iris.core.index.complex.VendorCodes;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ReferenceIndexImpl;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource.SUPPLIER;

public class TrustworthyItemConverterTest {

    private static final long DEFAULT_TIMESTAMP = Instant.now().toEpochMilli();

    @Test
    public void shouldSuccessConvertItem() {
        TrustworthyItem trustworthyItem = TrustworthyItemConverter.convert(initReferenceIndex());

        assertSoftly(assertions -> {
            assertions.assertThat(trustworthyItem.getWeightGross()).isEqualTo(BigDecimal.valueOf(12.112));
            assertions.assertThat(trustworthyItem.getName()).isEqualTo("sku1");
            assertions.assertThat(trustworthyItem.getWeightNet()).isNull();
            assertions.assertThat(trustworthyItem.getWeightTare()).isNull();

            assertions.assertThat(trustworthyItem.getDimensions())
                    .isEqualTo(new Dimensions(BigDecimal.valueOf(10.5), BigDecimal.valueOf(15.0), BigDecimal.valueOf(20)));

            assertions.assertThat(trustworthyItem.getSnMask()).isEqualTo("SN273648");
            assertions.assertThat(trustworthyItem.getImeiMask()).isEqualTo("356938035643809");
            assertions.assertThat(trustworthyItem.getCheckSn()).isEqualTo(1);
            assertions.assertThat(trustworthyItem.getCheckImei()).isEqualTo(1);

            assertions.assertThat(trustworthyItem.getLifetime()).isEqualTo(35);
            assertions.assertThat(trustworthyItem.getHasLifeTime()).isFalse();

            assertions.assertThat(trustworthyItem.getVendorCodes())
                    .isEqualTo(VendorCodes.of(ImmutableList.of("vendor_code_1", "vendor_code_2")));

            assertions.assertThat(trustworthyItem.getUrls())
                    .isEqualTo(Urls.of(ImmutableList.of("url_1", "url_2")));

            assertions.assertThat(trustworthyItem.getCargoTypes())
                    .isEqualTo(CargoTypes.of(List.of(40, 80)));

            assertions.assertThat(trustworthyItem.getBarcodes())
                    .isEqualTo(Barcodes.of(ImmutableList.of(new Barcode("code", "type", SUPPLIER))));

            assertions.assertThat(trustworthyItem.getBoxCount()).isEqualTo(1);
            assertions.assertThat(trustworthyItem.getBoxCapacity()).isEqualTo(6);
            assertions.assertThat(trustworthyItem.getMsku()).isEqualTo(1122334L);
            assertions.assertThat(trustworthyItem.getStockLifetime()).isNotNull();

            assertions.assertThat(trustworthyItem.getInboundRemainingLifetimesDays())
                    .isEqualTo(createRemainingLifetimes(60));

            assertions.assertThat(trustworthyItem.getInboundRemainingLifetimesPercentage())
                    .isEqualTo(createRemainingLifetimes(50));

            assertions.assertThat(trustworthyItem.getOutboundRemainingLifetimesDays())
                    .isEqualTo(createRemainingLifetimes(35));

            assertions.assertThat(trustworthyItem.getOutboundRemainingLifetimesPercentage())
                    .isEqualTo(createRemainingLifetimes(45));
        });

    }

    private ReferenceIndexImpl initReferenceIndex() {
        ReferenceIndexImpl referenceIndex = new ReferenceIndexImpl();

        referenceIndex.set(PredefinedFields.WEIGHT_GROSS, Dimension.of(BigDecimal.valueOf(12.112)), ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.NAME_FIELD, "sku1", ZonedDateTime.now());
        referenceIndex.set(
                PredefinedFields.DIMENSIONS,
                new ru.yandex.market.logistics.iris.core.index.complex.Dimensions(
                        Dimension.of(BigDecimal.valueOf(10.5)),
                        Dimension.of(BigDecimal.valueOf(15.0)),
                        Dimension.of(BigDecimal.valueOf(20))
                ),
                ZonedDateTime.now());

        referenceIndex.set(PredefinedFields.SN_MASK_FIELD, "SN273648", ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.IMEI_MASK_FIELD, "356938035643809", ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.CHECK_SN_FIELD, 1, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.CHECK_IMEI_FIELD, 1, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.LIFETIME_DAYS_FIELD, 35, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.HAS_LIFETIME_FIELD, false, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.VENDOR_CODES_FIELD, createVendorCodes(), ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.URLS_FIELD, createUrls(), ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.BARCODES, createBarcodes(), ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.CARGO_TYPES, createCargoTypes(), ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.BOX_COUNT_FIELD, 1, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.BOX_CAPACITY_FIELD, 6, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.MSKU_FIELD, 1122334L, ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.STOCK_LIFETIME, createStockLifetime(), ZonedDateTime.now());
        referenceIndex.set(
                PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_DAY_FIELD,
                createRemainingLifetimes(60),
                ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_DAY_FIELD,
                createRemainingLifetimes(35),
                ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.MIN_INBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                createRemainingLifetimes(50),
                ZonedDateTime.now());
        referenceIndex.set(PredefinedFields.MIN_OUTBOUND_REMAINING_LIFETIME_PERCENTAGE_FIELD,
                createRemainingLifetimes(45),
                ZonedDateTime.now());

        return referenceIndex;
    }

    private VendorCodes createVendorCodes() {
        return VendorCodes.of(ImmutableList.of("vendor_code_1", "vendor_code_2"));
    }

    private Urls createUrls() {
        return Urls.of(ImmutableList.of("url_1", "url_2"));
    }

    private Barcodes createBarcodes() {
        return Barcodes.of(ImmutableList.of(new Barcode("code", "type", SUPPLIER)));
    }

    private CargoTypes createCargoTypes() {
        return CargoTypes.of(List.of(40, 80));
    }

    private StockLifetime createStockLifetime() {
        return StockLifetime.of(ImmutableMap.of(
                "1970-01-01",
                Collections.singleton(new FlatStock(StockType.FIT, 10)))
        );
    }

    private RemainingLifetimes createRemainingLifetimes(int value) {
        return RemainingLifetimes.of(ImmutableList.of(RemainingLifetime.of(value, DEFAULT_TIMESTAMP)));
    }
}
