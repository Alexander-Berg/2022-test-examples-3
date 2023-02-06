package ru.yandex.direct.core.entity.goal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.bannercategoriesmultik.client.BannerCategoriesMultikClient;
import ru.yandex.direct.bannercategoriesmultik.client.model.BannerCategories;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesRequest;
import ru.yandex.direct.bannercategoriesmultik.client.model.CategoriesResponse;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.currency.service.CurrencyRateService;
import ru.yandex.direct.core.entity.goal.repository.ConversionPriceForecastRepository;
import ru.yandex.direct.core.entity.goal.repository.MetrikaConversionAdGoalsRepository;
import ru.yandex.direct.core.entity.goal.repository.PriceWithClicks;
import ru.yandex.direct.core.entity.goal.service.CampaignConversionPriceForGoalsWithCategoryCpaSource;
import ru.yandex.direct.core.entity.goal.service.ConversionPriceForecastService;
import ru.yandex.direct.core.entity.goal.service.ConversionPriceForecastServiceGeoHelper;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.currencies.CurrencyRub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.richcontent.RichContentClient;
import ru.yandex.direct.richcontent.model.UrlInfo;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType.ACTION;
import static ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType.NUMBER;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UZBEKISTAN_REGION_ID;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class ConversionPriceForecastServiceTest {

    ConversionPriceForecastService service;

    @Mock
    RichContentClient richContentClient;

    @Mock
    BannerCategoriesMultikClient multikClient;

    @Autowired
    CurrencyRateService currencyRateService;

    @Mock
    ConversionPriceForecastRepository conversionPriceForecastRepository;

    @Mock
    MetrikaConversionAdGoalsRepository metrikaConversionAdGoalsRepository;

    @Mock
    ClientService clientService;

    @Mock
    ConversionPriceForecastServiceGeoHelper geoHelper;

    private static final Long CLIENT_ID_L = 456L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(CLIENT_ID_L);

    private static final Long CAMP_ID = 10000L;
    private static final String URL = "https://market.yandex.ru";
    private static final Map<Long, String> URL_BY_CAMPAIGN_ID = Map.of(CAMP_ID, URL);

    private static final Long CATEGORY_ID = 12345L;
    private static final Long SHOULD_NOT_USE_CATEGORY_ID = 678L;

    private static final Long CLIENT_COUNTRY_ID = RUSSIA_REGION_ID;

    private static final Long GOAL_ID1 = 1L;
    private static final Long GOAL_ID2 = 2L;
    private static final Set<Long> GOALS = Set.of(GOAL_ID1, GOAL_ID2);
    private static final Map<Long, MetrikaCounterGoalType> METRIKA_GOAL_TYPES = Map.of(
            GOAL_ID1, ACTION,
            GOAL_ID2, NUMBER);
    private static final Map<Long, String> GOAL_TYPES = EntryStream.of(METRIKA_GOAL_TYPES)
            .mapValues(goalType -> MetrikaCounterGoalType.toSource(goalType).getLiteral())
            .toMap();

    private static final Integer MODEL_ID = 0;
    private static final Integer SHOULD_NOT_USE_MODEL_ID = 1;

    private final UrlInfo urlInfo = new UrlInfo();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        urlInfo.setUrl(URL);
        urlInfo.setTitle("Easter");
        urlInfo.setDescription("Happy Easter!");

        service = new ConversionPriceForecastService(richContentClient, multikClient, currencyRateService,
                conversionPriceForecastRepository, metrikaConversionAdGoalsRepository,
                clientService, geoHelper);
        doReturn(urlInfo).when(richContentClient).getUrlInfo(URL);
        doReturn(CurrencyRub.getInstance()).when(clientService).getWorkCurrency(CLIENT_ID);
        doReturn(CLIENT_COUNTRY_ID).when(clientService).getCountryRegionIdByClientIdStrict(CLIENT_ID);
        doReturn(METRIKA_GOAL_TYPES).when(metrikaConversionAdGoalsRepository).getMetrikaGoalTypes(GOALS);
        doReturn(new CategoriesResponse(
                List.of(new BannerCategories(
                        Map.of(MODEL_ID, List.of(CATEGORY_ID),
                                SHOULD_NOT_USE_MODEL_ID, List.of(SHOULD_NOT_USE_CATEGORY_ID))))))
                .when(multikClient)
                .getCategories(any(CategoriesRequest.class));
    }

    @Test
    public void shouldReturnEmptyMap_WhenInvalidUrlInfo() {
        doReturn(Map.of(CAMP_ID, List.of(CLIENT_COUNTRY_ID)))
                .when(geoHelper).getCampRegionIds(CLIENT_ID, URL_BY_CAMPAIGN_ID);

        UrlInfo emptyUrlInfo = new UrlInfo();
        urlInfo.setUrl(URL);
        doReturn(emptyUrlInfo).when(richContentClient).getUrlInfo(URL);

        Map<Long, CampaignConversionPriceForGoalsWithCategoryCpaSource> recommendedPriceForCampWithId =
                service.getRecommendedConversionPriceByGoalIds(CLIENT_ID, any(), URL_BY_CAMPAIGN_ID);

        assertThat(recommendedPriceForCampWithId.size()).isOne();
        assertThat(recommendedPriceForCampWithId).containsKey(CAMP_ID);
        assertThat(recommendedPriceForCampWithId.get(CAMP_ID).getPriceByGoalId()).isEqualTo(emptyMap());
    }

    @Test
    public void shouldUseClientCountryIdAsGeo_WhenHighLevelGeoInDefaultClientGeo() {
        doReturn(Map.of(CAMP_ID, List.of(CLIENT_COUNTRY_ID)))
                .when(geoHelper).getCampRegionIds(CLIENT_ID, URL_BY_CAMPAIGN_ID);
        doReturn(Map.of(GOAL_TYPES.get(GOAL_ID1), List.of(new PriceWithClicks(1., 2.))))
                .when(conversionPriceForecastRepository)
                .getPriceWithClicksByGoalTypes(
                        eq(CATEGORY_ID),
                        eq(List.copyOf(GOAL_TYPES.values())),
                        eq(List.of(CLIENT_COUNTRY_ID)));

        var res =
                service.getRecommendedConversionPriceByGoalIds(CLIENT_ID, GOALS, URL_BY_CAMPAIGN_ID);
        assertThat(res.keySet().size()).isOne();
        assertThat(res).containsKey(CAMP_ID);

        var campaignConversionPrices = res.get(CAMP_ID).getPriceByGoalId();
        var expectedPriceByGoalId = Map.of(
                GOAL_ID1, BigDecimal.valueOf(30).setScale(2, RoundingMode.HALF_UP));
        assertThat(campaignConversionPrices).isEqualTo(expectedPriceByGoalId);
    }

    @Test
    public void shouldReturnWeightedPrice_WhenSeveralGeoWithNoCommonAncestor() {
        List<Long> geo = List.of(UZBEKISTAN_REGION_ID, CLIENT_COUNTRY_ID);
        doReturn(Map.of(CAMP_ID, geo))
                .when(geoHelper).getCampRegionIds(CLIENT_ID, URL_BY_CAMPAIGN_ID);
        doReturn(
                Map.of(
                        GOAL_TYPES.get(GOAL_ID1),
                        List.of(new PriceWithClicks(1.4, 2.5),
                                new PriceWithClicks(2.2, 3.3)),
                        GOAL_TYPES.get(GOAL_ID2),
                        List.of(new PriceWithClicks(7.2, 5.4),
                                new PriceWithClicks(8.5, 8.7))))
                .when(conversionPriceForecastRepository)
                .getPriceWithClicksByGoalTypes(
                        eq(CATEGORY_ID),
                        eq(List.copyOf(GOAL_TYPES.values())),
                        eq(geo));

        var res =
                service.getRecommendedConversionPriceByGoalIds(CLIENT_ID, GOALS, URL_BY_CAMPAIGN_ID);
        assertThat(res.keySet().size()).isOne();
        assertThat(res).containsKey(CAMP_ID);

        var campaignConversionPrices = res.get(CAMP_ID).getPriceByGoalId();
        var expectedPriceByGoalIds = Map.of(
                GOAL_ID1, BigDecimal.valueOf(55.66),
                GOAL_ID2, BigDecimal.valueOf(240.07));
        assertThat(campaignConversionPrices).isEqualTo(expectedPriceByGoalIds);
    }
}
