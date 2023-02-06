package ru.yandex.market.mbi.affiliate.promo.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.loyalty.api.model.PromoDescriptionResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.dao.CatalogCategoriesDao;
import ru.yandex.market.mbi.affiliate.promo.dao.CatalogPromoDao;
import ru.yandex.market.mbi.affiliate.promo.model.Category;
import ru.yandex.market.mbi.affiliate.promo.model.ReportData;
import ru.yandex.market.mbi.affiliate.promo.report.ReportClient;
import ru.yandex.market.mbi.affiliate.promo.yt.CollectedPromoDetailsScanner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class CatalogPromoServiceTest {

    @Autowired
    private CatalogPromoDao dao;

    @Autowired
    private CatalogCategoriesDao catalogCategoriesDao;

    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_service.before.csv",
            after = "catalog_promo_service.report_update.after.csv")
    public void testUpdateFromReport() {
        var reportClient = mock(ReportClient.class);
        when(reportClient.getPromoOfferCategoriesSync("111"))
                .thenReturn(new ReportData(
                        12,
                        Map.of("Косметика", 12),
                        List.of(cat(2323,"Бытовая техника")),
                        null, false));
        when(reportClient.getPromoOfferCategoriesSync("222"))
                .thenReturn(new ReportData(
                        1, Map.of("Товары для спорта", 1),
                        List.of(
                                cat(4545, "Товары для дома"),
                                cat(2323, "Бытовая техника")),
                        null, false));
        when(reportClient.getPromoOfferCategoriesSync("333"))
                .thenReturn(new ReportData(0, Map.of(),
                        List.of(), null, false));

        var service = new CatalogPromoService(null, dao, catalogCategoriesDao,null, reportClient, null, clock);
        service.updateFromReport();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_service.before.csv",
            after = "catalog_promo_service.became_valid_update.after.csv")
    public void testUpdateBecameValidTimestamps() {
        var service = new CatalogPromoService(null, dao, catalogCategoriesDao,
                null, null, null, clock);
        service.updateBecameValidTimestamps();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_service.meta.before.csv",
            after = "catalog_promo_service.meta.merged_in.csv")
    public void testUpdateMetaPromos() {
        var service = new CatalogPromoService(null, dao, catalogCategoriesDao,null, null, null, clock);
        service.updateMetaPromos();
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_service.meta.merged_in.csv"
    )
    public void testGetMergedReportDataForMeta() {
        var service = new CatalogPromoService(null, dao, catalogCategoriesDao,null, null, null, clock);
        Map<String, ReportData> input = Map.of(
                "100010", new ReportData(5, Map.of("Кофе", 5), List.of(cat(88, "Продукты")), null, false),
                "100011", new ReportData(10, Map.of("Чай", 10), List.of(cat(88, "Продукты")), null, false),
                "100020", new ReportData(15, Map.of("Книги", 15), List.of(cat(99, "Хобби")), null, false),
                "100021", new ReportData(20, Map.of("Настольные игры", 20), List.of(cat(99, "Хобби")), null, false)
        );
        var result = service.getMergedReportDataForMetaPromos(input);
        assertThat(result.size()).isEqualTo(2);
        var coffeeTea = result.get("995587069");
        assertThat(coffeeTea.getNumOffers()).isEqualTo(15);
        assertThat(coffeeTea.getCategoriesForDescriptionWithNumOffers())
                .containsExactlyInAnyOrderEntriesOf(Map.of("Кофе", 5, "Чай", 10));
        assertThat(coffeeTea.getCategoriesForFilter()).containsExactlyInAnyOrder(
                cat(88, "Продукты"));
        var hobbies = result.get("798487037");
        assertThat(hobbies.getNumOffers()).isEqualTo(35);
        assertThat(hobbies.getCategoriesForDescriptionWithNumOffers())
                .containsExactlyInAnyOrderEntriesOf(Map.of("Книги", 15, "Настольные игры", 20));
        assertThat(hobbies.getCategoriesForFilter()).containsExactlyInAnyOrder(
                cat(99, "Хобби"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "catalog_promo_service.update_landings.before.csv",
            after = "catalog_promo_service.update_landings.after.csv"
    )
    @SuppressWarnings("unchecked")
    public void testUpdateLandings() {
        MarketLoyaltyClient loyaltyClientMock = mock(MarketLoyaltyClient.class);
        when(loyaltyClientMock.getPromoByShopPromoId("111"))
                .thenReturn(loyaltyResponse("111","abbc", "http://market.yandex.ru/landing/abbc-l"));
        when(loyaltyClientMock.getPromoByShopPromoId("112"))
                .thenReturn(loyaltyResponse("112","abbd", "http://market.yandex.ru/landing/abbc-l"));
        when(loyaltyClientMock.getPromoByShopPromoId("222"))
                .thenReturn(loyaltyResponse("222","effg", "http://market.yandex.ru/landing/effg-l"));

        CollectedPromoDetailsScanner collectedPromoDetailsScannerMock = mock(CollectedPromoDetailsScanner.class);
        ArgumentCaptor<Map<?, ?>> scannerArg = ArgumentCaptor.forClass(Map.class);
        when(collectedPromoDetailsScannerMock.fetchNumOffers((Map<String, String>) scannerArg.capture()))
            .thenReturn(Map.of("111", 2, "112", 3, "222", 0));

        var service = new CatalogPromoService(
                null, dao, catalogCategoriesDao, loyaltyClientMock, null, collectedPromoDetailsScannerMock, clock);
        service.updateLandingsAndNumOffersAndReloadMetas();

        assertThat((Set<String>) scannerArg.getValue().keySet()).containsExactlyInAnyOrder("abbc", "abbd", "effg");
    }

    private static Category cat(int hid, String name) {
        return new Category(hid, name, null);
    }

    private static PromoDescriptionResponse loyaltyResponse(String shopPromoId, String promoKey, String landing) {
        return new PromoDescriptionResponse(null, null, shopPromoId, promoKey, null, null, null, null, null, 0, 0L, landing, null, null);
    }
}