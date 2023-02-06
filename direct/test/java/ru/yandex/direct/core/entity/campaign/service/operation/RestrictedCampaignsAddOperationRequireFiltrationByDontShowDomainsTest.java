package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.operation.RestrictedCampaignsUpdateOperationRequireFiltrationByDontShowDomainsTest.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsAddOperationRequireFiltrationByDontShowDomainsTest {

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public RbacService rbacService;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    private GoalUtilsService goalUtilsService;

    private int shard;
    private ClientId clientId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.userSteps().createDefaultUser().getClientInfo();

        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
    }

    // Text campaign

    @Test
    public void apply_CampaignWithFieldValueTrue_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(true);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, true);
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(false);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(null);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithFieldValueTrue_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(true);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), invalidValue())));
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(false);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FeatureIsOff_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        TextCampaign textCampaign = defaultTextCampaign().withRequireFiltrationByDontShowDomains(null);
        MassResult<Long> result = apply(textCampaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    // Content promotion campaign

    @Test
    public void apply_CampaignWithFieldValueTrue_FieldForbiddenForCampaign_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(true);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), isNull())));
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FieldForbiddenForCampaign_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(false);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FieldForbiddenForCampaign_FeatureIsOn_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(null);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithFieldValueTrue_FieldForbiddenForCampaign_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(true);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), isNull())));
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FieldForbiddenForCampaign_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(false);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FieldForbiddenForCampaign_FeatureIsOff_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        CommonCampaign campaign = defaultContentPromotionCampaign()
                .withRequireFiltrationByDontShowDomains(null);
        MassResult<Long> result = apply(campaign);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(result, false);
    }

    private void checkRequireFiltrationByDontShowDomains(MassResult<Long> result, boolean expectedFieldValue) {
        Long campaignId = result.get(0).getResult();
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId));

        assumeThat(campaigns, hasSize(1));
        CommonCampaign campaign = (CommonCampaign) campaigns.get(0);

        assertThat(campaign.getRequireFiltrationByDontShowDomains(), is(expectedFieldValue));
    }

    private MassResult<Long> apply(CommonCampaign campaign) {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(campaign),
                clientInfo.getShard(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                clientInfo.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);
        return addOperation.prepareAndApply();
    }
}
