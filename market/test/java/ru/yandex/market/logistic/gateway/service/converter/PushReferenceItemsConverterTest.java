package ru.yandex.market.logistic.gateway.service.converter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistic.gateway.BaseTest;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.PushReferenceItemsConverter;
import ru.yandex.market.logistics.iris.client.model.entity.ReferenceItem;
import ru.yandex.market.logistics.iris.core.index.complex.Barcodes;
import ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetime;
import ru.yandex.market.logistics.iris.core.index.complex.VendorCodes;

public class PushReferenceItemsConverterTest extends BaseTest {

    @Test
    public void convertItemListToReferenceItemList() {

        List<ReferenceItem> expected = createExpectedReferenceItemList();
        List<ReferenceItem> actual =
            PushReferenceItemsConverter.convertItemListToReferenceItemList(createItemList(getItem()));

        assertions.assertThat(actual)
            .as("Asserting the result is present")
            .isNotEmpty();
        assertions.assertThat(actual)
            .as("Asserting that converted ReferenceItem list instance is valid")
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(expected);
    }

    @Test
    public void convertSimpleItemListToSimpleReferenceItemList() {

        List<ReferenceItem> expected = createExpectedSimpleReferenceItemList();
        List<ReferenceItem> actual =
            PushReferenceItemsConverter.convertItemListToReferenceItemList(createItemList(getSimpleItem()));

        assertions.assertThat(actual)
            .as("Asserting the result is present")
            .isNotEmpty();
        assertions.assertThat(actual)
            .as("Asserting that converted ReferenceItem list instance is valid")
            .usingRecursiveFieldByFieldElementComparator()
            .isEqualTo(expected);
    }

    private List<ReferenceItem> createExpectedSimpleReferenceItemList() {
        ArrayList<ReferenceItem> referenceItems = new ArrayList<>();
        ReferenceItem item = new ReferenceItem();

        item.new Builder()
            .setName("name")
            .setBarcodes(Barcodes.of(Collections.emptyList()))
            .build();
        referenceItems.add(item);
        return referenceItems;
    }

    private List<ReferenceItem> createExpectedReferenceItemList() {
        ArrayList<ReferenceItem> referenceItems = new ArrayList<>();
        ReferenceItem item = new ReferenceItem();
        OffsetDateTime updatedDateTime = OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 0, ZoneOffset.UTC);
        item.new Builder()
            .setUnitId(
                new ru.yandex.market.logistics.iris.client.model.entity.UnitId("id", 1L, "item"))
            .setName("name")
            .setBarcodes(Barcodes.of(Collections.singletonList(
                new ru.yandex.market.logistics.iris.core.index.complex.Barcode("code", null,
                    ru.yandex.market.logistics.iris.core.index.complex.BarcodeSource.UNKNOWN))))
            .setBoxCapacity(1)
            .setBoxCount(1)
            .setHasLifeTime(true)
            .setVendorCodes(VendorCodes.of(Collections.singletonList("vendorCode")))
            .setKorobyte(new ru.yandex.market.logistics.iris.client.model.entity.Korobyte.KorobyteBuilder()
                .setHeight(BigDecimal.ONE)
                .setLength(BigDecimal.ONE)
                .setWidth(BigDecimal.ONE)
                .setWeightGross(BigDecimal.ONE)
                .setWeightNet(BigDecimal.ONE)
                .setWeightTare(BigDecimal.ONE)
                .build())
            .setLifetime(1)
            .setUpdatedDateTime(updatedDateTime)
            .setInboundRemainingLifetimesDays(
                ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes.of(
                    Collections.singletonList(RemainingLifetime.of(1, updatedDateTime.toInstant().toEpochMilli()))))
            .setInboundRemainingLifetimesPercentage(
                ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes.of(
                    Collections.singletonList(RemainingLifetime.of(2, updatedDateTime.toInstant().toEpochMilli()))))
            .setOutboundRemainingLifetimesDays(
                ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes.of(
                    Collections.singletonList(RemainingLifetime.of(3, updatedDateTime.toInstant().toEpochMilli()))))
            .setOutboundRemainingLifetimesPercentage(
                ru.yandex.market.logistics.iris.core.index.complex.RemainingLifetimes.of(
                    Collections.singletonList(RemainingLifetime.of(4, updatedDateTime.toInstant().toEpochMilli()))))
            .build();

        referenceItems.add(item);
        return referenceItems;
    }

    private List<Item> createItemList(Item item) {
        List<Item> items = new ArrayList<>();
        items.add(item);
        return items;
    }

    @NotNull
    private Item getItem() {
        return new Item.ItemBuilder("name", 2, BigDecimal.ONE)
            .setUnitId(new UnitId("id", 1L, "item"))
            .setArticle("article")
            .setBarcodes(Collections.singletonList(new Barcode.BarcodeBuilder("code").build()))
            .setBoxCapacity(1)
            .setBoxCount(1)
            .setComment("comment")
            .setHasLifeTime(true)
            .setVendorCodes(Collections.singletonList("vendorCode"))
            .setKorobyte(new Korobyte.KorobyteBuiler(1, 1, 1, BigDecimal.ONE)
                .setWeightNet(BigDecimal.ONE)
                .setWeightTare(BigDecimal.ONE)
                .build())
            .setLifeTime(1)
            .setUpdatedDateTime(DateTime.fromOffsetDateTime(OffsetDateTime.of(2000, 1, 1, 1, 1, 1, 0, ZoneOffset.UTC)))
            .setRemainingLifetimes(new RemainingLifetimes(getShelfLives(1, 2), getShelfLives(3, 4)))
            .build();
    }

    @NotNull
    private Item getSimpleItem() {
        return new Item.ItemBuilder("name", 1, BigDecimal.ONE).build();
    }

    @NotNull
    private ShelfLives getShelfLives(int days, int percentage) {
        return new ShelfLives(new ShelfLife(days), new ShelfLife(percentage));
    }
}
