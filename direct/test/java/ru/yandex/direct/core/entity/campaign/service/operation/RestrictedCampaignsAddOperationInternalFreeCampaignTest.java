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
import ru.yandex.direct.core.entity.campaign.model.InternalFreeCampaign;
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
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultInternalFreeCampaignWithSystemFields;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class RestrictedCampaignsAddOperationInternalFreeCampaignTest {

    private static final BeanFieldPath[] EXCLUDED_FIELDS = {
            newPath(InternalFreeCampaign.AUTOBUDGET_FORECAST_DATE.name()),
            newPath(InternalFreeCampaign.METRIKA_COUNTERS.name()),
            newPath(InternalFreeCampaign.MEANINGFUL_GOALS.name()),
            newPath(InternalFreeCampaign.CREATE_TIME.name()),
            newPath(InternalFreeCampaign.SOURCE.name()),
            newPath(InternalFreeCampaign.IS_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalFreeCampaign.IS_PRICE_RECOMMENDATIONS_MANAGEMENT_ENABLED.name()),
            newPath(InternalFreeCampaign.METATYPE.name())
    };

    private static final DefaultCompareStrategy CAMPAIGN_COMPARE_STRATEGY = DefaultCompareStrategies
            .allFieldsExcept(EXCLUDED_FIELDS)
            .forFields(newPath(InternalFreeCampaign.LAST_CHANGE.name()))
            .useMatcher(approximatelyNow())
            .forFields(newPath(InternalFreeCampaign.ID.name())).useMatcher(notNullValue())
            .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer())
            .forFields(newPath(InternalFreeCampaign.SOURCE.name()))
            .useMatcher(equalTo(CampaignSource.DIRECT))
            .forFields(newPath(InternalFreeCampaign.METATYPE.name()))
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
    private InternalFreeCampaign originalCampaign;

    @Before
    public void setUp() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        var operatorClientInfo =
                steps.clientSteps().createDefaultClientWithRoleInAnotherShard(RbacRole.INTERNAL_AD_ADMIN);
        operator = operatorClientInfo.getChiefUserInfo().getUser();

        originalCampaign = defaultInternalFreeCampaignWithSystemFields(clientInfo)
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
        originalCampaign.setRestrictionValue(null);

        RestrictedCampaignsAddOperation addOperation = getAddOperation(originalCampaign);
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(path(index(0),
                        field(InternalFreeCampaign.RESTRICTION_VALUE)),
                        CommonDefects.notNull()))));
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


    private RestrictedCampaignsAddOperation getAddOperation(InternalFreeCampaign originalCampaign) {
        return campaignOperationService.createRestrictedCampaignAddOperation(
                singletonList(originalCampaign), operator.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                new CampaignOptions());
    }

    private InternalFreeCampaign getExpectedCampaign() {
        return defaultInternalFreeCampaignWithSystemFields(clientInfo)
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
