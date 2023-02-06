package ru.yandex.direct.core.entity.deal.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.StatusAdfox;
import ru.yandex.direct.core.entity.deal.model.StatusAdfoxSync;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.LoginAndClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealServiceSmokeTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private Steps steps;

    private ClientInfo clientInfo;
    private User user;
    private List<DealInfo> createdDeals;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        user = clientInfo.getChiefUserInfo().getUser();
        createdDeals = steps.dealSteps().addRandomDeals(clientInfo, 1);
    }

    @Test
    public void getDeals() {
        List<Deal> actualDeals =
                StreamEx.of(
                        dealService.getDeals(clientInfo.getClientId(), mapList(createdDeals, t -> t.getDeal().getId())))
                        .sorted(Comparator.comparing(Deal::getId))
                        .toList();

        List<Deal> expectedDeals = StreamEx.of(createdDeals)
                .map(DealInfo::getDeal)
                .sorted(Comparator.comparing(Deal::getId))
                .toList();

        BeanDifferMatcher<List<? extends Deal>> matcher = beanDiffer(expectedDeals);
        matcher.useCompareStrategy(dealListCompareStrategy());
        Assertions.assertThat(actualDeals).is(matchedBy(matcher));
    }

    @Test
    public void add() {
        Deal expectedDeal = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        expectedDeal.setStatusAdfoxSync(null);
        expectedDeal.setDirectStatus(null);

        MassResult<Long> result = dealService.addDeals(clientInfo.getClientId(),
                singletonList(expectedDeal));
        Assertions.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();
        List<Deal> actualDealList = dealService
                .getDeals(clientInfo.getClientId(), singletonList(result.getResult().get(0).getResult()));

        BeanDifferMatcher<List<? extends Deal>> matcher = beanDiffer(singletonList(expectedDeal));
        matcher.useCompareStrategy(dealListCompareStrategy());
        Assertions.assertThat(actualDealList).is(matchedBy(matcher));
    }

    @Test
    public void getDealsBrief() {
        List<Deal> actualDeals =
                StreamEx.of(
                        dealService.getDealsBrief(clientInfo.getClientId()))
                        .sorted(Comparator.comparing(Deal::getId))
                        .toList();


        List<Deal> expectedDeals = StreamEx.of(createdDeals)
                .map(DealInfo::getDeal)
                .sorted(Comparator.comparing(Deal::getId))
                .toList();

        BeanDifferMatcher<List<? extends Deal>> matcher = beanDiffer(expectedDeals);
        matcher.useCompareStrategy(dealListCompareStrategy());
        Assertions.assertThat(actualDeals).is(matchedBy(matcher));
    }

    @Test
    public void getLoginAndClientIdsByDealIds() {
        Map<Long, LoginAndClientId> actual =
                dealService.getLoginAndClientIdsByDealIds(singletonList(createdDeals.get(0).getDealId()));
        Map<Long, LoginAndClientId> expected = new HashMap<>();
        expected.put(createdDeals.get(0).getDealId(), LoginAndClientId.of(user.getLogin(), clientInfo.getClientId()));
        assertThat(actual, beanDiffer(expected));
    }

    private DefaultCompareStrategy dealListCompareStrategy() {
        return DefaultCompareStrategies
                .onlyExpectedFields()
                .forFields(newPath("\\d+", "dateCreated")).useMatcher(approximatelyNow())
                .forFields(newPath("\\d+", "directStatus")).useMatcher(Matchers.equalTo(StatusDirect.RECEIVED))
                .forFields(newPath("\\d+", "statusAdfoxSync")).useMatcher(Matchers.equalTo(StatusAdfoxSync.NO))
                .forFields(newPath("\\d+", "adfoxStatus")).useMatcher(Matchers.equalTo(StatusAdfox.CREATED))
                .forFields(newPath("\\d+", "cpm")).useDiffer(new BigDecimalDiffer())
                .forFields(newPath("\\d+", "agencyMinPrice", "bigDecimalValue"))
                .useMatcher(Matchers.greaterThan(BigDecimal.ZERO));
    }
}
