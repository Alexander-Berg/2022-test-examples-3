package ru.yandex.direct.core.entity.campaign.service.operation;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.model.ContentPromotionCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignAdditionalActionsService;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsUpdateOperation;
import ru.yandex.direct.core.entity.campaign.service.type.update.CampaignUpdateOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.UpdateRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeContentPromotionCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
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
public class RestrictedCampaignsUpdateOperationRequireFiltrationByDontShowDomainsTest {

    static final String REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME = "requireFiltrationByDontShowDomains";

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;

    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;

    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private Steps steps;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public MetrikaClientStub metrikaClient;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    private int shard;
    private ClientId clientId;
    private CampaignInfo textCampaignInfo;
    private CampaignInfo contentPromotionCampaignInfo;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        textCampaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(null, null)
                        .withStartTime(LocalDate.now())
                        .withEmail("example@example.com"), clientInfo);

        contentPromotionCampaignInfo = steps.campaignSteps().createCampaign(
                activeContentPromotionCampaign(null, null)
                        .withStrategy(manualStrategy()
                                .withPlatform(CampaignsPlatform.SEARCH))
                        .withStartTime(LocalDate.now())
                        .withEmail("example@example.com"), clientInfo);
    }

    // Text campaign

    @Test
    public void apply_CampaignWithFieldValueTrue_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(textCampaignInfo, true);
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(textCampaignInfo, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, null);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(textCampaignInfo, false);
    }

    @Test
    @Ignore
    public void apply_CampaignWithFieldValueTrue_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), invalidValue())));
    }

    @Test
    @Ignore
    public void apply_CampaignWithFieldValueFalse_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(textCampaignInfo, false);
    }

    @Test
    @Ignore
    public void apply_CampaignWithNoFieldValue_FeatureIsOff_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<TextCampaign> textCampaignModelChanges = ModelChanges.build(
                textCampaignInfo.getCampaignId(), TextCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, null);
        MassResult<Long> result = apply(textCampaignInfo, textCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(textCampaignInfo, false);
    }

    // Content promotion campaign

    @Test
    public void apply_CampaignWithFieldValueTrue_FieldForbiddenForCampaign_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), isNull())));
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FieldForbiddenForCampaign_FeatureIsOn_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(contentPromotionCampaignInfo, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FieldForbiddenForCampaign_FeatureIsOn_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, null);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(contentPromotionCampaignInfo, false);
    }

    @Test
    public void apply_CampaignWithFieldValueTrue_FieldForbiddenForCampaign_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, true);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS_FIELD_NAME)), isNull())));
    }

    @Test
    public void apply_CampaignWithFieldValueFalse_FieldForbiddenForCampaign_FeatureIsOff_ValidationError() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(contentPromotionCampaignInfo, false);
    }

    @Test
    public void apply_CampaignWithNoFieldValue_FieldForbiddenForCampaign_FeatureIsOff_Success() {
        steps.featureSteps().addClientFeature(clientId, FeatureName.CAN_REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, false);

        ModelChanges<ContentPromotionCampaign> contentPromotionCampaignModelChanges = ModelChanges.build(
                contentPromotionCampaignInfo.getCampaignId(), ContentPromotionCampaign.class,
                TextCampaign.REQUIRE_FILTRATION_BY_DONT_SHOW_DOMAINS, null);
        MassResult<Long> result = apply(contentPromotionCampaignInfo, contentPromotionCampaignModelChanges);

        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
        checkRequireFiltrationByDontShowDomains(contentPromotionCampaignInfo, false);
    }

    private MassResult<Long> apply(CampaignInfo campaignInfo, ModelChanges<? extends BaseCampaign> modelChanges) {
        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation restrictedCampaignsUpdateOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                ppcDslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);
        return restrictedCampaignsUpdateOperation.apply();
    }

    private void checkRequireFiltrationByDontShowDomains(CampaignInfo campaignInfo, boolean expectedFieldValue) {
        Long campaignId = campaignInfo.getCampaignId();
        List<? extends BaseCampaign> campaigns = campaignTypedRepository.getTypedCampaigns(shard, List.of(campaignId));

        assumeThat(campaigns, hasSize(1));
        CommonCampaign campaign = (CommonCampaign) campaigns.get(0);

        assertThat(campaign.getRequireFiltrationByDontShowDomains(), is(expectedFieldValue));
    }
}
