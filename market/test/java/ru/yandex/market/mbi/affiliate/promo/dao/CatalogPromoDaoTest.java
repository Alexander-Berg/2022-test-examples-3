package ru.yandex.market.mbi.affiliate.promo.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.CatalogPromoType;
import ru.yandex.market.mbi.affiliate.promo.api.server.dto.DeviceType;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.model.CatalogPromoEntity;
import ru.yandex.market.mbi.affiliate.promo.model.ReportData;
import ru.yandex.market.mbi.affiliate.promo.service.CatalogPromoService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class CatalogPromoDaoTest {
    @Autowired
    private CatalogPromoDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testGetAllValid() {
        List<CatalogPromoEntity> result = dao.getAllValid(3, 2, new CatalogPromoService.Filters(), null, null);
        assertThat(result)
                .usingElementComparatorOnFields(
                        "id", "description", "promoType", "discountValuePercent", "discountValueRub",
                        "promocodeValue", "cashbackValue", "numOffers", "deviceType")
                .containsExactlyInAnyOrder(
                        new CatalogPromoEntity()
                                .setId("128")
                                .setDescription("Промокод на сноуборд 5%")
                                .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                                .setDiscountValuePercent(5)
                                .setPromocodeValue("sb-sale")
                                .setNumOffers(54)
                                .setDeviceType(DeviceType.APPLICATION),
                        new CatalogPromoEntity()
                                .setId("129")
                                .setDescription("Кэшбэк на снегоходы 30")
                                .setPromoType(CatalogPromoType.CASHBACK)
                                .setCashbackValue(30)
                                .setPromocodeValue(null)
                                .setNumOffers(11),
                        new CatalogPromoEntity()
                                .setId("130")
                                .setDescription("Минус 100р на тюбинги по промокоду")
                                .setPromoType(CatalogPromoType.FIXED_PROMOCODE)
                                .setDiscountValueRub(100)
                                .setPromocodeValue("tubing-promo")
                                .setNumOffers(2));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testGetFilteredByType() {
        List<CatalogPromoEntity> result = dao.getAllValid(5, 0,
                new CatalogPromoService.Filters()
                        .setTypes(List.of(CatalogPromoType.DISCOUNT, CatalogPromoType.N_IS_N_PLUS_1)),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId))
                .containsExactlyInAnyOrder("123", "125");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testSortingAsc() {
        List<CatalogPromoEntity> result =
                dao.getAllValid(20, 0, new CatalogPromoService.Filters(), null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId))
                .containsExactlyInAnyOrder("123", "125", "128", "129", "130", "133");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testSortingDesc() {
        List<CatalogPromoEntity> result =
                dao.getAllValid(20, 0, new CatalogPromoService.Filters(), "end_date", SortingOrder.DESC);
        assertThat(result.stream().map(CatalogPromoEntity::getId))
                .containsExactlyInAnyOrder("125", "128", "129", "130", "133", "123");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testCreatedFrom() {
        List<CatalogPromoEntity> result = dao.getAllValid(
                3, 0,
                new CatalogPromoService.Filters().setCreatedDateFrom(LocalDate.parse("2022-02-15")),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId))
                .containsExactlyInAnyOrder("128", "130", "133");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testCategoryFilter() {
        List<CatalogPromoEntity> result = dao.getAllValid(
                4, 0,
                new CatalogPromoService.Filters().setCategoryHids(List.of(2323L, 9990L, 24L)),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId))
                .containsExactlyInAnyOrder("123", "125", "133");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testPromocodeValueFilter() {
        List<CatalogPromoEntity> result = dao.getAllValid(
                3, 0,
                new CatalogPromoService.Filters().setSearchSubstring("sb-sal"),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId)).containsExactlyInAnyOrder("128");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testDescriptionValueFilter() {
        List<CatalogPromoEntity> result = dao.getAllValid(
                3, 0,
                new CatalogPromoService.Filters().setSearchSubstring("тюбинги"),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId)).containsExactlyInAnyOrder("130");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testRegionalFilter() {
        List<CatalogPromoEntity> result = dao.getAllValid(
                3, 0,
                new CatalogPromoService.Filters().setHideRegionalPromos(true),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId)).containsExactlyInAnyOrder("125", "128", "129");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testTopPromoFilter() {
        List<CatalogPromoEntity> result = dao.getAllValid(10, 0,
                new CatalogPromoService.Filters().setShowOnlyTopPromos(true),
                null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId)).containsExactly("133");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_after.csv")
    public void testUpdateFromStorage() {
        List<CatalogPromoEntity> toMerge = List.of(
                new CatalogPromoEntity()
                        .setId("201")
                        .setPromoType(CatalogPromoType.DISCOUNT)
                        .setDiscountValueRub(800)
                        .setEndDate(LocalDate.of(2022, 2, 28)),
                new CatalogPromoEntity()
                        .setId("124")
                        .setLandingUrl("http://market.yandex.ru/special-ski-skate")
                        .setPromoType(CatalogPromoType.DISCOUNT)
                        .setDiscountValuePercent(12)
                        .setEndDate(LocalDate.of(2021, 11, 30)),
                new CatalogPromoEntity()
                        .setId("128")
                        .setLandingUrl("http://market.yandex.ru/special-snowboard")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setEndDate(LocalDate.of(2021, 11, 30))
                        .setPromocodeValue("sb-sale")
                        .setDiscountValuePercent(5)
                        .setBucketMinPrice(7000)
                        .setBucketMaxPrice(200000)
                        .setOneOrderPromocode(false)
        );
        dao.mergeInAllNewAndExisting(toMerge, false);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_landings_after.csv")
    public void testUpdateLandingUrls() {
        dao.updateLandingUrls(Map.of(
                "123", "http://market.yandex.ru/special/ski-skate-2022",
                "124", "http://market.yandex.ru/special/sledge-2022"));
    }

    @Test
    @DbUnitDataSet(
            dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_set_became_valid_after.csv"
    )
    public void testSetBecameValidTimestamp() {
        dao.setBecameValidTimestamp(List.of("123", "124"), LocalDateTime.parse("2022-02-16T12:24:00"));
        dao.setBecameValidTimestamp(List.of("125"), null);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "catalog_promo_dao_before.csv")
    public void testGetAllIds() {
        List<String> result = dao.getAllIds(null);
        assertThat(result).containsExactlyInAnyOrder(
                "123", "124", "125", "126", "127", "128", "129", "130", "131", "132", "133", "280");
    }


    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_force_skip_after.csv")
    public void testSetForceSkip() {
        dao.setForceSkip(List.of("124", "129"), true);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_reload_after.csv")
    public void testReload() {
        dao.reload(List.of(
                new CatalogPromoEntity()
                        .setId("201")
                        .setPromoType(CatalogPromoType.DISCOUNT)
                        .setDiscountValueRub(800)
                        .setEndDate(LocalDate.of(2022, 2, 28))
                        .setDescription("Best discount")
                        .setForceSkipPromo(true)
        ));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv")
    public void testHasMoreData() {
        assertFalse(dao.hasData(3, new CatalogPromoService.Filters().setTypes(List.of(CatalogPromoType.DISCOUNT))));
        assertFalse(dao.hasData(100, new CatalogPromoService.Filters()));
        assertTrue(dao.hasData(1, new CatalogPromoService.Filters().setTypes(List.of(CatalogPromoType.DISCOUNT))));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_description_after.csv")
    public void testUpdateDescriptionsById() {
        dao.updateDescriptionsById(Map.of(
                "128", new ReportData(6,
                        Map.of("Игрушки для детей до 3 лет", 6), List.of(), null, false),
                "132", new ReportData(5,
                        Map.of("Чай", 1), List.of(), "Чай Ахмад в пакетиках", false)));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_num_offers_after.csv")
    public void testUpdateNumOffers() {
        dao.updateNumOffers(Map.of("128", 6, "132", 1, "133", 5));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_landings_metas_before.csv",
            after = "catalog_promo_dao_landings_metas_after.csv")
    public void testUpdateLandingsSetMeta() {
        dao.updateLandingUrls(Map.of(
                "100010", "http://someurl.me",
                "100011", "http://someurl.me",
                "100020", "http://someurl2.me",
                "100021", "http://someurl2.me",
                "100", "http://someurl3.me"
                ));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_metas_before.csv")
    public void testGetPromosHavingMeta() {
        var result = dao.getPromosHavingMetaPromo();
        assertThat(result).containsExactlyInAnyOrder(
                new CatalogPromoEntity()
                        .setId("100010")
                        .setMetaId("995587069")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setStartDate(LocalDate.of(2022, 4, 1))
                        .setEndDate(LocalDate.of(2022, 4, 30))
                        .setDescription("Кофе")
                        .setLandingUrl("http://someurl.me")
                        .setMetaPromo(false)
                        .setNumOffers(5)
                        .setRegionalPromo(false),
                new CatalogPromoEntity()
                        .setId("100011")
                        .setMetaId("995587069")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setStartDate(LocalDate.of(2022, 4, 1))
                        .setEndDate(LocalDate.of(2022, 4, 30))
                        .setDescription("Чай")
                        .setLandingUrl("http://someurl.me")
                        .setMetaPromo(false)
                        .setNumOffers(10)
                        .setRegionalPromo(false),
                new CatalogPromoEntity()
                        .setId("100020")
                        .setMetaId("798487037")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setStartDate(LocalDate.of(2022, 4, 1))
                        .setEndDate(LocalDate.of(2022, 4, 30))
                        .setDescription("Книги")
                        .setLandingUrl("http://someurl2.me")
                        .setMetaPromo(false)
                        .setNumOffers(15)
                        .setRegionalPromo(false),
                new CatalogPromoEntity()
                        .setId("100021")
                        .setMetaId("798487037")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setStartDate(LocalDate.of(2022, 4, 1))
                        .setEndDate(LocalDate.of(2022, 4, 30))
                        .setDescription("Настольные игры")
                        .setLandingUrl("http://someurl2.me")
                        .setMetaPromo(false)
                        .setNumOffers(20)
                        .setRegionalPromo(false)
        );
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_metas_before.csv")
    public void testGetAllIdsWithMetaFilter() {
        var nonMeta = dao.getAllIds(false);
            assertThat(nonMeta).containsExactlyInAnyOrder("100010", "100011", "100020", "100021", "100", "101");
        var meta = dao.getAllIds(true);
        assertThat(meta).containsExactlyInAnyOrder("798487037", "995587069");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_metas_merge_before.csv",
            after = "catalog_promo_dao_metas_merge_after.csv")
    public void testMergeMetaPromos() {
        List<CatalogPromoEntity> input = List.of(
                new CatalogPromoEntity()
                    .setId("798487037")
                    .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                    .setStartDate(LocalDate.of(2022, 4, 1))
                    .setEndDate(LocalDate.of(2022, 4, 30))
                    .setDescription("Книги и настольные игры")
                    .setLandingUrl("http://someurl2.me")
                    .setMetaPromo(true),
                new CatalogPromoEntity()
                        .setId("995587069")
                        .setPromoType(CatalogPromoType.PERCENT_PROMOCODE)
                        .setStartDate(LocalDate.of(2022, 4, 1))
                        .setEndDate(LocalDate.of(2022, 4, 30))
                        .setDescription("Кофе и чай")
                        .setLandingUrl("http://someurl1.me")
                        .setMetaPromo(true));
        dao.mergeInAllNewAndExisting(input, true);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_metas_before.csv")
    public void testGetAllValidWithMeta() {
        var result = dao.getAllValid(1000, 0, new CatalogPromoService.Filters(), null, null);
        assertThat(result.stream().map(CatalogPromoEntity::getId).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("100", "995587069", "798487037");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_update_regionals_after.csv"
    )
    public void testUpdateIsRegionalPromo() {
        dao.updateRegionalPromosFlags(Map.of("123", false, "124", false, "125", true, "126", true));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_dao_before.csv",
            after = "catalog_promo_dao_top_promo_after.csv"
    )
    public void testUpdateTopPromo() {
        dao.setTopPromo(List.of("124", "127"), true);
    }
}
