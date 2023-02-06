package ru.yandex.direct.grid.processing.service.brandlift;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.BasicUplift;
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStatus;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBrandSurveyYtRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBudgetReachDailyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.grid.model.campaign.GdBrandSurveyStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignBrandSurvey;
import ru.yandex.direct.grid.model.campaign.GdCampaignBrandSurveyInfo;
import ru.yandex.direct.grid.model.campaign.GdSurveyStatus;
import ru.yandex.direct.grid.processing.model.campaign.GdGetBrandSurveys;
import ru.yandex.direct.grid.processing.model.campaign.GdGetBrandSurveysFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.BRAND_SURVEY_BUDGET_DATE;
import static ru.yandex.direct.grid.processing.util.ContextHelper.buildDefaultContext;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
public class BrandSurveysInfoServiceTest {

    private static final CompareStrategy STRATEGY = allFields();

    @Mock
    private BrandSurveyRepository brandSurveyRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignBrandSurveyYtRepository brandSurveyYtRepository;

    @Mock
    private ShardHelper shardHelper;

    private BrandSurveysInfoService brandSurveysInfoService;

    @Mock
    private FeatureService featureService;

    @Mock
    private PpcPropertiesSupport ppcPropertySupport;

    @Mock
    private CampaignBudgetReachDailyRepository campaignBudgetReachDailyRepository;

    private static List<GdCampaignBrandSurvey> expected = List.of(
            new GdCampaignBrandSurvey()
                    .withName("Brand-lift 12412")
                    .withBrandSurveyId("qwerty123")
                    .withStatus(new GdBrandSurveyStatus()
                            .withSurveyStatusDaily(GdSurveyStatus.DRAFT)
                            .withSumSpentByDay(BigDecimal.ZERO)
                    )
                    .withCampaigns(
                            List.of(
                                    new GdCampaignBrandSurveyInfo().withName("firstCampaign").withId(123L),
                                    new GdCampaignBrandSurveyInfo().withName("secondCampaign").withId(978L)
                            )), new GdCampaignBrandSurvey()
                    .withName("Brand-lift 6567")
                    .withBrandSurveyId("asdfg123")
                    .withStatus(new GdBrandSurveyStatus()
                            .withSurveyStatusDaily(GdSurveyStatus.ACTIVE)
                            .withSumSpentByDay(BigDecimal.ZERO)
                    )
                    .withCampaigns(
                            List.of(
                                    new GdCampaignBrandSurveyInfo().withName("sixthCampaign").withId(999L),
                                    new GdCampaignBrandSurveyInfo().withName("thirdCampaign").withId(9786L)
                            )),
            new GdCampaignBrandSurvey()
                    .withName("Brand-lift 12412")
                    .withBrandSurveyId("zxcvb123")
                    .withStatus(new GdBrandSurveyStatus()
                            .withSurveyStatusDaily(GdSurveyStatus.COMPLETED)
                            .withSumSpentByDay(BigDecimal.ZERO)
                    )
                    .withCampaigns(
                            List.of(
                                    new GdCampaignBrandSurveyInfo().withName("fourthCampaign").withId(12345L),
                                    new GdCampaignBrandSurveyInfo().withName("fifthCampaign").withId(1234L)
                            ))
    );

    @Before
    public void before() {
        doReturn(mock(PpcProperty.class)).when(ppcPropertySupport).get(BRAND_SURVEY_BUDGET_DATE);
        Mockito.when(shardHelper.getShardByClientId(any())).thenReturn(1);
        var firstBrandSurvey = new BrandSurvey()
                .withName("Brand-lift 12412")
                .withBrandSurveyId("qwerty123")
                .withClientId(345L)
                .withRetargetingConditionId(123L)
                .withIsBrandLiftHidden(false);
        var secondBrandSurvey = new BrandSurvey()
                .withName("Brand-lift 6567")
                .withBrandSurveyId("asdfg123")
                .withClientId(345L)
                .withRetargetingConditionId(678L)
                .withIsBrandLiftHidden(false);
        var thirdBrandSurvey = new BrandSurvey()
                .withName("Brand-lift 12412")
                .withBrandSurveyId("zxcvb123")
                .withClientId(345L)
                .withRetargetingConditionId(987L)
                .withIsBrandLiftHidden(true);

        doReturn(List.of(firstBrandSurvey, secondBrandSurvey, thirdBrandSurvey))
                .when(brandSurveyRepository).getClientBrandSurveys(anyInt(), anyLong());

        doReturn(Map.of(
                firstBrandSurvey.getBrandSurveyId(),
                List.of(
                        new Campaign()
                                .withName("firstCampaign")
                                .withId(123L),
                        new Campaign()
                                .withName("secondCampaign")
                                .withId(978L)
                ),
                secondBrandSurvey.getBrandSurveyId(),
                List.of(
                        new Campaign()
                                .withName("sixthCampaign")
                                .withId(999L),
                        new Campaign()
                                .withName("thirdCampaign")
                                .withId(9786L)
                ),
                thirdBrandSurvey.getBrandSurveyId(),
                List.of(

                        new Campaign()
                                .withName("fourthCampaign")
                                .withId(12345L),
                        new Campaign()
                                .withName("fifthCampaign")
                                .withId(1234L)
                )
                )
        ).when(campaignRepository).getCampaignsForBrandSurveys(anyInt(), any(), any());

        doReturn(Map.of(
                firstBrandSurvey.getBrandSurveyId(),
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.DRAFT)
                        .withBasicUplift(new BasicUplift()),
                secondBrandSurvey.getBrandSurveyId(),
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.ACTIVE)
                        .withBasicUplift(new BasicUplift()),

                thirdBrandSurvey.getBrandSurveyId(),
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.COMPLETED)
                        .withBasicUplift(new BasicUplift())
                )).when(brandSurveyYtRepository).getStatusForBrandSurveys(any());

        brandSurveysInfoService = new BrandSurveysInfoService(brandSurveyRepository,
                campaignRepository,
                brandSurveyYtRepository,
                shardHelper,
                campaignBudgetReachDailyRepository,
                featureService, ppcPropertySupport);
    }

    @Test
    public void testGetBrandSurveysWithHidden() {
        var context =  buildDefaultContext();
        when(featureService.isEnabled(context.getOperator().getUid(), FeatureName.BRAND_LIFT_HIDDEN))
                .thenReturn(true);
        var result = brandSurveysInfoService
                .getBrandSurveysWithCampaigns(ClientId.fromLong(345L), context,
                        new GdGetBrandSurveys()
                                .withFilter(new GdGetBrandSurveysFilter())
                );

        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected).useCompareStrategy(STRATEGY)));
    }

    @Test
    public void testGetBrandSurveysFiltered() {
        var result = brandSurveysInfoService
                .getBrandSurveysWithCampaigns(ClientId.fromLong(345L), buildDefaultContext(),
                        new GdGetBrandSurveys()
                                .withFilter(new GdGetBrandSurveysFilter().withAddCampMode(true))
                );

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected.subList(0, 2)).useCompareStrategy(STRATEGY)));
    }

    @Test
    public void testGetBrandSurveysWithoutHidden() {
        var result = brandSurveysInfoService
                .getBrandSurveysWithCampaigns(ClientId.fromLong(345L), buildDefaultContext(),
                        new GdGetBrandSurveys()
                                .withFilter(new GdGetBrandSurveysFilter())
                );

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected.subList(0, 2)).useCompareStrategy(STRATEGY)));
    }

}
