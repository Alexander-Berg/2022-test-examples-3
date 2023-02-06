package ru.yandex.market.loyalty.admin.yt;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import ru.yandex.market.loyalty.admin.config.AdminTestConfig;
import ru.yandex.market.loyalty.admin.yt.dao.AnaplanPromoYtDao;
import ru.yandex.market.loyalty.admin.yt.dao.PersonalPromoYtDao;
import ru.yandex.market.loyalty.admin.yt.dao.PromoStorageYtDao;
import ru.yandex.market.loyalty.admin.yt.dao.PromoYtDao;
import ru.yandex.market.loyalty.admin.yt.fallback.YtTableClient;
import ru.yandex.market.loyalty.admin.yt.service.PromoStoragePromoImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter;
import ru.yandex.market.loyalty.admin.yt.service.PromoYtImporter.ImportResult;
import ru.yandex.market.loyalty.admin.yt.source.YtSource;
import ru.yandex.market.loyalty.core.service.personalpromo.PersonalPromoService;
import ru.yandex.market.loyalty.core.test.LoyaltySpringTestRunner;
import ru.yandex.market.loyalty.core.test.SupplementaryDataLoader;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionInAnyOrder;

@Ignore("this test suite should be run manually because it uses real YT")
@RunWith(LoyaltySpringTestRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = AdminTestConfig.class, properties = {
        "market.loyalty.yt.promo.bundles.urls=hahn://home/market/production/indexer/stratocaster/promos/blue/in" +
                "/recent,arnold://home/market/production/indexer/gibson/promos/blue/in/recent",
        "market.loyalty.yt.promo.personal.urls=hahn://home/market/testing/monetize/dynamic_pricing/personal_promo" +
                "/promo_for_index/2021-05-29T02:50:32",
        "market.loyalty.yt.promo.anaplan.urls=arnold://home/market/testing/indexer/stratocaster/promos" +
                "/collected_promo_details/recent",
        "market.loyalty.yt.promo.autosets.urls=arnold://home/market/production/yamarec/master/outlet/autobundles" +
                "/blue1/recent",
        "market.loyalty.yt.promo.promostorage.urls=hahn://home/market/testing/indexer/datacamp/promo/backups/recent," +
                "arnold://home/market/testing/indexer/datacamp/promo/backups/recent",
        "market.loyalty.yt.yql.oauth.token=token"
})
public class PromoYtImporterTest {
    @Autowired
    private PromoYtImporter promoYtImporter;
    @Autowired
    private SupplementaryDataLoader supplementaryDataLoader;
    @Autowired
    private YtTableClient tableClient;
    @Autowired
    private PromoStoragePromoImporter promoImporter;
    @Autowired
    private PersonalPromoService personalPromoService;

    @Before
    public void prepare() {
        supplementaryDataLoader.createTechnicalIfNotExists();
    }

    @Test
    public void shouldLoadTableData() {
        Map<YtSource, ImportResult> importResults = promoYtImporter.importPromos().stream().collect(
                Collectors.toUnmodifiableMap(ImportResult::getYtSource, Function.identity()));


        assertThat(importResults.get(YtSource.FIRST_PARTY_PIPELINE).getImportResults(), not(empty()));

        Map<String, PromoYtImporter.PromoImportResult> promoImportResultMap = importResults.get(
                YtSource.FIRST_PARTY_PIPELINE).getImportResults().stream()
                .collect(Collectors.toUnmodifiableMap(PromoYtImporter.PromoImportResult::getPromoKey,
                        Function.identity()));

        assertThat(promoImportResultMap, not(anEmptyMap()));
    }

    @Test
    public void shouldLoadPersonalPromo() {
        Set<String> activePromoKeys;
        activePromoKeys = personalPromoService.getActivePromoKeys();
        assertTrue(activePromoKeys.isEmpty());
        ImportResult importResult;
        importResult = doPersonalPromoImport();
        assertThat(importResult.getImportResults(), everyItem(hasProperty("valid", is(true))));
        importResult = doPersonalPromoImport();
        assertThat(importResult.getImportResults(), everyItem(hasProperty("valid", is(true))));
        activePromoKeys = personalPromoService.getActivePromoKeys();
        assertTrue(!activePromoKeys.isEmpty());
    }

    @NotNull
    private ImportResult doPersonalPromoImport() {
        ImportResult importResult = promoYtImporter.importPromos(new PersonalPromoYtDao(
                YtSource.PERSONAL_PROMO_PIPELINE,
                () -> YtPath.listOf("hahn://home/market/testing/monetize/dynamic_pricing/personal_promo" +
                        "/promo_for_index/2021-05-29T02:50:32"),
                tableClient
        ), p -> true, p -> promoYtImporter.importSilently(p));
        return importResult;
    }

    @Test
    public void shouldLoadSnapshotTableData() {
        ImportResult importResult = promoYtImporter.importPromos(new PromoYtDao(
                YtSource.ANAPLAN_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/production/indexer/gibson/promos/blue/in/20201211_102037"),
                tableClient
        ), p -> true, p -> new PromoYtImporter.PromoImportResult(p.getPromoKey(), p.getReportPromoType()));

        assertThat(importResult.getImportResults(), everyItem(hasProperty("valid", is(true))));
    }

    @Test
    public void shouldLoadPromoFromPromoStorage() {
        PromoStoragePromoImporter.PromoStorageImportResults promoStorageImportResults =
                promoImporter.importPromos(new PromoStorageYtDao(
                YtSource.PROMO_STORAGE_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/testing/indexer/datacamp/promo/backups/recent"),
                tableClient
        ));

        assertThat(promoStorageImportResults.getImportResults(), not(empty()));
    }

    @Test
    public void shouldEqualsResultsFromTables() {
        ImportResult importResult1 = promoYtImporter.importPromos(new PromoYtDao(
                YtSource.ANAPLAN_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/testing/indexer/stratocaster/promos/blue_on_white" +
                        "/mbi_datacamp_in/recent"),
                tableClient
        ), p -> true, p -> new PromoYtImporter.PromoImportResult(p.getPromoKey(), p.getReportPromoType()));

        ImportResult importResult2 = promoYtImporter.importPromos(new AnaplanPromoYtDao(
                YtSource.ANAPLAN_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/testing/indexer/stratocaster/promos/collected_promo_details" +
                        "/recent"),
                tableClient
        ), p -> true, p -> new PromoYtImporter.PromoImportResult(p.getPromoKey(), p.getReportPromoType()));

        assertThat(
                importResult1.getImportResults(),
                sameCollectionInAnyOrder(importResult2.getImportResults())
        );
    }

    @Test
    public void shouldLoadFastPromos() {
        ImportResult importResult = promoYtImporter.importPromos(new AnaplanPromoYtDao(
                YtSource.FAST_PROMOS_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/testing/indexer/stratocaster/promos/fast_promos/recent"),
                tableClient
        ), p -> true, sf -> new PromoYtImporter.PromoImportResult(sf.getPromoKey(), sf.getReportPromoType()));

        ImportResult importResult2 = promoYtImporter.importPromos(new AnaplanPromoYtDao(
                YtSource.ANAPLAN_PIPELINE,
                () -> YtPath.listOf("arnold://home/market/testing/indexer/stratocaster/promos/collected_promo_details" +
                        "/recent"),
                tableClient
        ), p -> true, p -> new PromoYtImporter.PromoImportResult(p.getPromoKey(), p.getReportPromoType()));

        Set<String> pksFast = importResult.getImportResults().stream()
                .map(PromoYtImporter.PromoImportResult::getPromoKey)
                .collect(Collectors.toSet());
        Set<String> pksUsual = importResult2.getImportResults().stream()
                .map(PromoYtImporter.PromoImportResult::getPromoKey)
                .collect(Collectors.toSet());
        assertThat(pksFast.stream().filter(pksUsual::contains).collect(Collectors.toList()), not(empty()));


        assertThat(importResult.getImportResults(), notNullValue());
        assertThat(importResult.getImportResults().size(), greaterThan(0));
    }
}
