package ru.yandex.market.core.moderation.impl;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.moderation.ShopsModeratedSupplier;
import ru.yandex.market.core.moderation.TestingShop;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;

@DbUnitDataSet(before = "DefaultShopsModeratedSupplierFunctionalTest.csv")
class DefaultShopsModeratedSupplierFunctionalTest extends FunctionalTest {
    @Autowired
    private ShopsModeratedSupplier shopsModeratedSupplier;

    /**
     * Тест проверяет, что магазины, находящиеся на проверке с успешными индексациями или с индексациями с
     * предупреждениями, не попадают в выборку магазинов для отмены премодерации.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_doesntReturnValidShops.before.csv")
    void testGetFeedLoadCheckFailedShopIds_doesntReturnValidShops() {
        assertThat(shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(), emptyIterable());
    }

    /**
     * Тест проверяет, что магазины, находящиеся на проверке и завалившие индексацию по причине ошибки
     * скачивания 3 или меньше раз, не попадают в выборку магазинов для отмены премодерации.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_doesntReturnShopsWithThreeDownloadErrors.before.csv")
    void testGetFeedLoadCheckFailedShopIds_doesntReturnShopsWithThreeDownloadErrors() {
        assertThat(shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(), emptyIterable());
    }

    /**
     * Тест проверяет, что магазины, находящиеся на проверке и завалившие индексацию ранее чем было последнее
     * обновление {@code SHOPS_WEB.DATASOURCES_IN_TESTING.UPDATED_AT}, не попадают в выборку магазинов
     * для отмены премодерации.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_doesntReturnShopsWithOldErrors.before.csv")
    void testGetFeedLoadCheckFailedShopIds_doesntReturnShopsWithOldErrors() {
        assertThat(shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(), emptyIterable());
    }

    /**
     * Тест проверяет, что магазины, находящиеся на проверке и завалившие последние 4 и более индексаций по
     * причине ошибки скачивания, попадают в выборку магазинов для отмены премодерации.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_returnShopsWithFourDownloadErrors.before.csv")
    void testGetFeedLoadCheckFailedShopIds_returnShopsWithFourDownloadErrors() {
        assertThat(
                shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(),
                containsInAnyOrder(new TestingShop(101000, 1000), new TestingShop(201000, 1000))
        );
    }

    /**
     * Тест проверяет, что магазины, находящиеся на проверке и завалившие последние 4 и более индексаций по
     * причине ошибки скачивания, которые были ДО запроса отправления магазина в индекс не попадают в выборку
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_doNotReturnShopsWithFourDownloadErrors.before.csv")
    void testGetFeedLoadCheckFailedShopIds_failedBeforeModerationRequest() {
        assertThat(shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(), emptyIterable());
    }

    /**
     * Тест проверяет, что магазины, находящиеся на проверке и завалившие хотя бы одну индексацию по причине
     * ошбики парсинга, попадают в выборку магазинов для отмены премодерации.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckFailedShopIds_returnShopsWithOneParseError.before.csv")
    void testGetFeedLoadCheckFailedShopIds_returnShopsWithOneParseError() {
        assertThat(
                shopsModeratedSupplier.getFeedLoadCheckFailedShopIds(),
                containsInAnyOrder(new TestingShop(101001, 1001))
        );
    }

    /**
     * Проверяет, что в выборку магазинов, прошедших проверку фида попадают и обычные фиды и дефолтные.
     */
    @Test
    @DbUnitDataSet(before = "testGetFeedLoadCheckPassedShopIds.csv")
    void testGetFeedLoadCheckPassedShopIds() {
        assertThat(
                shopsModeratedSupplier.getFeedLoadCheckPassedShopIds(),
                containsInAnyOrder(new TestingShop(101000, 1000), new TestingShop(101001, 1001))
        );
    }

    @Test
    @DisplayName("Проверку загрузки фида могут пройти только те магазины, у которых нет сфейленных фидов. " +
            "(кейс для мультифидовых магазинов)")
    @DbUnitDataSet(before = "testFailedFeedLoadCheckForMultifeeds.csv")
    void testFailedFeedLoadCheckForMultifeeds() {
        final Iterable<TestingShop> actual = shopsModeratedSupplier.getFeedLoadCheckPassedShopIds();
        final List<TestingShop> expected = List.of();

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Для мультифидового магазина должны только один раз переключать статус WAITING_FEED_FIRST_LOAD -> CHECKING." +
            "У магазина оба фида попали в индекс")
    @DbUnitDataSet(before = "testFeedLoadCheckForMultifeeds.csv")
    void testFeedLoadCheckForMultifeeds() {
        final Iterable<TestingShop> actual = shopsModeratedSupplier.getFeedLoadCheckPassedShopIds();

        final List<TestingShop> expected = List.of(
                new TestingShop(101002, 1002)
        );

        Assertions.assertEquals(expected, actual);
    }
}
