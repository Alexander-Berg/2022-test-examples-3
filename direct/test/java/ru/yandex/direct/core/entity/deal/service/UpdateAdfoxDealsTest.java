package ru.yandex.direct.core.entity.deal.service;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.deal.model.CompleteReason;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealAdfox;
import ru.yandex.direct.core.entity.deal.model.DealBase;
import ru.yandex.direct.core.entity.deal.model.StatusAdfox;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestClients;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UpdateAdfoxDealsTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private Steps steps;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private ClientInfo agencyClientInfo;
    private Long dealId;

    @Before
    public void setUp() {
        int dealShard = 1;
        agencyClientInfo =
                steps.clientSteps().createClient(new ClientInfo().withShard(dealShard).withClient(
                        TestClients.defaultClient().withRole(RbacRole.AGENCY)));
        Deal deal = TestDeals.defaultPrivateDeal(agencyClientInfo.getClientId());
        deal.withDirectStatus(StatusDirect.ACTIVE);
        List<DealInfo> createdDeals = steps.dealSteps().addDeals(Collections.singletonList(deal), agencyClientInfo);
        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        dealId = dealIds.get(0);
    }

    @Test
    public void adfoxClosedDealTest() {
        DealAdfox dealAdfox = dealService.getSingleDeal(agencyClientInfo.getClientId(), dealId);
        dealService.updateAdfoxDeals(singletonList(dealAdfox.withAdfoxStatus(StatusAdfox.CLOSED)));
        DealBase dealAfterUpdate = dealService.getSingleDeal(agencyClientInfo.getClientId(), dealId);
        DealBase expectedDealBase = new DealBase()
                .withDirectStatus(StatusDirect.COMPLETED)
                .withCompleteReason(CompleteReason.BY_PUBLISHER);
        assertThat(dealAfterUpdate,
                beanDiffer(expectedDealBase).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void adfoxNotClosedDealTest() {
        DealAdfox dealAdfox = dealService.getSingleDeal(agencyClientInfo.getClientId(), dealId);
        dealService.updateAdfoxDeals(singletonList(dealAdfox));
        DealBase dealAfterUpdate = dealService.getSingleDeal(agencyClientInfo.getClientId(), dealId);
        assertThat(dealAfterUpdate.getDirectStatus(), equalTo(StatusDirect.ACTIVE));
    }
}
