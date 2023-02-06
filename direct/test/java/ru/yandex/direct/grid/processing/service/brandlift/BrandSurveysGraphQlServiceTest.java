package ru.yandex.direct.grid.processing.service.brandlift;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey;
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository;
import ru.yandex.direct.core.entity.campaign.model.BasicUplift;
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStatus;
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBrandSurveyYtRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignBudgetReachDailyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.grid.model.campaign.GdBrandSurveyStatus;
import ru.yandex.direct.grid.model.campaign.GdCampaignBrandSurvey;
import ru.yandex.direct.grid.model.campaign.GdCampaignBrandSurveyInfo;
import ru.yandex.direct.grid.model.campaign.GdSurveyStatus;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.campaign.GdGetBrandSurveys;
import ru.yandex.direct.grid.processing.model.campaign.GdGetBrandSurveysFilter;
import ru.yandex.direct.grid.processing.model.client.GdClient;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.util.ContextHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BrandSurveysGraphQlServiceTest {

    private static final CompareStrategy STRATEGY = allFields();

    @Autowired
    private Steps steps;

    @Autowired
    private BrandSurveyRepository brandSurveyRepository;

    @Autowired
    private CampaignRepository campaignRepository;

    private CampaignBrandSurveyYtRepository brandSurveyYtRepository;

    @Autowired
    private PpcPropertiesSupport ppcPropertySupport;

    @Autowired
    private ShardHelper shardHelper;

    private BrandSurveysInfoService brandSurveysInfoService;

    private GridGraphQLContext context;

    private BrandSurveysGraphQlService brandSurveysGraphQlService;

    private List<GdCampaignBrandSurvey> expected;

    @Autowired
    private CampaignBudgetReachDailyRepository campaignBudgetReachDailyRepository;

    @Autowired
    private FeatureService featureService;

    private CampaignInfo campNotToFilterOut;

    @Before
    public void before() {
        ppcPropertySupport.remove(PpcPropertyNames.BRAND_SURVEY_BUDGET_DATE.getName());
        var clientInfo = steps.clientSteps().createDefaultClient();
        String first = "qwerty123" + clientInfo.getLogin();
        var firstCampaign = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, first);
        String second = "asdfg123" + clientInfo.getLogin();
        var secondCampaign = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, second);
        String third = "zxcvb123" + clientInfo.getLogin();
        var thirdCampaign = steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(clientInfo, third);
        campNotToFilterOut = firstCampaign;
        steps.campaignSteps().setCreateTime(clientInfo.getShard(), firstCampaign.getCampaignId(),
                LocalDateTime.now().minusMonths(2));

        brandSurveyRepository.addBrandSurvey(clientInfo.getShard(),
                new BrandSurvey()
                        .withName("first")
                        .withClientId(clientInfo.getClientId().asLong())
                        .withBrandSurveyId(first)
                        .withRetargetingConditionId(123L));

        brandSurveyRepository.addBrandSurvey(clientInfo.getShard(),
                new BrandSurvey()
                        .withName("second")
                        .withClientId(clientInfo.getClientId().asLong())
                        .withBrandSurveyId(second)
                        .withRetargetingConditionId(123L));

        brandSurveyRepository.addBrandSurvey(clientInfo.getShard(),
                new BrandSurvey()
                        .withName("third")
                        .withClientId(clientInfo.getClientId().asLong())
                        .withBrandSurveyId(third)
                        .withRetargetingConditionId(123L));

        brandSurveyYtRepository = mock(CampaignBrandSurveyYtRepository.class);

        doReturn(Map.of(
                first,
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.DRAFT)
                        .withBasicUplift(new BasicUplift()),
                second,
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.ACTIVE)
                        .withBasicUplift(
                                new BasicUplift()
                                        .withAdRecall(-1.5555555)
                                        .withBrandAwareness(0.0)
                                        .withBrandFavorability(9.25)
                                        .withAdMessageRecall(11.867)
                                        .withProductConsideration(3.345)
                        ),
                third,
                new BrandSurveyStatus()
                        .withSurveyStatusDaily(SurveyStatus.COMPLETED)
                        .withBasicUplift(
                                new BasicUplift()
                                        .withAdRecall(1.6666)
                                        .withBrandAwareness(6.323223)
                                        .withPurchaseIntent(8.9999)
                        )
        )).when(brandSurveyYtRepository).getStatusForBrandSurveys(any());

        context = ContextHelper.buildContext(clientInfo.getChiefUserInfo().getUser())
                .withFetchedFieldsReslover(null);

        brandSurveysInfoService = new BrandSurveysInfoService(brandSurveyRepository, campaignRepository,
                brandSurveyYtRepository, shardHelper, campaignBudgetReachDailyRepository, featureService,
                ppcPropertySupport);

        brandSurveysGraphQlService = new BrandSurveysGraphQlService(brandSurveysInfoService);
        expected = List.of(
                new GdCampaignBrandSurvey()
                        .withName("second")
                        .withBrandSurveyId(second)
                        .withStatus(
                                new GdBrandSurveyStatus()
                                        .withSurveyStatusDaily(GdSurveyStatus.ACTIVE)
                                        .withAdRecall(null)
                                        .withBrandAwareness(null)
                                        .withBrandFavorability(9)
                                        .withAdMessageRecall(12)
                                        .withProductConsideration(3)
                                        .withSumSpentByDay(BigDecimal.ZERO)
                        )
                        .withCampaigns(
                                List.of(
                                        new GdCampaignBrandSurveyInfo()
                                                .withName(secondCampaign.getCampaign().getName())
                                                .withId(secondCampaign.getCampaignId())
                                )),
                new GdCampaignBrandSurvey()
                        .withName("first")
                        .withBrandSurveyId(first)
                        .withStatus(
                                new GdBrandSurveyStatus()
                                        .withSurveyStatusDaily(GdSurveyStatus.DRAFT)
                                        .withSumSpentByDay(BigDecimal.ZERO)
                        )
                        .withCampaigns(
                                List.of(
                                        new GdCampaignBrandSurveyInfo()
                                                .withName(firstCampaign.getCampaign().getName())
                                                .withId(firstCampaign.getCampaignId())
                                )),
                new GdCampaignBrandSurvey()
                        .withName("third")
                        .withBrandSurveyId(third)
                        .withStatus(
                                new GdBrandSurveyStatus()
                                        .withSurveyStatusDaily(GdSurveyStatus.COMPLETED)
                                        .withAdRecall(2)
                                        .withBrandAwareness(6)
                                        .withPurchaseIntent(9)
                                        .withSumSpentByDay(BigDecimal.ZERO)
                        )
                        .withCampaigns(
                                List.of(
                                        new GdCampaignBrandSurveyInfo()
                                                .withName(thirdCampaign.getCampaign().getName())
                                                .withId(thirdCampaign.getCampaignId())
                                ))
        );
    }

    @Test
    public void testServiceSimple() {
        var input = new GdGetBrandSurveys()
                .withFilter(new GdGetBrandSurveysFilter());

        var result = brandSurveysGraphQlService.getBrandSurveys(context,
                new GdClient().withInfo(new GdClientInfo().withId(context.getOperator().getId())), input);
        assertThat(result.getTotalCount()).isEqualTo(3);

        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected).useCompareStrategy(STRATEGY)));

    }

    @Test
    public void testServiceFiltered() {
        var input = new GdGetBrandSurveys()
                .withFilter(new GdGetBrandSurveysFilter().withAddCampMode(true));

        var result = brandSurveysGraphQlService.getBrandSurveys(context,
                new GdClient().withInfo(new GdClientInfo().withId(context.getOperator().getId())), input);

        assertThat(result.getTotalCount()).isEqualTo(2);

        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected.subList(0, 2)).useCompareStrategy(STRATEGY)));

    }

    @Test
    public void testSkipOld() {
        // включаем пропертю brand_survey_budget_date на месяц назад
        // и остатся только одна кампания, у которой дата свежая
        ppcPropertySupport.set(PpcPropertyNames.BRAND_SURVEY_BUDGET_DATE.getName(),
                LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        var input = new GdGetBrandSurveys()
                .withFilter(new GdGetBrandSurveysFilter().withAddCampMode(true));

        var result = brandSurveysGraphQlService.getBrandSurveys(context,
                new GdClient().withInfo(new GdClientInfo().withId(context.getOperator().getId())), input);

        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected.subList(0, 1)).useCompareStrategy(STRATEGY)));
    }

    @Test
    public void testSkipOldButOne() {
        // включаем пропертю brand_survey_budget_date на месяц назад
        // и остатся две кампании - свежая и та, которая передается через input
        ppcPropertySupport.set(PpcPropertyNames.BRAND_SURVEY_BUDGET_DATE.getName(),
                LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        var input = new GdGetBrandSurveys().withCurrentCampaignId(campNotToFilterOut.getCampaignId())
                .withFilter(new GdGetBrandSurveysFilter().withAddCampMode(true));

        var result = brandSurveysGraphQlService.getBrandSurveys(context,
                new GdClient().withInfo(new GdClientInfo().withId(context.getOperator().getId())), input);

        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getRowset()).is(matchedBy(beanDiffer(expected.subList(0, 2)).useCompareStrategy(STRATEGY)));
    }
}
