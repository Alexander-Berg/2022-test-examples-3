package ru.yandex.direct.core.entity.campaign.service.operation;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.InternalDistribCampaign;
import ru.yandex.direct.core.entity.campaign.model.RfCloseByClickType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;
import ru.yandex.direct.validation.defect.CommonDefects;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefects.requiredImpressionRateDueToRfCloseByClick;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalDistribCampaignWithSystemFields;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class RestrictedCampaignsAddOperationInternalDistribCampaignTest {

    private static final BeanFieldPath[] EXCLUDED_FIELDS = {
            newPath(InternalDistribCampaign.AUTOBUDGET_FORECAST_DATE.name()),
            newPath(InternalDistribCampaign.METRIKA_COUNTERS.name()),
            newPath(InternalDistribCampaign.MEANINGFUL_GOALS.name()),
            newPath(InternalDistribCampaign.CREATE_TIME.name()),
            newPath(InternalDistribCampaign.SOURCE.name()),
            newPath(InternalDistribCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalDistribCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalDistribCampaign.METATYPE.name())
    };

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(EXCLUDED_FIELDS)
            .forFields(newPath(InternalDistribCampaign.LAST_CHANGE.name()))
            .useMatcher(approximatelyNow())
            .forFields(newPath(InternalDistribCampaign.ID.name())).useMatcher(notNullValue())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer())
            .forFields(newPath(InternalDistribCampaign.SOURCE.name()))
            .useMatcher(equalTo(CampaignSource.DIRECT))
            .forFields(newPath(InternalDistribCampaign.METATYPE.name()))
            .useMatcher(equalTo(CampaignMetatype.DEFAULT_));

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private Steps steps;

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    private ClientInfo clientInfo;
    private User operator;
    private InternalDistribCampaign originalCampaign;

    @Before
    public void setUp() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        var operatorClientInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN);
        operator = operatorClientInfo.getChiefUserInfo().getUser();

        originalCampaign = defaultInternalDistribCampaignWithSystemFields(clientInfo)
                .withAgencyId(null)
                .withWalletId(null)
                .withClientId(null)
                .withStrategy(null)
                .withEnableCpcHold(null)
                .withHasExtendedGeoTargeting(null)
                .withHasTitleSubstitution(null)
                .withIsServiceRequested(null)
                .withEnableCompanyInfo(null);
    }


    @Test
    public void add_HasValidationError() {
        originalCampaign.setRotationGoalId(null);

        RestrictedCampaignsAddOperation addOperation = getAddOperation(originalCampaign);
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                                field(InternalDistribCampaign.ROTATION_GOAL_ID)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void add_ForbiddenToChangeStrategy() {
        originalCampaign.setStrategy(defaultAutobudgetStrategy());

        RestrictedCampaignsAddOperation addOperation = getAddOperation(originalCampaign);
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                                field(InternalDistribCampaign.STRATEGY)),
                        CommonDefects.isNull()))));
    }

    @Test
    public void add_Success() {
        var operation = getAddOperation(originalCampaign);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getSuccessfulCount()).isEqualTo(1);

        Long campaignId = result.get(0).getResult();

        List<? extends BaseCampaign> dbCampaigns = campaignTypedRepository.getTypedCampaigns(
                clientInfo.getShard(), singletonList(campaignId));

        assertThat(dbCampaigns).hasSize(1);
        assertThat(dbCampaigns.get(0))
                .is(matchedBy(beanDiffer(getExpectedCampaign()).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));

        Long statusClickTrack = testCampaignRepository
                .getCampaignFieldValue(clientInfo.getShard(), campaignId, CAMP_OPTIONS.STATUS_CLICK_TRACK);
        assertThat(statusClickTrack)
                .isEqualTo(1L);
    }

    @Test
    public void add_WithRfCloseByClick_Success() {
        originalCampaign.setRfCloseByClick(RfCloseByClickType.CAMPAIGN);

        var operation = getAddOperation(originalCampaign);
        MassResult<Long> result = operation.prepareAndApply();
        assertThat(result.getSuccessfulCount()).isEqualTo(1);

        Long campaignId = result.get(0).getResult();

        List<? extends BaseCampaign> dbCampaigns = campaignTypedRepository.getTypedCampaigns(
                clientInfo.getShard(), singletonList(campaignId));

        var expectedCampaign = getExpectedCampaign()
                .withRfCloseByClick(RfCloseByClickType.CAMPAIGN);

        assertThat(dbCampaigns).hasSize(1);
        assertThat(dbCampaigns.get(0))
                .is(matchedBy(beanDiffer(expectedCampaign).useCompareStrategy(CAMPAIGN_COMPARE_STRATEGY)));
    }

    @Test
    public void add_WithRfCloseByClick_HasValidationError() {
        originalCampaign
                .withRfCloseByClick(RfCloseByClickType.CAMPAIGN)
                .withImpressionRateCount(null)
                .withImpressionRateIntervalDays(null);

        var operation = getAddOperation(originalCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                                field(InternalDistribCampaign.IMPRESSION_RATE_COUNT)),
                        requiredImpressionRateDueToRfCloseByClick()))));
    }

    @Test
    public void add_WithImpressionRateIntervalDaysEq101_HasValidationError() {
        originalCampaign
                .withImpressionRateIntervalDays(101);

        var operation = getAddOperation(originalCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors().size()).isEqualTo(1);
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                                field(InternalDistribCampaign.IMPRESSION_RATE_INTERVAL_DAYS)),
                        MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX))));
    }

    @Test
    public void add_WithImpressionRateIntervalDaysEq100_NoValidationError() {
        originalCampaign
                .withImpressionRateIntervalDays(100);

        var operation = getAddOperation(originalCampaign);
        MassResult<Long> result = operation.prepareAndApply();

        assertThat(result.getSuccessfulCount()).isEqualTo(1);
    }

    private RestrictedCampaignsAddOperation getAddOperation(InternalDistribCampaign originalCampaign) {
        return campaignOperationService.createRestrictedCampaignAddOperation(
                singletonList(originalCampaign), operator.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                new CampaignOptions());
    }

    private InternalDistribCampaign getExpectedCampaign() {
        return defaultInternalDistribCampaignWithSystemFields(clientInfo)
                .withOrderId(0L)
                .withStatusBsSynced(CampaignStatusBsSynced.NO)
                .withStatusModerate(CampaignStatusModerate.READY)
                .withStatusPostModerate(CampaignStatusPostmoderate.NEW)
                .withStatusShow(false)
                .withSumSpent(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ZERO)
                .withStatusActive(false);
    }

}
