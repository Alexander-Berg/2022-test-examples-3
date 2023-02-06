package ru.yandex.direct.core.entity.campaign.service.operation;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
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
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.validation.ValidationUtils;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyList;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RestrictedCampaignAddOperationCampMetrikaCountersTest {

    private static final int METRIKA_COUNTER = RandomNumberUtils.nextPositiveInteger();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private CampaignModifyRepository campaignModifyRepository;

    @Autowired
    CampaignAddOperationSupportFacade campaignAddOperationSupportFacade;

    @Autowired
    private Steps steps;

    @Autowired
    public DslContextProvider dslContextProvider;

    @Autowired
    public RbacService rbacService;

    @Autowired
    private CampMetrikaCountersRepository campMetrikaCountersRepository;

    @Autowired
    private AddRestrictedCampaignValidationService addRestrictedCampaignValidationService;

    @Autowired
    private RequestBasedMetrikaClientFactory metrikaClientFactory;

    @Autowired
    GoalUtilsService goalUtilsService;

    private ClientInfo defaultClient;

    @Before
    public void before() {
        defaultClient = steps.userSteps().createDefaultUser().getClientInfo();
        metrikaClientStub.addUserCounter(defaultClient.getUid(), METRIKA_COUNTER);
    }


    @Test
    public void add_noCounters() {
        TextCampaign campaignWithoutCounters = defaultTextCampaign()
                .withMetrikaCounters(emptyList());
        Long cid = addCampaign(campaignWithoutCounters);

        Map<Long, List<Long>> metrikaCountersByCids =
                campMetrikaCountersRepository.getMetrikaCountersByCids(defaultClient.getShard(), List.of(cid));
        assertThat(metrikaCountersByCids)
                .extractingByKey(cid, InstanceOfAssertFactories.LIST)
                .isEmpty();
    }

    @Test
    public void add_hasCounters() {
        TextCampaign campaignWithCounters = defaultTextCampaign()
                .withMetrikaCounters(List.of((long) METRIKA_COUNTER));
        Long cid = addCampaign(campaignWithCounters);

        Map<Long, List<Long>> metrikaCountersByCids =
                campMetrikaCountersRepository.getMetrikaCountersByCids(defaultClient.getShard(), List.of(cid));
        assertThat(metrikaCountersByCids)
                .containsOnly(entry(cid, List.of((long) METRIKA_COUNTER)));
    }

    @Test
    public void addTwoCampaigns_withCounters_andWithoutCounters() {
        TextCampaign campaignWithoutCounters = defaultTextCampaign()
                .withMetrikaCounters(emptyList());
        TextCampaign campaignWithCounters = defaultTextCampaign()
                .withMetrikaCounters(List.of((long) METRIKA_COUNTER));
        List<Long> campaignIds = addCampaigns(List.of(campaignWithoutCounters, campaignWithCounters));
        Map<Long, List<Long>> metrikaCountersByCids =
                campMetrikaCountersRepository.getMetrikaCountersByCids(defaultClient.getShard(), campaignIds);
        assertThat(metrikaCountersByCids)
                .containsOnly(entry(campaignIds.get(0), emptyList()),
                        entry(campaignIds.get(1), List.of((long) METRIKA_COUNTER)));
    }

    private Long addCampaign(CampaignWithMetrikaCounters campaignWithMetrikaCounters) {
        return addCampaigns(List.of(campaignWithMetrikaCounters)).get(0);
    }

    private List<Long> addCampaigns(List<CampaignWithMetrikaCounters> campaignWithMetrikaCounters) {
        var options = new CampaignOptions();
        RestrictedCampaignsAddOperation addOperation = new RestrictedCampaignsAddOperation(
                campaignWithMetrikaCounters,
                defaultClient.getShard(),
                UidAndClientId.of(defaultClient.getUid(), defaultClient.getClientId()),
                defaultClient.getUid(),
                campaignModifyRepository,
                strategyTypedRepository,
                addRestrictedCampaignValidationService,
                campaignAddOperationSupportFacade, dslContextProvider, rbacService, options, metrikaClientFactory,
                goalUtilsService);

        MassResult<Long> result = addOperation.prepareAndApply();
        checkState(!ValidationUtils.hasAnyErrorsOrWarnings(result.getValidationResult()),
                "validationResult hasAnyErrorsOrWarnings");
        return mapList(result.getResult(), Result::getResult);
    }

}
