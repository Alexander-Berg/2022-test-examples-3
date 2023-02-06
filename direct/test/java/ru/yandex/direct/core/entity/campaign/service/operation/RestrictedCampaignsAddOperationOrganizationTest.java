package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.organizations.service.OrganizationService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.organizations.validation.OrganizationDefects.organizationNotFound;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class
RestrictedCampaignsAddOperationOrganizationTest {
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;
    @Autowired
    private CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationsClientStub organizationsClient;
    @Autowired
    private RbacService rbacService;
    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    private Steps steps;
    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;
    @Autowired
    private GoalUtilsService goalUtilsService;

    private ClientInfo clientInfo;

    private Long validPermalink = 1010L;
    private Long invalidPermalink = 2020L;

    @Before
    public void before() {
        clientInfo = steps.userSteps().createDefaultUser().getClientInfo();
        Long chiefUid = rbacService.getChiefByClientId(clientInfo.getClientId());
        organizationsClient.addUidsByPermalinkId(validPermalink, List.of(chiefUid));
    }

    @Test
    public void apply_CampaignWithOrganizationAdded_Success() {
        TextCampaign textCampaign = defaultTextCampaign().withDefaultPermalinkId(validPermalink);

        MassResult<Long> result = apply(textCampaign);
        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
    }

    @Test
    public void apply_CampaignWithOrganizationAdded_InvalidPermalink_Fail() {
        TextCampaign textCampaign = defaultTextCampaign().withDefaultPermalinkId(invalidPermalink);

        MassResult<Long> result = apply(textCampaign);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result.getValidationResult().flattenErrors()).hasSize(1);
            softly.assertThat(result.getValidationResult().flattenErrors().get(0).getDefect())
                    .isEqualTo(organizationNotFound());
        });
    }

    private MassResult<Long> apply(TextCampaign textCampaign) {
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
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
