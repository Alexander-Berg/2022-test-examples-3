package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
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
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class RestrictedCampaignsUpdateOperationTextCampaignTest {
    public static final String INVALID_MINUS_KEYWORD = "asd,.as,da.,,21111,.111";
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    private OrganizationsClientStub organizationsClientStub;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private Steps steps;

    @Test
    public void update_HasPreValidationError_ValidationDoNotFails() {
        TextCampaignInfo campaignInfo = steps.textCampaignSteps().createDefaultCampaign();

        ModelChanges<TextCampaign> modelChanges = new ModelChanges<>(campaignInfo.getId(), TextCampaign.class);
        modelChanges.process(new BroadMatch()
                .withBroadMatchLimit(RandomNumberUtils.nextPositiveInteger())
                .withBroadMatchGoalId(RandomNumberUtils.nextPositiveLong()), TextCampaign.BROAD_MATCH);

        modelChanges.process(List.of(INVALID_MINUS_KEYWORD), TextCampaign.MINUS_KEYWORDS);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation addOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                dslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);

        MassResult<Long> result = addOperation.apply();

        assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
    }

    @Test
    public void update_success() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo = steps.textCampaignSteps().createDefaultCampaign(clientInfo);
        long permalinkId = RandomUtils.nextLong();
        organizationsClientStub.addUidsByPermalinkId(permalinkId, List.of(campaignInfo.getUid()));

        Long phoneId = steps.clientPhoneSteps()
                .addDefaultClientOrganizationPhone(clientInfo.getClientId(), permalinkId).getId();

        ModelChanges<TextCampaign> modelChanges = new ModelChanges<>(campaignInfo.getId(), TextCampaign.class);
        modelChanges.process(permalinkId, TextCampaign.DEFAULT_PERMALINK_ID);
        modelChanges.process(phoneId, TextCampaign.DEFAULT_TRACKING_PHONE_ID);

        var options = new CampaignOptions();
        RestrictedCampaignsUpdateOperation addOperation = new RestrictedCampaignsUpdateOperation(
                Collections.singletonList(modelChanges),
                campaignInfo.getUid(),
                UidClientIdShard.of(campaignInfo.getUid(), campaignInfo.getClientId(), campaignInfo.getShard()),
                campaignModifyRepository,
                campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService,
                campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService,
                dslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);

        MassResult<Long> result = addOperation.apply();

        var campaigns = campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(), List.of(campaignInfo.getId()));

        assertThat(result.getValidationResult().flattenErrors()).isNotNull();
        assertThat(((TextCampaign) campaigns.get(0)).getDefaultPermalinkId()).isEqualTo(permalinkId);
        assertThat(((TextCampaign) campaigns.get(0)).getDefaultTrackingPhoneId()).isEqualTo(phoneId);
    }

}
