package ru.yandex.direct.grid.processing.service.offer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import graphql.ExecutionResult;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.jooq.Select;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.grid.core.entity.offer.model.GdiOffer;
import ru.yandex.direct.grid.core.frontdb.steps.FilterShortcutsSteps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.offer.GdOfferFilter;
import ru.yandex.direct.grid.processing.model.offer.GdOffersContainer;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.ytcomponents.service.OfferStatDynContextProvider;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.stub.MetrikaClientStub.buildCounter;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.defaultGdiOffer;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.defaultGdiOfferStats;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.getDefaultGdOffersContainer;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.noEcommerceGdiOfferStats;
import static ru.yandex.direct.grid.processing.util.OfferTestDataUtils.toOffersRowset;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@GridProcessingTest
@RunWith(JUnitParamsRunner.class)
public class OfferGraphQlServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final Long COUNTER_ID = 1L;

    private static final String QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    offers(input: %s) {\n"
            + "      statsColumnFlags {\n"
            + "        hasRevenue\n"
            + "        hasCrr\n"
            + "        hasCarts\n"
            + "        hasPurchases\n"
            + "        hasAvgProductPrice\n"
            + "        hasAvgPurchaseRevenue\n"
            + "      }\n"
            + "      rowset {\n"
            + "        id {\n"
            + "          businessId\n"
            + "          shopId\n"
            + "          offerYabsId\n"
            + "        }\n"
            + "        url\n"
            + "        name\n"
            + "        imageUrl\n"
            + "        price\n"
            + "        currencyIsoCode\n"
            + "        stats {\n"
            + "          shows\n"
            + "          clicks\n"
            + "          ctr\n"
            + "          cost\n"
            + "          costWithTax\n"
            + "          revenue\n"
            + "          crr\n"
            + "          carts\n"
            + "          purchases\n"
            + "          avgClickCost\n"
            + "          avgProductPrice\n"
            + "          avgPurchaseRevenue\n"
            + "          autobudgetGoals\n"
            + "          meaningfulGoals\n"
            + "        }\n"
            + "      }\n"
            + "      filter {\n"
            + "        campaignIdIn\n"
            + "        adGroupIdIn\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final String METRIKA_STAT_URL_QUERY_TEMPLATE = ""
            + "{\n"
            + "  client(searchBy: {login: \"%s\"}) {\n"
            + "    offers(input: %s) {\n"
            + "      rowset {\n"
            + "        stats {\n"
            + "          metrikaStatUrlByCounterId\n"
            + "        }\n"
            + "      }\n"
            + "    }\n"
            + "  }\n"
            + "}\n";

    private static final Map<String, Boolean> FULL_STATS_COLUMN_FLAGS = Map.of(
            "hasRevenue", true,
            "hasCrr", true,
            "hasCarts", true,
            "hasPurchases", true,
            "hasAvgProductPrice", true,
            "hasAvgPurchaseRevenue", true
    );

    private static final Map<String, Boolean> NO_REVENUE_STATS_COLUMN_FLAGS = Map.of(
            "hasRevenue", false,
            "hasCrr", false,
            "hasCarts", true,
            "hasPurchases", true,
            "hasAvgProductPrice", true,
            "hasAvgPurchaseRevenue", false
    );

    private static final Map<String, Boolean> NO_ECOMMERCE_STATS_COLUMN_FLAGS = Map.of(
            "hasRevenue", false,
            "hasCrr", false,
            "hasCarts", false,
            "hasPurchases", false,
            "hasAvgProductPrice", false,
            "hasAvgPurchaseRevenue", false
    );

    private static final DefaultCompareStrategy EXCLUDE_FILTER_COMPARE_STRATEGY =
            allFieldsExcept(newPath("client", "offers", "filter"));

    private ClientInfo otherClientInfo;
    private PerformanceAdGroupInfo adGroupInfo;
    private GdiOffer offer;
    private GdOffersContainer offersContainer;
    private GridGraphQLContext context;

    @Autowired
    private GridContextProvider gridContextProvider;

    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;

    @Autowired
    private Steps steps;
    @Autowired
    private UserService userService;

    @Autowired
    private FilterShortcutsSteps filterShortcutsSteps;

    @Autowired
    private OfferStatDynContextProvider dynContextProvider;

    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    private AutoCloseable mocks;

    @Before
    public void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        otherClientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();

        campMetrikaCountersRepository.updateMetrikaCounters(adGroupInfo.getShard(), Map.of(adGroupInfo.getCampaignId(),
                List.of(new MetrikaCounter().withId(COUNTER_ID))));

        offer = defaultGdiOffer()
                .withStats(defaultGdiOfferStats());
        YtDynamicContext ytDynamicContext = mock(YtDynamicContext.class);
        when(ytDynamicContext.executeSelect(any(Select.class))).thenAnswer(invocation ->
                toOffersRowset(List.of(offer), adGroupInfo.getOrderId()));
        doReturn(ytDynamicContext).when(dynContextProvider).getContext();

        offersContainer = getDefaultGdOffersContainer()
                .withFilter(new GdOfferFilter()
                        .withCampaignIdIn(Set.of(adGroupInfo.getCampaignId()))
                        .withAdGroupIdIn(Set.of(adGroupInfo.getAdGroupId())));

        User user = userService.getUser(adGroupInfo.getUid());
        context = ContextHelper.buildContext(user);
        gridContextProvider.setGridContext(context);
    }

    @After
    public void releaseMocks() throws Exception {
        Mockito.reset(dynContextProvider);
        metrikaClientStub.clearUserCounters(adGroupInfo.getUid());
        metrikaClientStub.clearUserCounters(otherClientInfo.getUid());
        mocks.close();
    }

    public static Object[] parameters() {
        return new Object[][]{
                {true},
                {false},
        };
    }

    @Test
    @TestCaseName("use filterKey instead of filter: {0}")
    @Parameters(method = "parameters")
    public void testService(boolean replaceFilterToFilterKey) {
        metrikaClientStub.addUserCounter(adGroupInfo.getUid(), COUNTER_ID.intValue());

        if (replaceFilterToFilterKey) {
            String jsonFilter = JsonUtils.toJson(offersContainer.getFilter());
            String key = filterShortcutsSteps.saveFilter(adGroupInfo.getClientId(), jsonFilter);

            offersContainer.setFilter(null);
            offersContainer.setFilterKey(key);
        }

        ExecutionResult result = doQuery();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "offers", Map.of(
                                "filter", Map.of(
                                        "campaignIdIn", List.of(adGroupInfo.getCampaignId()),
                                        "adGroupIdIn", List.of(adGroupInfo.getAdGroupId())
                                ),
                                "statsColumnFlags", FULL_STATS_COLUMN_FLAGS,
                                "rowset", List.of(getExpectedOffer(offer, false, true))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    @TestCaseName("ecommerce: {0}")
    @Parameters(method = "parameters")
    public void testService_unavailableCounter(boolean ecommerce) {
        metrikaClientStub.addUserCounter(otherClientInfo.getUid(),
                buildCounter(COUNTER_ID.intValue()).withEcommerce(ecommerce));

        ExecutionResult result = doQuery();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "offers", Map.of(
                                // ожидаем наличие ecommerce-столбцов независимо от наличия ecommerce на счетчике,
                                // т.к. там ненулевые значения
                                "statsColumnFlags", NO_REVENUE_STATS_COLUMN_FLAGS,
                                "rowset", List.of(getExpectedOffer(offer, ecommerce, false))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCLUDE_FILTER_COMPARE_STRATEGY)));
    }

    @Test
    @TestCaseName("ecommerce: {0}")
    @Parameters(method = "parameters")
    public void testService_zeroes(boolean ecommerce) {
        metrikaClientStub.addUserCounter(adGroupInfo.getUid(),
                buildCounter(COUNTER_ID.intValue()).withEcommerce(ecommerce));

        offer = defaultGdiOffer()
                .withStats(noEcommerceGdiOfferStats());

        ExecutionResult result = doQuery();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "offers", Map.of(
                                "statsColumnFlags", ecommerce ? FULL_STATS_COLUMN_FLAGS : NO_ECOMMERCE_STATS_COLUMN_FLAGS,
                                "rowset", List.of(getExpectedOffer(offer, ecommerce, true))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCLUDE_FILTER_COMPARE_STRATEGY)));
    }

    @Test
    @TestCaseName("ecommerce: {0}")
    @Parameters(method = "parameters")
    public void testService_zeroes_unavailableCounter(boolean ecommerce) {
        metrikaClientStub.addUserCounter(otherClientInfo.getUid(),
                buildCounter(COUNTER_ID.intValue()).withEcommerce(ecommerce));

        offer = defaultGdiOffer()
                .withStats(noEcommerceGdiOfferStats());

        ExecutionResult result = doQuery();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "offers", Map.of(
                                "statsColumnFlags", ecommerce ? NO_REVENUE_STATS_COLUMN_FLAGS : NO_ECOMMERCE_STATS_COLUMN_FLAGS,
                                "rowset", List.of(getExpectedOffer(offer, ecommerce, false))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCLUDE_FILTER_COMPARE_STRATEGY)));
    }

    @Test
    @TestCaseName("ecommerce: {0}")
    @Parameters(method = "parameters")
    public void testService_nulls(boolean ecommerce) {
        metrikaClientStub.addUserCounter(adGroupInfo.getUid(),
                buildCounter(COUNTER_ID.intValue()).withEcommerce(ecommerce));

        offer = defaultGdiOffer()
                .withName(null)
                .withImageUrl(null)
                .withPrice(null)
                .withCurrencyIsoCode(null)
                .withStats(defaultGdiOfferStats()
                        .withRevenue(null)
                        .withCrr(null)
                        .withCarts(null)
                        .withPurchases(null)
                        .withAvgProductPrice(null)
                        .withAvgPurchaseRevenue(null));

        ExecutionResult result = doQuery();

        Map<String, Object> data = result.getData();
        Map<String, Object> expected = Map.of(
                "client", Map.of(
                        "offers", Map.of(
                                "statsColumnFlags", NO_ECOMMERCE_STATS_COLUMN_FLAGS,
                                "rowset", List.of(getExpectedOffer(offer, ecommerce, true))
                        )
                )
        );
        assertThat(data).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCLUDE_FILTER_COMPARE_STRATEGY)));
    }

    @Test
    public void testService_metrikaStatUrl_withEcommerce() {
        metrikaClientStub.addUserCounter(adGroupInfo.getUid(), buildCounter(COUNTER_ID.intValue()).withEcommerce(true));

        ExecutionResult result = doQuery(METRIKA_STAT_URL_QUERY_TEMPLATE);

        Map<Long, String> metrikaStatUrlByCounterId = getDataValue(result.getData(),
                "client/offers/rowset/0/stats/metrikaStatUrlByCounterId");
        assertSoftly(softly -> {
            softly.assertThat(metrikaStatUrlByCounterId).hasSize(1);
            softly.assertThat(metrikaStatUrlByCounterId.get(COUNTER_ID)).isNotEmpty();
        });
    }

    @Test
    public void testService_metrikaStatUrl_withoutEcommerce() {
        metrikaClientStub.addUserCounter(adGroupInfo.getUid(), COUNTER_ID.intValue());

        ExecutionResult result = doQuery(METRIKA_STAT_URL_QUERY_TEMPLATE);

        Map<Long, String> metrikaStatUrlByCounterId = getDataValue(result.getData(),
                "client/offers/rowset/0/stats/metrikaStatUrlByCounterId");
        assertThat(metrikaStatUrlByCounterId).isEmpty();
    }

    private ExecutionResult doQuery() {
        return doQuery(QUERY_TEMPLATE);
    }

    private ExecutionResult doQuery(String queryTemplate) {
        String query = String.format(queryTemplate, context.getOperator().getLogin(),
                graphQlSerialize(offersContainer));
        ExecutionResult result = processor.processQuery(null, query, null, context);
        assumeThat(result.getErrors(), empty());
        return result;
    }

    private Map<String, Object> getExpectedOffer(GdiOffer offer, boolean ecommerce, boolean withRevenue) {
        return map(
                "id", Map.of(
                        "businessId", offer.getId().getBusinessId().toString(),
                        "shopId", offer.getId().getShopId().toString(),
                        "offerYabsId", offer.getId().getOfferYabsId().toString()
                ),
                "url", offer.getUrl(),
                "name", offer.getName(),
                "imageUrl", offer.getImageUrl(),
                "price", offer.getPrice(),
                "currencyIsoCode", offer.getCurrencyIsoCode(),
                "stats", map(
                        "shows", offer.getStats().getShows().longValue(),
                        "clicks", offer.getStats().getClicks().longValue(),
                        "ctr", offer.getStats().getCtr(),
                        "cost", offer.getStats().getCost(),
                        "costWithTax", offer.getStats().getCostWithTax(),
                        "revenue", withRevenue ? zeroToNull(ecommerce, offer.getStats().getRevenue()) : null,
                        "crr", withRevenue ? zeroToNull(ecommerce, offer.getStats().getCrr()) : null,
                        "carts", ifNotNull(zeroToNull(ecommerce, offer.getStats().getCarts()), BigDecimal::longValue),
                        "purchases", ifNotNull(zeroToNull(ecommerce, offer.getStats().getPurchases()), BigDecimal::longValue),
                        "avgClickCost", offer.getStats().getAvgClickCost(),
                        "avgProductPrice", zeroToNull(ecommerce, offer.getStats().getAvgProductPrice()),
                        "avgPurchaseRevenue", withRevenue ? zeroToNull(ecommerce, offer.getStats().getAvgPurchaseRevenue()) : null,
                        "autobudgetGoals", offer.getStats().getAutobudgetGoals().longValue(),
                        "meaningfulGoals", offer.getStats().getMeaningfulGoals().longValue()
                )
        );
    }

    @Nullable
    private BigDecimal zeroToNull(boolean ecommerce, @Nullable BigDecimal value) {
        if (!ecommerce && value != null && value.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return value;
    }
}
