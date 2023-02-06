package ru.yandex.market.reporting.generator.dao;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.reporting.config.IntegrationTestConfig;
import ru.yandex.market.reporting.generator.domain.Domain;
import ru.yandex.market.reporting.generator.domain.OffersCount;
import ru.yandex.market.reporting.generator.domain.OffersCountAverage;
import ru.yandex.market.reporting.generator.domain.ShareByPp;
import ru.yandex.market.reporting.generator.domain.ShopRegion;
import ru.yandex.market.reporting.generator.domain.forecaster.ClickType;
import ru.yandex.market.reporting.generator.domain.forecaster.OfferTmpRecord;
import ru.yandex.market.reporting.generator.service.DictionaryService;
import ru.yandex.market.reporting.generator.service.ForecasterCleanupService;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Ignore("For debug use")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
public class ForecasterYtServiceTest {

    @Inject
    private DictionaryService dictionaryService;
    @Inject
    private ForecasterYtService forecasterYtService;
    @Inject
    private ForecasterCleanupService forecasterCleanupService;
    @Inject
    private YtService ytService;
    @Inject
    private Yt yt;
    @Inject
    private JdbcTemplate yqlTemplate;

    private final String sessionId = "job_20171207_200935_test_user_1";
    private final long regionId = 213L;
    private final long categoryId = 91491L;
    private final LocalDate periodTo = LocalDate.now().minusDays(1L);
    private final LocalDate periodFrom = periodTo.minusDays(7L);
    private List<ShopRegion> shopRegionPairs;
    private Domain domain;
    private LocalDate offersDay;


    //    @PostConstruct
    public void init() {
        domain = dictionaryService.getDomains().get("megafon.ru");
        shopRegionPairs = forecasterYtService.getShopRegionPairs(domain.getShopIdsList(), singletonList(regionId));

        offersDay = forecasterYtService.getLastOffersDay(periodTo.plusDays(1));
    }

    @Test
    public void cleanup() {
        yqlTemplate.query("use hahn; select position from [//home/market/production/mstat/forecaster/sessions-dev/job_20171206_181503_test_user_1/overkill_rank_best_213];", rs -> {
            rs.getLong("position");
        });

//        yt.tables().read(YPath.simple("//home/market/production/mstat/dictionaries/shops/latest"),
//            YTableEntryTypes.YSON, r -> {
//            r.getLong("shop_id");
//            r.getBool("enabled");
//            r.get("")
//            });
//        forecasterCleanupService.dropOldSessions();
    }

    @Test
    public void getLastOffersGeneration() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        assertThat(forecasterYtService.getLastOffersGeneration(yesterday), is(yesterday));
    }

    @Test
    public void createGeoParentIdToCityTable() throws Exception {
        forecasterYtService.createGeoParentIdToCityTable();
    }

    @Test
    public void createAllOffersTable() throws Exception {
        forecasterYtService.createAllOffersTable(sessionId, categoryId,
            forecasterYtService.getLastOffersDay(LocalDate.now().minusDays(1)));
    }

    @Test
    public void createOffersRawTable() throws Exception {
//        forecasterYtService.createOffersRawTable(sessionId, );
    }

    @Test
    public void createHistoryClicksModelsTable() throws Exception {
        forecasterYtService.createHistoryClicksModelsTable(sessionId, categoryId, periodFrom, periodTo, asList(regionId));
    }

    @Test
    public void createOffersTmpTable() throws Exception {
        forecasterYtService.createOffersTmpTable(sessionId, periodTo, periodTo, shopRegionPairs, categoryId);
    }

    @Test
    public void createOverkillRankTable() {
        forecasterYtService.createOverkillRankBestTable(sessionId, 145121L, categoryId, regionId,
            LocalDate.parse("2017-11-26"));
        List<OfferTmpRecord> tmpOffers = forecasterYtService.getTmpOffers(sessionId, categoryId, regionId);
    }

    @Test
    public void getTmpOffers() {
        List<OfferTmpRecord> tmpOffers = forecasterYtService.getTmpOffers(sessionId, categoryId, regionId);
    }

    @Test
    public void getOffersWithClicksCount() {
        Map<ClickType, OffersCount> offersWithClicksCount = forecasterYtService.getOffersWithClicksCount(sessionId, shopRegionPairs);
    }

    @Test
    public void getOffersCountAverage() {
        Map<ClickType, OffersCountAverage> offersCountAverage = forecasterYtService.getOffersCountAverage(
            sessionId,
            shopRegionPairs,
            categoryId,
            offersDay);
    }

    @Test
    public void getShopOrdersPpSharePair() {
        Map<ClickType, ShareByPp> shopOrdersPpSharePair = forecasterYtService.getShopOrdersPpSharePair(
            sessionId,
            shopRegionPairs,
            categoryId,
            periodFrom,
            periodTo);
    }

    @Test
    public void countForecastData() throws ExecutionException, InterruptedException {
        forecasterYtService.createAllOffersTable(sessionId, categoryId, offersDay);

//        Forecast forecast = forecasterYtService.countForecastData(sessionId, shopRegionPairs, asList(categoryId), periodFrom, periodTo, offersDay, true);
//
//        forecasterYtService.saveStats(forecast, LocalDateTime.now(), LocalDateTime.now(), 7);
    }

    @Test
    public void getTtlOrdersPpShare() {
        ShareByPp ttlOrdersPpShare = forecasterYtService.getTtlOrdersPpShare(sessionId, categoryId, periodFrom, periodTo);
    }

    @Test
    public void createClicksWithOffersTables() throws Exception {
//        forecasterYtService.createAllOffersTable(sessionId, categoryId, offersDay);
//        forecasterYtService.createCpcClicksWithOffersTable(sessionId, shops(shopRegionPairs), periodFrom, periodTo, categoryId);
        forecasterYtService.createOrderDictTable(sessionId, categoryId, LocalDate.parse("2017-12-06").minusDays(7), LocalDate.parse("2017-12-06"));
//        forecasterYtService.createCpaClicksWithOffersTable(sessionId, asList(213L),
//            LocalDate.parse("2017-12-06").minusDays(7), LocalDate.parse("2017-12-06"), LocalDate.parse("2017-12-06"), categoryId);
//        forecasterYtService.createCardClicksOverkillResultTable(sessionId);

//        getCurrentForecasts(shopRegionPairs);
    }

    @Test
    public void getCurrentForecasts() {
        forecasterYtService.getCurrentForecasts(sessionId, shopRegionPairs, periodFrom, periodTo, categoryId);
    }

    @Test
    public void getShopRegionPairs() throws Exception {
        assertThat(shopRegionPairs, contains(new ShopRegion(178776L, regionId, true)));
    }

//    @Test
//    public void getForecast() {
//        forecasterYtService.getForecast("job_20171204_112932_test_user_1", shopRegionPairs,
//            "card_clicks_overkill_result", Function.identity(), Function.identity(), null);
//    }

    @Test
    public void getTopVendors() throws Exception {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        Map<Long, String> topVendors = forecasterYtService.getTopVendors(sessionId, 5,
            forecasterYtService.getShopRegionPairs(asList(145121L, 178776L), asList(regionId)), 10470548,
            yesterday.minusDays(7),
            yesterday,
            offersDay
        );

        assertThat(topVendors.size(), is(5));
    }

}