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
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultCpmBannerCampaign;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX;

@CoreTest
@RunWith(SpringRunner.class)
public class RestrictedCampaignsAddOperationCampaignDisabledPlacesTest {
    private static final List<String> DISABLED_VIDEO_PLACEMENTS = List.of("vk.com", "music.yandex.ru");
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;
    @Autowired
    CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private GoalUtilsService goalUtilsService;
    @Autowired
    private Steps steps;

    private ClientInfo defaultClient;
    private RestrictedCampaignsAddOperation addOperation;

    @Before
    public void before() {
        defaultClient = steps.userSteps().createDefaultUser().getClientInfo();
        CpmBannerCampaign cpmBannerCampaign = defaultCpmBannerCampaign()
                .withDisabledVideoPlacements(DISABLED_VIDEO_PLACEMENTS);

        CampaignOptions options = new CampaignOptions();
        addOperation = new RestrictedCampaignsAddOperation(
                List.of(cpmBannerCampaign),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, options, metrikaClientFactory,
                goalUtilsService);
    }

    @Test
    public void add_ValidDisabledVideoPlacements() {
        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
        Campaign actualCampaign = campaignRepository.getCampaigns(defaultClient.getShard(),
                List.of(result.get(0).getResult())).get(0);
        assertThat(actualCampaign.getDisabledVideoPlacements()).containsOnlyElementsOf(DISABLED_VIDEO_PLACEMENTS);
    }

    @Test
    public void add_InvalidDisabledVideoPlacements() {
        steps.clientSteps().updateClientLimits(defaultClient
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(defaultClient.getClientId())
                        .withVideoBlacklistSizeLimit(1L)));

        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isNotEmpty()
                .extracting(defectDefectInfo -> defectDefectInfo.getDefect().defectId())
                .first()
                .isEqualTo(SIZE_CANNOT_BE_MORE_THAN_MAX);
    }

}
