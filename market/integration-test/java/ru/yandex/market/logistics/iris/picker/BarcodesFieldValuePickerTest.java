package ru.yandex.market.logistics.iris.picker;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.configuration.ReferenceIndexerTestConfiguration;
import ru.yandex.market.logistics.iris.core.domain.item.Item;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.domain.util.ReferenceIndexWithUpdatedDate;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.complex.Barcode;
import ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.field.Value;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.picker.predefined.BarcodesFieldValuePicker;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Import(ReferenceIndexerTestConfiguration.class)
public class BarcodesFieldValuePickerTest {

    private static final Source WH_145 = new Source("145", SourceType.WAREHOUSE);
    private static final Source WH_147 = new Source("147", SourceType.WAREHOUSE);
    private static final Source WH_149 = new Source("149", SourceType.WAREHOUSE);

    private static final ItemIdentifier ITEM_IDENTIFIER = ItemIdentifier.of("id", "sku");

    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;

    private BarcodesFieldValuePicker picker = new BarcodesFieldValuePicker();

    /**
     * Проверяем, что при попытке выбрать barcodes у товара,
     * у которого нет источников справочной информации - в ответ мы получим Optional.empty.
     */
    @Test
    public void pickAmongZeroSources() {
        Item item = new Item(ITEM_IDENTIFIER, ImmutableMap.of());

        Optional<Value<Barcodes>> pick = picker.pick(item);

        assertThat(pick).isEmpty();
    }

    /**
     * Проверяем, что при попытке выбрать barcodes у товара,
     * у которого есть источники справочной информации,
     * но они не обладают информацией о barcodes - в ответ получим Optional.empty.
     */
    @Test
    public void pickBarcodesAmongMultipleSourcesWithoutBarcodes() {
        ChangeTrackingReferenceIndex emptyIndex = referenceIndexer.createEmptyIndex();
        ReferenceIndexWithUpdatedDate emptyIndexWithUpdatedDate =
                new ReferenceIndexWithUpdatedDate(emptyIndex, LocalDateTime.now());

        Item item = new Item(
            ITEM_IDENTIFIER,
            ImmutableMap.of(WH_145, emptyIndexWithUpdatedDate, WH_147, emptyIndexWithUpdatedDate)
        );

        Optional<Value<Barcodes>> pick = picker.pick(item);

        assertThat(pick).isEmpty();
    }

    /**
     * Проверяем, что при попытке выбрать barcodes у товара,
     * у которого есть источник справочной информации с этими полем -
     * результатом будет набор из этого баркода.
     */
    @Test
    public void pickBarcodesFromSingleSourceWithBarcodes() {
        Barcode barcode = new Barcode("1", "1", BarcodeSource.UNKNOWN);

        Item item = new Item(
            ITEM_IDENTIFIER,
            ImmutableMap.of(WH_145, createIndexWithBarcodes(barcode))
        );

        Optional<Value<Barcodes>> pick = picker.pick(item);

        SoftAssertions.assertSoftly(assertions -> {
            assertThat(pick).isPresent();
            pick.ifPresent(value -> {
                assertions.assertThat(value.getValue().getBarcodes()).containsExactlyInAnyOrder(barcode);
            });
        });
    }


    /**
     * Проверяем, что при попытке выбрать barcodes у товара,
     * у которого есть источники справочной информации с этими полями -
     * результатом будет набор из всех возможных баркодов из всех источников.
     */
    @Test
    public void pickBarcodesFromMultipleSouresWithBarcodes() {
        Barcode firstBarcode = new Barcode("1", "1", BarcodeSource.UNKNOWN);
        Barcode secondBarcode = new Barcode("2", "2", BarcodeSource.UNKNOWN);
        Barcode thirdBarcode = new Barcode("3", "3", BarcodeSource.PARTNER);
        Barcode fourthBarcode = new Barcode("4", "4", BarcodeSource.SUPPLIER);

        Item item = new Item(
            ITEM_IDENTIFIER,
            ImmutableMap.of(
                WH_145, createIndexWithBarcodes(firstBarcode, secondBarcode),
                WH_147, createIndexWithBarcodes(thirdBarcode, fourthBarcode),
                WH_149, new ReferenceIndexWithUpdatedDate(referenceIndexer.createEmptyIndex(), LocalDateTime.now()),
                new Source("1", SourceType.ADMIN), createIndexWithBarcodes(firstBarcode, fourthBarcode)
            )
        );

        Optional<Value<Barcodes>> pick = picker.pick(item);

        SoftAssertions.assertSoftly(assertions -> {
            assertThat(pick).isPresent();
            pick.ifPresent(value -> {
                assertions.assertThat(value.getValue().getBarcodes()).containsExactlyInAnyOrder(
                    firstBarcode,
                    secondBarcode,
                    thirdBarcode,
                    fourthBarcode
                );
            });
        });


    }

    private ReferenceIndexWithUpdatedDate createIndexWithBarcodes(Barcode... barcodes) {
        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(PredefinedFields.BARCODES, Barcodes.of(Stream.of(barcodes).collect(Collectors.toList())), ZonedDateTime.now());

        return new ReferenceIndexWithUpdatedDate(index, LocalDateTime.now());
    }
}
