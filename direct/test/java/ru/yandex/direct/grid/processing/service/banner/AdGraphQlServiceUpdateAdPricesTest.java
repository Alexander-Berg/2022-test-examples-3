package ru.yandex.direct.grid.processing.service.banner;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import graphql.ExecutionResult;
import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrice;
import ru.yandex.direct.grid.processing.model.banner.GdAdPriceCurrency;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPayloadItem;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdPrice;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.grid.processing.util.UserHelper;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration.GRAPH_QL_PROCESSOR;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildContext;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.convertValue;
import static ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateAdPricesTest {
    private static final CompareStrategy COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(newPath("lastChange"), newPath("statusBsSynced"), newPath("bannerPrice"));

    private static final String PREVIEW_MUTATION = "updateAdPrices";
    private static final String QUERY_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {"
            + "      id"
            + "    }\n"
            + "  }\n"
            + "}";

    private static final String UPDATE_ADS_MUTATION = "updateAdPrices";
    @Autowired
    public OldBannerRepository bannerRepository;
    @Autowired
    @Qualifier(GRAPH_QL_PROCESSOR)
    private GridGraphQLProcessor processor;
    @Autowired
    private Steps steps;
    private long bannerId;
    private User operator;
    private TextBannerInfo defaultBanner;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        defaultAdGroup = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        defaultBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        bannerId = defaultBanner.getBannerId();
        operator = UserHelper.getUser(clientInfo.getClient());
        TestAuthHelper.setDirectAuthentication(operator);
    }

    @Test
    public void updateOneBannerPrice() {
        GdAdPrice adPrice = new GdAdPrice().withPrice("123.00").withCurrency(GdAdPriceCurrency.RUB);

        String query = createQuery(
                singletonList(new GdUpdateAdPrice().withId(bannerId).withPrice(adPrice))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(defaultBanner.getShard(), bannerId);
        assertThat(actualPrice, notNullValue());
        assertThat(actualPrice.getCurrency(), is(OldBannerPricesCurrency.valueOf(adPrice.getCurrency().name())));
        assertThat(actualPrice.getPrice(), is(new BigDecimal(adPrice.getPrice())));

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(defaultBanner.getShard(), singletonList(bannerId)).get(0);
        assertThat(actualBanner, beanDiffer(defaultBanner.getBanner()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateTwoBannerPrices() {
        TextBannerInfo secondBanner = steps.bannerSteps().createDefaultBanner(defaultAdGroup);
        Long secondBannerId = secondBanner.getBannerId();
        GdAdPrice adPrice = new GdAdPrice().withPrice("123.00").withCurrency(GdAdPriceCurrency.RUB);

        String query = createQuery(
                Arrays.asList(
                        new GdUpdateAdPrice().withId(bannerId).withPrice(adPrice),
                        new GdUpdateAdPrice().withId(secondBannerId).withPrice(adPrice))
        );
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId, secondBannerId);

        for (TextBannerInfo banner : Arrays.asList(defaultBanner, secondBanner)) {
            OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(banner.getShard(), banner.getBannerId());
            assertThat(actualPrice, notNullValue());
            assertThat(actualPrice.getCurrency(), is(OldBannerPricesCurrency.valueOf(adPrice.getCurrency().name())));
            assertThat(actualPrice.getPrice(), is(new BigDecimal(adPrice.getPrice())));
            OldTextBanner actualBanner = (OldTextBanner) bannerRepository
                    .getBanners(banner.getShard(), singletonList(banner.getBannerId())).get(0);
            assertThat(actualBanner, beanDiffer(banner.getBanner()).useCompareStrategy(COMPARE_STRATEGY));
        }
    }

    @Test
    public void deleteBannerPrice() {
        updateOneBannerPrice();

        String query = createQuery(singletonList(new GdUpdateAdPrice().withId(bannerId)));
        Map<String, Object> data = processQueryAndGetResult(query);
        validateUpdateSuccessful(data, bannerId);

        OldBannerPrice actualPrice = steps.bannerPriceSteps().getBannerPrice(defaultBanner.getShard(), bannerId);
        assertThat(actualPrice, nullValue());

        OldTextBanner actualBanner =
                (OldTextBanner) bannerRepository.getBanners(defaultBanner.getShard(), singletonList(bannerId)).get(0);
        assertThat(actualBanner, beanDiffer(defaultBanner.getBanner()).useCompareStrategy(COMPARE_STRATEGY));
    }

    @Test
    public void updateOneBannerPrice_invalidRequest() {
        GdAdPrice adPrice = new GdAdPrice().withPrice("-123.00").withCurrency(GdAdPriceCurrency.RUB);

        String query = createQuery(
                singletonList(new GdUpdateAdPrice().withId(bannerId).withPrice(adPrice))
        );
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors(), hasSize(1));
    }

    private Map<String, Object> processQueryAndGetResult(String query) {
        ExecutionResult result = processor.processQuery(null, query, null, buildContext(operator));
        assertThat(result.getErrors(), empty());
        Map<String, Object> data = result.getData();
        assertThat(data.keySet(), contains(UPDATE_ADS_MUTATION));
        return data;
    }

    private void validateUpdateSuccessful(Map<String, Object> data, Long... bannerId) {
        GdUpdateAdsPayload expectedGdUpdateAdsPayload = new GdUpdateAdsPayload()
                .withUpdatedAds(StreamEx.of(bannerId).map(id -> new GdUpdateAdPayloadItem().withId(id)).toList());

        GdUpdateAdsPayload gdUpdateAdsPayload =
                convertValue(data.get(UPDATE_ADS_MUTATION), GdUpdateAdsPayload.class);

        assertThat(gdUpdateAdsPayload, beanDiffer(expectedGdUpdateAdsPayload));
    }

    private String createQuery(List<GdUpdateAdPrice> prices) {
        return String.format(QUERY_TEMPLATE, PREVIEW_MUTATION, graphQlSerialize(prices));
    }
}
