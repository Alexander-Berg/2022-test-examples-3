package ru.yandex.travel.hotels.searcher.services.cache.travelline.availability;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventory;
import ru.yandex.travel.hotels.common.partners.travelline.model.HotelInventoryResponse;
import ru.yandex.travel.hotels.proto.TInventoryItem;
import ru.yandex.travel.hotels.proto.TInventoryList;

import static org.assertj.core.api.Assertions.assertThat;

public class HelperTests {
    @Test
    public void testMatchByDateAddToEmptyCache() {
        TInventoryList cached = TInventoryList.newBuilder().build();
        var actual = new HotelInventoryResponse(List.of(
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 1))
                        .version(1)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 2))
                        .version(1)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 3))
                        .version(1)
                        .build()));
        List<TInventoryItem> updatedItems = new ArrayList<>();
        List<TInventoryItem> addedItems = new ArrayList<>();
        List<TInventoryItem> removedItems = new ArrayList<>();
        List<TInventoryItem> upToDateItems = new ArrayList<>();
        var result = Helpers.matchByDate("", cached, actual, updatedItems, addedItems, removedItems, upToDateItems);
        assertThat(result.getItemsCount()).isEqualTo(3);
        assertThat(addedItems.size()).isEqualTo(3);
        assertThat(removedItems.size()).isEqualTo(0);
        assertThat(updatedItems.size()).isEqualTo(0);
        assertThat(result.getItemsList()).allMatch(i -> i.getVersion() == 1);
    }

    @Test
    public void testMatchByDateRemoveAllFromCache() {
        TInventoryList cached = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-01").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-02").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-03").toEpochDay())
                        .setVersion(1)
                        .build())
                .build();
        var actual = new HotelInventoryResponse(Collections.emptyList());
        List<TInventoryItem> updatedItems = new ArrayList<>();
        List<TInventoryItem> addedItems = new ArrayList<>();
        List<TInventoryItem> removedItems = new ArrayList<>();
        List<TInventoryItem> upToDateItems = new ArrayList<>();
        var result = Helpers.matchByDate("", cached, actual, updatedItems, addedItems, removedItems, upToDateItems);
        assertThat(result.getItemsCount()).isEqualTo(0);
        assertThat(addedItems.size()).isEqualTo(0);
        assertThat(removedItems.size()).isEqualTo(3);
        assertThat(updatedItems.size()).isEqualTo(0);
    }

    @Test
    public void testMatchByDateUpdateAll() {
        TInventoryList cached = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-01").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-02").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-03").toEpochDay())
                        .setVersion(1)
                        .build())
                .build();
        var actual = new HotelInventoryResponse(List.of(
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 1))
                        .version(2)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 2))
                        .version(2)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 3))
                        .version(2)
                        .build()));
        List<TInventoryItem> updatedItems = new ArrayList<>();
        List<TInventoryItem> addedItems = new ArrayList<>();
        List<TInventoryItem> removedItems = new ArrayList<>();
        List<TInventoryItem> upToDateItems = new ArrayList<>();
        var result = Helpers.matchByDate("", cached, actual, updatedItems, addedItems, removedItems, upToDateItems);
        assertThat(result.getItemsCount()).isEqualTo(3);
        assertThat(addedItems.size()).isEqualTo(0);
        assertThat(removedItems.size()).isEqualTo(0);
        assertThat(updatedItems.size()).isEqualTo(3);
        assertThat(result.getItemsList()).allMatch(i -> i.getVersion() == 2);
    }

    @Test
    public void testMatchByDateNothingChanged() {
        TInventoryList cached = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-01").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-02").toEpochDay())
                        .setVersion(1)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-03").toEpochDay())
                        .setVersion(1)
                        .build())
                .build();
        var actual = new HotelInventoryResponse(List.of(
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 1))
                        .version(1)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 2))
                        .version(1)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 3))
                        .version(1)
                        .build()));
        List<TInventoryItem> updatedItems = new ArrayList<>();
        List<TInventoryItem> addedItems = new ArrayList<>();
        List<TInventoryItem> removedItems = new ArrayList<>();
        List<TInventoryItem> upToDateItems = new ArrayList<>();
        var result = Helpers.matchByDate("", cached, actual, updatedItems, addedItems, removedItems, upToDateItems);
        assertThat(result.getItemsCount()).isEqualTo(3);
        assertThat(addedItems.size()).isEqualTo(0);
        assertThat(removedItems.size()).isEqualTo(0);
        assertThat(updatedItems.size()).isEqualTo(0);
        assertThat(result.getItemsList()).allMatch(i -> i.getVersion() == 1);
    }

    @Test
    public void testMatchByDateCombined() {
        TInventoryList cached = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-01").toEpochDay())
                        .setVersion(2)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-02").toEpochDay())
                        .setVersion(2)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(LocalDate.parse("2019-10-03").toEpochDay())
                        .setVersion(2)
                        .build())
                .build();
        var actual = new HotelInventoryResponse(List.of(
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 9, 30))
                        .version(100)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 1))
                        .version(1)
                        .build(),
                HotelInventory.builder()
                        .date(LocalDate.of(2019, 10, 2))
                        .version(3)
                        .build()));
        List<TInventoryItem> updatedItems = new ArrayList<>();
        List<TInventoryItem> addedItems = new ArrayList<>();
        List<TInventoryItem> removedItems = new ArrayList<>();
        List<TInventoryItem> upToDateItems = new ArrayList<>();
        var result = Helpers.matchByDate("", cached, actual, updatedItems, addedItems, removedItems, upToDateItems);
        assertThat(result.getItemsCount()).isEqualTo(3);
        assertThat(result.getItems(2).getVersion()).isEqualTo(3);
        assertThat(result.getItems(1).getVersion()).isEqualTo(2);
        assertThat(addedItems.size()).isEqualTo(1);
        assertThat(addedItems.get(0).getDate()).isEqualTo(LocalDate.parse("2019-09-30").toEpochDay());
        assertThat(addedItems.get(0).getVersion()).isEqualTo(100);
        assertThat(removedItems.size()).isEqualTo(1);
        assertThat(removedItems.get(0).getDate()).isEqualTo(LocalDate.parse("2019-10-03").toEpochDay());
        assertThat(removedItems.get(0).getVersion()).isEqualTo(2);
        assertThat(updatedItems.size()).isEqualTo(1);
        assertThat(updatedItems.get(0).getDate()).isEqualTo(LocalDate.parse("2019-10-02").toEpochDay());
        assertThat(updatedItems.get(0).getVersion()).isEqualTo(3);
    }

    @Test
    public void testMayCacheResponseAllIn() {
        long today = LocalDate.now().toEpochDay();
        var inventory = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder().setDate(today).build())
                .addItems(TInventoryItem.newBuilder().setDate(today + 1).build())
                .addItems(TInventoryItem.newBuilder().setDate(today + 2).build())
                .addItems(TInventoryItem.newBuilder().setDate(today + 3).build())
                .build();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today),
                LocalDate.ofEpochDay(today + 1))).isNotNull();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today + 3),
                LocalDate.ofEpochDay(today + 4))).isNotNull();
    }

    @Test
    public void testMayNotCacheResponseOneOut() {
        long today = LocalDate.now().toEpochDay();
        var inventory = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder().setDate(today + 1).build())
                .addItems(TInventoryItem.newBuilder().setDate(today + 3).build())
                .build();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today + 1),
                LocalDate.ofEpochDay(today + 2))).isNotNull();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today + 1),
                LocalDate.ofEpochDay(today + 3))).isNull();
    }

    @Test
    public void testMayNotCacheResponseOutOfbound() {
        long today = LocalDate.now().toEpochDay();
        var inventory = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder().setDate(today).build())
                .addItems(TInventoryItem.newBuilder().setDate(today + 1).build())
                .build();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today + 1),
                LocalDate.ofEpochDay(today + 3))).isNull();
    }

    @Test
    public void testMayNotCacheEmptyInventory() {
        long today = LocalDate.now().toEpochDay();
        var inventory = TInventoryList.newBuilder()
                .build();
        assertThat(Helpers.generateCachedVersions(inventory, LocalDate.ofEpochDay(today),
                LocalDate.ofEpochDay(today + 1))).isNull();
    }

    @Test
    public void testBinSearch() {
        var inventory = TInventoryList.newBuilder()
                .addItems(TInventoryItem.newBuilder()
                        .setDate(1)
                        .setVersion(42)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(2)
                        .setVersion(42)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(4)
                        .setVersion(42)
                        .build())
                .addItems(TInventoryItem.newBuilder()
                        .setDate(6)
                        .setVersion(42)
                        .build())
                .build();
        assertThat(Helpers.findIndexOfFirstMatchingItem(inventory, TInventoryItem::getDate, 4L)).isEqualTo(2);
        assertThat(Helpers.findIndexOfFirstMatchingItem(inventory, TInventoryItem::getDate, 2L)).isEqualTo(1);
        assertThat(Helpers.findIndexOfFirstMatchingItem(inventory, TInventoryItem::getDate, 1L)).isEqualTo(0);
        assertThat(Helpers.findIndexOfFirstMatchingItem(inventory, TInventoryItem::getDate, 6L)).isEqualTo(3);
        assertThat(Helpers.findIndexOfFirstMatchingItem(inventory, TInventoryItem::getDate, 3L)).isEqualTo(-1);
        assertThat(Helpers.findIndexOfFirstMatchingItem(TInventoryList.newBuilder().build(), TInventoryItem::getDate,
                6L)).isEqualTo(-1);
    }
}
