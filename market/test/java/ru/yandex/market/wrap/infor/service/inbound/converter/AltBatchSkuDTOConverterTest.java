package ru.yandex.market.wrap.infor.service.inbound.converter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistics.test.integration.SoftAssertionSupport;
import ru.yandex.market.wrap.infor.client.model.AltSkuDTO;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.ItemMeta;

class AltBatchSkuDTOConverterTest extends SoftAssertionSupport {

    private static final UnitId UNIT_ID = new UnitId("ssku1", 1L, "ssku1");
    private static final String FORMATTED_ID = "ROV0000000000000000001";

    private static final String FIRST_BARCODE = "BCD001";
    private static final String SECOND_BARCODE = "BCD002";


    static Stream<Arguments> data() {
        return Stream.of(
            firstScenario(),
            secondScenario(),
            thirdScenario(),
            fourthScenario()
        );
    }

    private final AltSkuDTOConverter converter = new AltSkuDTOConverter();

    @MethodSource("data")
    @ParameterizedTest
    void conversion(ItemMeta input, List<AltSkuDTO> expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            softly.assertThatThrownBy(() -> converter.convert(input)).isInstanceOf(expectedException);
        } else {
            softly.assertThat(converter.convert(input)).isEqualTo(expected);
        }
    }

    /**
     * У Item'а отсутствуют barcode'ы - ожидаем пустой лист на выходе из конвертора.
     */
    private static Arguments firstScenario() {
        return Arguments.of(
            new ItemMeta(new Item.ItemBuilder(null, null, null).setUnitId(UNIT_ID).build(), UNIT_ID, FORMATTED_ID),
            Collections.emptyList(),
            null
        );
    }

    /**
     * У Item'а присутствует ровно 1 barcode - ожидаем на выходе ровно 1 AltSku в листе.
     */
    private static Arguments secondScenario() {
        return Arguments.of(
            new ItemMeta(new Item.ItemBuilder(null, null, null)
                .setUnitId(UNIT_ID)
                .setBarcodes(ImmutableList.of(new Barcode(FIRST_BARCODE, "", BarcodeSource.UNKNOWN)))
                .build(),
                UNIT_ID, FORMATTED_ID
            ),
            ImmutableList.of(new AltSkuDTO()
                .storerkey(UNIT_ID.getVendorId().toString())
                .sku(FORMATTED_ID)
                .altsku(FIRST_BARCODE)
                .type(AltSkuDTOConverter.BARCODE_ALTSKU_TYPE)
            ),
            null
        );
    }

    /**
     * У Item'а присутствует ровно 2 разных barcode'а - ожидаем на выходе 2 AltSku в листе.
     */
    private static Arguments thirdScenario() {
        return Arguments.of(
            new ItemMeta(new Item.ItemBuilder(null, null, null)
                .setUnitId(UNIT_ID)
                .setBarcodes(ImmutableList.of(
                    new Barcode(FIRST_BARCODE, "", BarcodeSource.UNKNOWN),
                    new Barcode(SECOND_BARCODE, "", BarcodeSource.UNKNOWN)
                ))
                .build(),
                UNIT_ID, FORMATTED_ID
            ),
            ImmutableList.of(
                new AltSkuDTO()
                    .storerkey(UNIT_ID.getVendorId().toString())
                    .sku(FORMATTED_ID)
                    .altsku(FIRST_BARCODE)
                    .type(AltSkuDTOConverter.BARCODE_ALTSKU_TYPE),
                new AltSkuDTO()
                    .storerkey(UNIT_ID.getVendorId().toString())
                    .sku(FORMATTED_ID)
                    .altsku(SECOND_BARCODE)
                    .type(AltSkuDTOConverter.BARCODE_ALTSKU_TYPE)
            ),
            null
        );
    }

    /**
     * У Item'а присутствует ровно 2 одинаковых barcode'а - ожидаем на выходе 1 AltSku в листе.
     */
    private static Arguments fourthScenario() {
        return Arguments.of(
            new ItemMeta(new Item.ItemBuilder(null, null, null)
                .setUnitId(UNIT_ID)
                .setBarcodes(ImmutableList.of(
                    new Barcode(FIRST_BARCODE, "", BarcodeSource.UNKNOWN),
                    new Barcode(FIRST_BARCODE, "", BarcodeSource.UNKNOWN)
                ))
                .build(),
                UNIT_ID, FORMATTED_ID
            ),
            ImmutableList.of(
                new AltSkuDTO()
                    .storerkey(UNIT_ID.getVendorId().toString())
                    .sku(FORMATTED_ID)
                    .altsku(FIRST_BARCODE)
                    .type(AltSkuDTOConverter.BARCODE_ALTSKU_TYPE)
            ),
            null
        );
    }
}
