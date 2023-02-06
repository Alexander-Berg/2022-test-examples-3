package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.validation.result.DefectIds.NO_RIGHTS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignsAddOperationTest {
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    private Steps steps;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    public DslContextProvider dslContextProvider;
    @Autowired
    public RbacService rbacService;

    @Autowired
    public GoalUtilsService goalUtilsService;

    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.userSteps().createDefaultUser().getClientInfo();
    }

    @Test
    public void add() {
        var options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultTextCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, options, metrikaClientFactory,
                goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isEmpty();
    }

    @Test
    public void add_AccessErrors() {
        UserInfo anotherUser = steps.userSteps().createDefaultUser();

        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(defaultTextCampaign()),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                anotherUser.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, new CampaignOptions(),
                metrikaClientFactory, goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isNotEmpty()
                .extracting(defectDefectInfo -> defectDefectInfo.getDefect().defectId())
                .first()
                .isEqualTo(NO_RIGHTS);
    }
}
