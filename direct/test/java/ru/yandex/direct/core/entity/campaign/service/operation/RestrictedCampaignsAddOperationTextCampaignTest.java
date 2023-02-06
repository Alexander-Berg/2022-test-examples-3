package ru.yandex.direct.core.entity.campaign.service.operation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampOptionsStrategy;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.model.StrategyName;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
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

@CoreTest
@RunWith(SpringRunner.class)
public class RestrictedCampaignsAddOperationTextCampaignTest {
    public static final String INVALID_MINUS_KEYWORD = "asd,.as,da.,,21111,.111";
    @Autowired
    private StrategyTypedRepository strategyTypedRepository;
    @Autowired
    private CampaignModifyRepository campaignModifyRepository;
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

    @Before
    public void before() {
        defaultClient = steps.userSteps().createDefaultUser().getClientInfo();
    }

    @Test
    public void add_HasPreValidationError_ValidationDoNotFails() {
        UserInfo anotherUser = steps.userSteps().createDefaultUser();

        TextCampaign textCampaign = defaultTextCampaign()
                .withDayBudget(BigDecimal.TEN)
                .withMinusKeywords(List.of(INVALID_MINUS_KEYWORD));
        CampaignOptions options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                anotherUser.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, options, metrikaClientFactory,
                goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();

        assertThat(result.getValidationResult().flattenErrors()).isNotEmpty();
    }

    @Test
    public void add_HasPreValidationBroadMatchChanges_ValidationDoNotFails() {
        var strategy = (DbStrategy) new DbStrategy()
                .withAutobudget(CampaignsAutobudget.YES)
                .withPlatform(CampaignsPlatform.CONTEXT)
                .withStrategy(CampOptionsStrategy.DIFFERENT_PLACES)
                .withStrategyName(StrategyName.AUTOBUDGET_WEEK_BUNDLE)
                .withStrategyData(new StrategyData().withLimitClicks(222L));

        TextCampaign textCampaign = defaultTextCampaign()
                .withStrategy(strategy)
                .withBroadMatch(new BroadMatch().withBroadMatchFlag(true).withBroadMatchLimit(5));

        CampaignOptions options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
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
        assertThat(textCampaign.getBroadMatch().getBroadMatchFlag()).isEqualTo(false);
    }

    @Test
    public void add_HasPreValidationCheckPosition_And_EnableSendAccountNewsChanges_ValidationDoNotFails() {

        TextCampaign textCampaign = defaultTextCampaign()
                .withEnableCheckPositionEvent(null)
                .withEnableSendAccountNews(null)
                .withCheckPositionIntervalEvent(null);

        CampaignOptions options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
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
        assertThat(textCampaign.getEnableCheckPositionEvent()).isEqualTo(CampaignConstants.DEFAULT_ENABLE_CHECK_POSITION_EVENT);
        assertThat(textCampaign.getCheckPositionIntervalEvent()).isEqualTo(CampaignConstants.DEFAULT_CAMPAIGN_CHECK_POSITION_INTERVAL);
        assertThat(textCampaign.getEnableSendAccountNews()).isEqualTo(CampaignConstants.DEFAULT_ENABLE_SEND_ACCOUNT_NEWS);

    }

}
