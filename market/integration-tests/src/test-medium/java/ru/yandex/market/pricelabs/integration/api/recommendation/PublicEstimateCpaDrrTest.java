package ru.yandex.market.pricelabs.integration.api.recommendation;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApiInterfaces;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyDrr;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.generated.server.pub.model.RecommendationItem;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.recommendation.FeeRecommendation;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.FilterClass;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.model.types.RecommendationType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pricelabs.integration.api.AbstractApiTests.checkResponse;

public class PublicEstimateCpaDrrTest extends BaseRecommendationTest {

    private final AutostrategyTarget target = AutostrategyTarget.blue;

    @Autowired
    private PublicAutostrategiesApi publicApiBean;
    private PublicAutostrategiesApiInterfaces publicApi;

    @BeforeEach
    public void init() {
        publicApi = MockMvcProxy.buildProxy(PublicAutostrategiesApiInterfaces.class, publicApiBean);
        super.init(AutostrategyTarget.blue);
        executors.feeRecommendations().clearTargetTable();
    }

    @Test
    void testRecommendationsQueryAlongWithAutostrategies() {
        autostrategiesExecutor.insert(List.of());
        long drr = 456L;
        // рекомендации для стратегии с ID 3
        for (int i = 0; i < 4; i++) {
            AutostrategySave auto = getAutostrategySave("Name_" + drr, drr, RecommendationType.DEFAULT);
            publicApi.autostrategyPost(SHOP1, auto, target.name());
        }
        List<AutostrategyLoad> responseBody = checkResponse(publicApi.autostrategiesGet(SHOP1, target.name()));
        assertFalse(responseBody.isEmpty());

        Map<Integer, AutostrategySettingsCPA> settingsByAutostrategies =
                responseBody.stream().collect(Collectors.toMap(AutostrategyLoad::getId, v -> v.getSettings().getCpa()));

        assertCpaSettings(drr, null, null, settingsByAutostrategies.get(1));
        assertCpaSettings(drr, null, null, settingsByAutostrategies.get(2));
        assertCpaSettings(drr, 5500L, 4100L, settingsByAutostrategies.get(3));
        assertCpaSettings(drr, null, null, settingsByAutostrategies.get(4));
    }

    private void assertCpaSettings(Long drr, Long maxDrr, Long optDrr, AutostrategySettingsCPA settings) {
        assertNotNull(settings);
        assertEquals(drr, settings.getDrrBid());
        //assertEquals(maxDrr, settings.getMaxDrrBid());
        //assertEquals(optDrr, settings.getOptimumDrrBid());
    }

    @Test
    void testEstimateCpaDrrOffersFromSameCategoryWithOptAndMaxDrr() {
        // берем офферы из одной категории
        List<Offer> offersInCategory = offersSampleShop1GroupedByCategories().get(CATEGORY1);
        List<String> offerIds = List.of(offersInCategory.get(0).getOffer_id(), offersInCategory.get(1).getOffer_id());

        double category1Drr = getBlueBidsRecommendations().get(CATEGORY1).getCpa_rec_bid();
        double expectedDrr = (category1Drr * offerIds.size()) / offerIds.size();

        AutostrategyDrr actual = checkResponse(
                publicApi.autostrategyEstimateCpaDrrPost(
                        SHOP1,
                        autostrategyFilter(offerIds),
                        target.name(),
                        false,
                        null
                )
        );

        assertEquals(expectedDrr * 100 /*33*/, actual.getDrr());
    }

    @Test
    void testEstimateCpaDrrFee() {
        executors.feeRecommendations().insert(List.of(
                new FeeRecommendation(SHOP1, "0", false, 1000, 1000, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "1", false, 800, 2000, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "2", false, 700, 3000, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "3", false, 500, 2500, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "4", false, 9999, 9999, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "5", false, 300, 1500, getInstant(), getInstant()),
                new FeeRecommendation(SHOP1, "6", false, 9999, 9999, getInstant(), getInstant())
        ));
        shop.setShop_id(SHOP1);
        var autostrategy = createAutostrategy(3, "Auto3", 1, RecommendationType.DEFAULT);
        filterExecutor.insert(List.of(getFilter(autostrategy)));
        createAutostrategy(4, "Auto4", 2, RecommendationType.DEFAULT);
        AutostrategyDrr actual = checkResponse(
                publicApi.autostrategyEstimateCpaDrrPost(
                        (int) shop.getShop_id(),
                        autostrategyFilter(List.of("0", "1", "2", "3", "4", "5", "6")),
                        target.name(),
                        true,
                        3L
                )
        );

        Assertions.assertThat(actual.getRecommendations())
                .containsExactlyInAnyOrder(
                        create(5, 60),
                        create(7, 85),
                        create(9, 100)
                );
    }

    private Filter getFilter(Autostrategy autostrategy) {
        Filter filter = new Filter();
        filter.setShop_id((int) autostrategy.getShop_id());
        filter.setFilter_class(FilterClass.BLUE_AUTOSTRATEGY);
        filter.setFilter_id(1);
        autostrategy.setFilter_id(filter.getFilter_id());
        autostrategy.setFilter_type(FilterType.SIMPLE);
        return filter;
    }

    @Nonnull
    private RecommendationItem create(double fee, int percentile) {
        RecommendationItem recommendationItem = new RecommendationItem();

        recommendationItem.setFee(fee);
        recommendationItem.setPercentile(percentile);

        return recommendationItem;
    }
}
