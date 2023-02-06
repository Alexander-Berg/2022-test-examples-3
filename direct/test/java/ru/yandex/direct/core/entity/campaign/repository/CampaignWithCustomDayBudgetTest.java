package ru.yandex.direct.core.entity.campaign.repository;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDayBudget;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientFactory;
import ru.yandex.direct.core.entity.campaign.service.RestrictedCampaignsAddOperation;
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade;
import ru.yandex.direct.core.entity.campaign.service.validation.AddRestrictedCampaignValidationService;
import ru.yandex.direct.core.entity.retargeting.service.common.GoalUtilsService;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;

import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.DAY_BUDGET_OVERRIDEN_BY_WALLET;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithCustomDayBudgetTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

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
    private TestCampaignRepository testCampaignRepository;

    @Autowired
    private GoalUtilsService goalUtilsService;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void dayBudgetOverriddenByWallet_Warning() {
        // Создаём одну кампанию, только для того, чтобы завести кошелёк
        addCampaign(defaultTextCampaign());

        // Поставим на кошелёк dayBudget поменьше
        List<? extends BaseCampaign> wallets = campaignTypedRepository.getTypedCampaigns(
                clientInfo.getShard(), clientInfo.getClientId(), List.of(CampaignType.WALLET));
        WalletTypedCampaign wallet = (WalletTypedCampaign) wallets.get(0);
        testCampaignRepository.setDayBudget(clientInfo.getShard(), wallet.getId(), BigDecimal.valueOf(300),
                null, null);

        // Создадим кампанию с dayBudget побольше
        TextCampaign textCampaign = defaultTextCampaign()
                .withDayBudget(BigDecimal.valueOf(400));

        var result = addCampaign(textCampaign);

        // Предупреждение - dayBudget кампании заменён на dayBudget кошелька
        MatcherAssert.assertThat(result.getValidationResult(), hasWarningWithDefinition(validationError(
                path(index(0), field(CampaignWithDayBudget.DAY_BUDGET)),
                DAY_BUDGET_OVERRIDEN_BY_WALLET)));
    }

    private MassResult<Long> addCampaign(TextCampaign textCampaign) {
        CampaignOptions options = new CampaignOptions();

        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                List.of(textCampaign),
                clientInfo.getShard(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                clientInfo.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, options, metrikaClientFactory,
                goalUtilsService);

        return addOperation.prepareAndApply();
    }

}
