package ru.yandex.market.billing.marketing.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.marketing.model.MarketingCampaign;
import ru.yandex.market.billing.marketing.model.PromoOrderItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.marketing.MarketingCampaignType;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.tax.model.VatRate;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class PromoOrderItemDaoTest extends FunctionalTest {
    private static final DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .appendLiteral(' ')
            .append(ISO_LOCAL_TIME)
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    @Autowired
    PromoOrderItemDao promoOrderItemDao;

    @Autowired
    MarketingCampaignDao marketingCampaignDao;

    @Test
    @DisplayName("Получение ID промозаказов, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemsByDate() {
        assertThat(promoOrderItemDao.getItemIdsForDeliveryDate(LocalDate.parse("2021-06-15"), true))
                .containsExactlyInAnyOrder(11L, 12L, 13L, 14L);
    }

    @Test
    @DisplayName("Получение ID промозаказов, доставленных в заданную дату из временной таблицы")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.fetchPromoOrderItemsByDateFromTmpTable.before.csv")
    void fetchPromoOrderItemsByDateFromTmpTable() {
        assertThat(promoOrderItemDao.getItemIdsForDeliveryDate(LocalDate.parse("2021-06-15"), false))
                .containsExactlyInAnyOrder(11L, 12L, 13L, 14L);
        assertThat(promoOrderItemDao.getItemIdsForDeliveryDate(LocalDate.parse("2021-06-15"), true))
                .isEmpty();
    }

    @Test
    @DisplayName("Получение ID промозаказов для одного item_id, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchSamePromoOrderItemsByDate() {
        assertThat(promoOrderItemDao.getItemIdsForDeliveryDate(LocalDate.parse("2021-06-20"), true))
                .containsExactlyInAnyOrder(15L);
    }

    @Test
    @DisplayName("Получение ID промозаказов в рамках одной маркетинговой кампании, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemsForSameCampaignByDate() {
        assertThat(promoOrderItemDao.getItemIdsForDeliveryDate(LocalDate.parse("2021-07-01"), true))
                .containsExactlyInAnyOrder(16L, 17L);
    }

    @Test
    @DisplayName("Получение маркетинговых кампаний промозаказов, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemMarketingCampaignsByDate() {
        LocalDate date = LocalDate.parse("2021-06-15");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(
                marketingCampaignDao.getCampaignsByAnaplanIds(
                                promoOrderItemDao.getOrderCampaignAnaplanIdsByItemIds(date, itemIds, true)
                        ).stream()
                        .map(MarketingCampaign::getId)
                        .collect(Collectors.toList())
        ).containsExactlyInAnyOrder(201L, 202L, 203L, 204L);
    }

    @Test
    @DisplayName("Получение маркетинговых кампаний несколькоих промозаказов для одного item_id, " +
            "доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchSamePromoOrderItemMarketingCampaignsByDate() {
        LocalDate date = LocalDate.parse("2021-06-20");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(marketingCampaignDao.getCampaignsByAnaplanIds(
                                promoOrderItemDao.getOrderCampaignAnaplanIdsByItemIds(date, itemIds, true)
                        )
                        .stream()
                        .map(MarketingCampaign::getId)
                        .collect(Collectors.toList())
        ).containsExactlyInAnyOrder(205L, 207L);
    }

    @Test
    @DisplayName("Получение маркетинговых кампаний промозаказов, доставленных в заданную дату, " +
            "для одной маркетинговой кампании")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemSameMarketingCampaignsByDate() {
        LocalDate date = LocalDate.parse("2021-07-01");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(marketingCampaignDao.getCampaignsByAnaplanIds(
                                promoOrderItemDao.getOrderCampaignAnaplanIdsByItemIds(date, itemIds, true)
                        ).stream()
                        .map(MarketingCampaign::getId)
                        .collect(Collectors.toList())
        ).containsExactlyInAnyOrder(208L);
    }

    @Test
    @DisplayName("Получение только промозаказов по ID, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemByIdsAndDate() {
        Instant dateTime = Instant.from(DATETIME_FORMATTER.parse("2021-06-15 00:00:00.00"));

        var expectedPromoOrderItems = List.of(
                PromoOrderItem.builder()
                        .setOrderId(4L).setItemId(11L).setPartnerId(103L).setAnaplanId(1003L)
                        .setPromoType(MarketingCampaignType.CHEAPEST_AS_GIFT)
                        .setCashbackAmount(0L).setDiscountAmount(13000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_0)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(3L).setItemId(12L).setPartnerId(102L).setAnaplanId(1002L)
                        .setPromoType(MarketingCampaignType.BLUE_SET)
                        .setCashbackAmount(0L).setDiscountAmount(12000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_10_110)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(5L).setItemId(13L).setPartnerId(104L).setAnaplanId(1004L)
                        .setPromoType(MarketingCampaignType.MARKET_COUPON)
                        .setCashbackAmount(0L).setDiscountAmount(14000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.NO_VAT)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(2L).setItemId(14L).setPartnerId(101L).setAnaplanId(1001L)
                        .setPromoType(MarketingCampaignType.GENERIC_BUNDLE)
                        .setCashbackAmount(0L).setDiscountAmount(11000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_18_118)
                        .setDeliveredDateTime(dateTime)
                        .build()
        );

        LocalDate date = LocalDate.parse("2021-06-15");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(promoOrderItemDao.getPromoOrderItemsForDeliveryDate(date, itemIds, true))
                .usingRecursiveComparison().isEqualTo(expectedPromoOrderItems);
    }

    @Test
    @DisplayName("Получение нескольких промозаказов для одного ID, доставленных в заданную дату")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchSamePromoOrderItemByIdsAndDate() {
        Instant dateTime = Instant.from(DATETIME_FORMATTER.parse("2021-06-20 00:00:00.00"));

        var expectedPromoOrderItems = List.of(
                PromoOrderItem.builder()
                        .setOrderId(6L).setItemId(15L).setPartnerId(105L).setAnaplanId(1005L)
                        .setPromoType(MarketingCampaignType.MARKET_COIN)
                        .setCashbackAmount(0L).setDiscountAmount(15000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_20)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(8L).setItemId(15L).setPartnerId(107L).setAnaplanId(1007L)
                        .setPromoType(MarketingCampaignType.MARKET_COUPON)
                        .setCashbackAmount(0L).setDiscountAmount(17000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_20)
                        .setDeliveredDateTime(dateTime)
                        .build()
        );

        LocalDate date = LocalDate.parse("2021-06-20");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(promoOrderItemDao.getPromoOrderItemsForDeliveryDate(date, itemIds, true))
                .usingRecursiveComparison().isEqualTo(expectedPromoOrderItems);
    }

    @Test
    @DisplayName("Получение промозаказов, доставленных в заданную дату, для одной маркетинговой кампании")
    @DbUnitDataSet(before = "PromoOrderItemDaoTest.before.csv")
    void fetchPromoOrderItemFoxSameCampaingByIdsAndDate() {
        Instant dateTime = Instant.from(DATETIME_FORMATTER.parse("2021-07-01 00:00:00.00"));

        var expectedPromoOrderItems = List.of(
                PromoOrderItem.builder()
                        .setOrderId(9L).setItemId(16L).setPartnerId(108L).setAnaplanId(1008L)
                        .setPromoType(MarketingCampaignType.MARKET_COUPON)
                        .setCashbackAmount(0L).setDiscountAmount(18000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_20)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(10L).setItemId(17L).setPartnerId(108L).setAnaplanId(1008L)
                        .setPromoType(MarketingCampaignType.MARKET_COUPON)
                        .setCashbackAmount(0L).setDiscountAmount(19000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_20)
                        .setDeliveredDateTime(dateTime)
                        .build()
        );

        LocalDate date = LocalDate.parse("2021-07-01");
        List<Long> itemIds = promoOrderItemDao.getItemIdsForDeliveryDate(date, true);
        assertThat(promoOrderItemDao.getPromoOrderItemsForDeliveryDate(date, itemIds, true))
                .usingRecursiveComparison().isEqualTo(expectedPromoOrderItems);
    }

    @Test
    @DisplayName("Удаление промозаказов, доставленных в заданную дату")
    @DbUnitDataSet(
            before = "PromoOrderItemDaoTest.before.csv",
            after = "PromoOrderItemDaoTest.delete.after.csv"
    )
    void deletePromoOrderItems() {
        LocalDate date = LocalDate.parse("2021-06-20");
        promoOrderItemDao.deletePromoOrderItemsImportedAt(date, true);
    }

    @Test
    @DisplayName("Добавление новых промозаказов")
    @DbUnitDataSet(
            before = "PromoOrderItemDaoTest.before.csv",
            after = "PromoOrderItemDaoTest.persist.after.csv"
    )
    void persistPromoOrderItems() {
        Instant dateTime = Instant.from(DATETIME_FORMATTER.parse("2021-07-02 12:00:00.00"));

        var promoOrderItems = List.of(
                PromoOrderItem.builder()
                        .setOrderId(11L).setItemId(18L).setPartnerId(101L).setAnaplanId(1001L)
                        .setPromoType(MarketingCampaignType.GENERIC_BUNDLE)
                        .setCashbackAmount(0L).setDiscountAmount(21000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_18_118)
                        .setDeliveredDateTime(dateTime)
                        .build(),
                PromoOrderItem.builder()
                        .setOrderId(12L).setItemId(19L).setPartnerId(102L).setAnaplanId(1002L)
                        .setPromoType(MarketingCampaignType.BLUE_SET)
                        .setCashbackAmount(0L).setDiscountAmount(22000L)
                        .setItemCount(1)
                        .setStatus(MbiOrderStatus.DELIVERED).setVatType(VatRate.VAT_10_110)
                        .setDeliveredDateTime(dateTime)
                        .build()
        );

        promoOrderItemDao.persistPromoOrderItems(promoOrderItems, true);
    }
}
