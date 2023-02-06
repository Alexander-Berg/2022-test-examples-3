package ru.yandex.market.adv.b2bmonetization.programs.service.autocreate;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.FeeRecommendation;
import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerColor;
import ru.yandex.market.adv.b2bmonetization.programs.model.autocreate.CampaignCreationData;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestCategory;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты сервиса AutostrategyCollector для dbs магазинов")
@ParametersAreNonnullByDefault
class DbsAutostrategyCollectorTest extends AbstractAutostrategyCollectorTest {

    DbsAutostrategyCollectorTest() {
        super(PartnerColor.WHITE);
    }

    @DisplayName("Получили рекомендации с группировкой по категориям. По рекомендациям fee. Включая категорию -1.")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_groupByBidWithFeeRecommendationsWithAllCategory_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_groupCategoryWithFeeRecommendationsWithAllCategory_autostrategiesList.before" +
                    ".json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_fee_recommendation"
            ),
            before = "AutostrategyCollector/json/yt/FeeRecommendation/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    void getAutostrategies_groupCategoryWithFeeRecommendationsWithAllCategory_autostrategiesList() {
        run("adv_unittest/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(999, Set.of(-1L),
                                        "Готовая кампания на категорию Все товары"),
                                new CampaignCreationData(612, Set.of(3L),
                                        "Готовая кампания на категорию Категория 3"),
                                new CampaignCreationData(777, Set.of(4L),
                                        "Готовая кампания на категорию Категория 4"),
                                new CampaignCreationData(1184, Set.of(2L),
                                        "Готовая кампания на категорию Категория 2"),
                                new CampaignCreationData(1617, Set.of(1L),
                                        "Готовая кампания на категорию Категория 1")
                        )
                )
        );
    }

    @DisplayName("Получили рекомендации с группировкой по ставкам. По рекомендациям fee. Включая категорию -1.")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_groupByBidWithFeeRecommendationsWithAllCategory_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_groupByBidWithFeeRecommendationsWithAllCategory_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_fee_recommendation"
            ),
            before = "AutostrategyCollector/json/yt/FeeRecommendation/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    void getAutostrategies_groupByBidWithFeeRecommendationsWithAllCategory_autostrategiesList() {
        run("adv_unittest/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(999, Set.of(-1L),
                                        "Готовая кампания со ставкой 9" + DS + "99%"),
                                new CampaignCreationData(612, Set.of(3L),
                                        "Готовая кампания со ставкой 6" + DS + "12%"),
                                new CampaignCreationData(777, Set.of(4L),
                                        "Готовая кампания со ставкой 7" + DS + "77%"),
                                new CampaignCreationData(888, Set.of(5L, 6L),
                                        "Готовая кампания со ставкой 8" + DS + "88%"),
                                new CampaignCreationData(1184, Set.of(2L),
                                        "Готовая кампания со ставкой 11" + DS + "84%"),
                                new CampaignCreationData(1617, Set.of(1L, 7L),
                                        "Готовая кампания со ставкой 16" + DS + "17%")
                        )
                )
        );
    }
}
