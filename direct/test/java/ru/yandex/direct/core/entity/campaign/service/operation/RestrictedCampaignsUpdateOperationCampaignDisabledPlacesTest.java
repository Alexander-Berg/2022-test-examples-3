package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
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
import ru.yandex.direct.core.testing.info.campaign.CpmBannerCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidClientIdShard;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.RECALC_BRAND_LIFT_CAMPAIGNS;

@CoreTest
@RunWith(SpringRunner.class)
public class RestrictedCampaignsUpdateOperationCampaignDisabledPlacesTest {
    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UpdateRestrictedCampaignValidationService updateRestrictedCampaignValidationService;
    @Autowired
    private CampaignUpdateOperationSupportFacade campaignUpdateOperationSupportFacade;
    @Autowired
    private CampaignAdditionalActionsService campaignAdditionalActionsService;
    @Autowired
    private MetrikaClient metrikaClient;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private FeatureService featureService;

    @Autowired
    private Steps steps;

    private RestrictedCampaignsUpdateOperation updateOperation;
    private CpmBannerCampaignInfo cpmBannerCampaign;

    @Before
    public void before() {
        cpmBannerCampaign = steps.cpmBannerCampaignSteps().createDefaultCampaign();

        ModelChanges<CpmBannerCampaign> campaignModelChanges = new ModelChanges<>(cpmBannerCampaign.getId(),
                CpmBannerCampaign.class);
        campaignModelChanges.process(DISABLED_VIDEO_PLACEMENTS, CpmBannerCampaign.DISABLED_VIDEO_PLACEMENTS);

        var options = new CampaignOptions();
        updateOperation = new RestrictedCampaignsUpdateOperation(List.of(campaignModelChanges),
                cpmBannerCampaign.getUid(), UidClientIdShard.of(cpmBannerCampaign.getUid(),
                cpmBannerCampaign.getClientId(),
                cpmBannerCampaign.getShard()), campaignModifyRepository, campaignTypedRepository,
                strategyTypedRepository,
                updateRestrictedCampaignValidationService, campaignUpdateOperationSupportFacade,
                campaignAdditionalActionsService, dslContextProvider, rbacService, metrikaClientFactory, featureService,
                Applicability.PARTIAL, options);

        steps.dbQueueSteps().registerJobType(RECALC_BRAND_LIFT_CAMPAIGNS);
    }

    @Test
    public void update_ValidDisabledVideoPlacements() {
        MassResult<Long> result = updateOperation.apply();

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        Campaign actualCampaign = campaignRepository.getCampaigns(cpmBannerCampaign.getShard(),
                List.of(result.get(0).getResult())).get(0);
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);
    }

}
