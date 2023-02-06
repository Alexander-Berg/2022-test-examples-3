package ru.yandex.market.adv.b2bmonetization.programs.service.autocreate;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.FeeRecommendation;
import ru.yandex.market.adv.b2bmonetization.programs.model.PartnerColor;
import ru.yandex.market.adv.b2bmonetization.programs.model.autocreate.CampaignCreationData;
import ru.yandex.market.adv.b2bmonetization.programs.service.autocreate.collector.AutostrategyCollector;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestCategory;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@ParametersAreNonnullByDefault
@RequiredArgsConstructor
public abstract class AbstractAutostrategyCollectorTest extends AbstractMonetizationTest {

    private final PartnerColor color;

    protected static final String DS = "" + new DecimalFormat().getDecimalFormatSymbols().getDecimalSeparator();

    @Autowired
    private AutostrategyCollector autostrategyCollector;

    @DisplayName("Получили рекомендации с группировкой по категориям. По рекомендациям fee.")
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
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_groupCategoryWithFeeRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_blue_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_groupCategoryWithFeeRecommendations_autostrategiesList.before.json"
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
    void getAutostrategies_groupCategoryWithFeeRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(612, Set.of(3L),
                                        "Готовая кампания на категорию Категория 3"),
                                new CampaignCreationData(777, Set.of(4L),
                                        "Готовая кампания на категорию Категория 4"),
                                new CampaignCreationData(801, Set.of(5L),
                                        "Готовая кампания на категорию Категория 5"),
                                new CampaignCreationData(1184, Set.of(2L),
                                        "Готовая кампания на категорию Категория 2"),
                                new CampaignCreationData(1617, Set.of(1L),
                                        "Готовая кампания на категорию Категория 1")
                        )
                )
        );
    }

    @DisplayName("Получили рекомендации с группировкой по ставкам. По рекомендациям fee.")
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
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList.before.json"
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
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_blue_shop_category"
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
    void getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_groupByBidWithFeeRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
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

    @DisplayName("Получили рекомендации с группировкой по категориям. " +
            "По дефолтным категорийным рекомендациям (пустые рекомендации fee).")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_categoryRecommendations_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest/getAutostrategies_categoryRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_categoryRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_categoryRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_categoryRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryRecommendations_autostrategiesList_blue_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_categoryRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryRecommendations_autostrategiesList_fee_recommendation"
            )
    )
    void getAutostrategies_groupByCategoryWithDefaultCategoryRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_categoryRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(520, Set.of(2004L),
                                        "Готовая кампания на категорию Категория 4"),
                                new CampaignCreationData(7000, Set.of(2002L),
                                        "Готовая кампания на категорию Категория 2"),
                                new CampaignCreationData(8000, Set.of(2003L),
                                        "Готовая кампания на категорию Категория 3"),
                                new CampaignCreationData(9000, Set.of(2001L),
                                        "Готовая кампания на категорию Категория 1")
                        )
                )
        );
    }

    @DisplayName("Получили рекомендации с группировкой по категориям. " +
            "По дефолтной рекомендации (пустые fee и дефолтные категорийные рекомендации).")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_categoryEmptyRecommendations_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_categoryEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_categoryEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_categoryEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_blue_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_categoryEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_fee_recommendation"
            )
    )
    void getAutostrategies_groupByCategoryWithDefaultRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_categoryEmptyRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(520, Set.of(2001L),
                                        "Готовая кампания на категорию Категория 1"),
                                new CampaignCreationData(520, Set.of(2002L),
                                        "Готовая кампания на категорию Категория 2"),
                                new CampaignCreationData(520, Set.of(2003L),
                                        "Готовая кампания на категорию Категория 3"),
                                new CampaignCreationData(520, Set.of(2004L),
                                        "Готовая кампания на категорию Категория 4")
                        )
                )
        );
    }

    @DisplayName("Получили рекомендации с группировкой по ставкам. " +
            "По дефолтным категорийным рекомендациям (пустые рекомендации fee).")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_bidRecommendations_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest/getAutostrategies_bidRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_bidRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest/getAutostrategies_bidRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_bidRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest/getAutostrategies_bidRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_bidRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidRecommendations_autostrategiesList_blue_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_bidRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidRecommendations_autostrategiesList_fee_recommendation"
            )
    )
    void getAutostrategies_groupByBidWithDefaultCategoryRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_bidRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(520, Set.of(2004L, 2005L, 2006L),
                                        "Готовая кампания со ставкой 5" + DS + "2%"),
                                new CampaignCreationData(7000, Set.of(2001L, 2002L),
                                        "Готовая кампания со ставкой 70%"),
                                new CampaignCreationData(9000, Set.of(2003L),
                                        "Готовая кампания со ставкой 90%")
                        )
                )
        );
    }

    @DisplayName("Получили рекомендации с группировкой по ставкам. " +
            "По дефолтной рекомендации (пустые fee и дефолтные категорийные рекомендации).")
    @Test
    @DbUnitDataSet(
            before = "AutostrategyCollector/csv/CategoryRecommendations/" +
                    "getAutostrategies_bidEmptyRecommendations_autostrategiesList.before.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest/getAutostrategies_bidEmptyRecommendations_autostrategiesList_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_bidEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidEmptyRecommendations_autostrategiesList_blue_shop_offer"
            ),
            before = "AutostrategyCollector/json/yt/Offer/" +
                    "getAutostrategies_bidEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidEmptyRecommendations_autostrategiesList_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_bidEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidEmptyRecommendations_autostrategiesList_blue_shop_category"
            ),
            before = "AutostrategyCollector/json/yt/Category/" +
                    "getAutostrategies_bidEmptyRecommendations_autostrategiesList.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/adv_unittest" +
                            "/getAutostrategies_bidEmptyRecommendations_autostrategiesList_fee_recommendation"
            )
    )
    void getAutostrategies_groupByBidWithDefaultRecommendations_autostrategiesList() {
        run("adv_unittest/getAutostrategies_bidEmptyRecommendations_autostrategiesList_",
                () -> assertBidCategories(
                        List.of(
                                new CampaignCreationData(520,
                                        Set.of(2001L, 2002L, 2003L, 2004L,
                                                2005L, 2006L),
                                        "Готовая кампания со ставкой 5" + DS + "2%")
                        )
                )
        );
    }

    protected void assertBidCategories(List<CampaignCreationData> expected) {
        Assertions.assertThat(autostrategyCollector.getBidCategories(1L, color))
                .containsExactlyElementsOf(expected);
    }
}
