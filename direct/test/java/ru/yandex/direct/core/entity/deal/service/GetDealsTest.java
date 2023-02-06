package ru.yandex.direct.core.entity.deal.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.deal.model.CompleteReason;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetDealsTest {
    @Autowired
    private DealService dealService;

    @Autowired
    private Steps steps;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private ClientInfo clientInfo;
    private List<DealInfo> createdDeals;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
        Deal deal = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        deal.withDirectStatus(StatusDirect.RECEIVED);
        deal.withCpm(BigDecimal.valueOf(10.6666d)).withMarginRatio(Percent.fromPercent(BigDecimal.TEN));
        createdDeals = steps.dealSteps()
                .addDeals(Collections.singletonList(deal),
                        clientInfo);
    }

    @Test
    public void getDealsCheckAgencyMinPrice() {
        Deal dealForAdd = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        dealForAdd.withCpm(BigDecimal.valueOf(10.6666d)).withMarginRatio(Percent.fromPercent(BigDecimal.TEN));
        createdDeals = steps.dealSteps()
                .addDeals(Collections.singletonList(dealForAdd),
                        clientInfo);
        List<Deal> actualDeals =
                StreamEx.of(
                        dealService.getDeals(clientInfo.getClientId(), mapList(createdDeals, t -> t.getDeal().getId())))
                        .sorted(Comparator.comparing(Deal::getId))
                        .toList();
        CurrencyCode workCurrency = clientInfo.getClient().getWorkCurrency();
        Money expectedAgencyMinPrice = Money.valueOf(BigDecimal.valueOf(13.5d), workCurrency);
        softly.assertThat(actualDeals.get(0).getAgencyMinPrice()).isEqualTo(expectedAgencyMinPrice);
    }

    @Test
    public void activateDeals() {
        Deal dealForAdd = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        dealForAdd.withDirectStatus(StatusDirect.RECEIVED);
        createdDeals = steps.dealSteps().addDeals(Collections.singletonList(dealForAdd), clientInfo);

        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        MassResult<Long>
                result = dealService.activateDeals(clientInfo.getClientId(), dealIds, Applicability.FULL);
        Assertions.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();

        List<Deal> actualDealList = dealService
                .getDeals(clientInfo.getClientId(), singletonList(result.getResult().get(0).getResult()));

        Assertions.assertThat(actualDealList).allMatch(deal -> deal.getDirectStatus() == StatusDirect.ACTIVE);
    }

    @Test
    public void completeDeals() {
        Deal dealForAdd = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        dealForAdd.withDirectStatus(StatusDirect.RECEIVED);
        createdDeals = steps.dealSteps().addDeals(Collections.singletonList(dealForAdd), clientInfo);

        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        MassResult<Long> result =
                dealService
                        .completeDeals(clientInfo.getClientId(), dealIds, CompleteReason.BY_CLIENT, Applicability.FULL);
        Assertions.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();

        List<Deal> actualDealList = dealService
                .getDeals(clientInfo.getClientId(), singletonList(result.getResult().get(0).getResult()));

        Assertions.assertThat(actualDealList).allMatch(deal -> deal.getCompleteReason() == CompleteReason.BY_CLIENT);
    }

    @Test
    public void archiveDeals() {
        Deal dealForAdd = TestDeals.defaultPrivateDeal(clientInfo.getClientId());
        dealForAdd.withDirectStatus(StatusDirect.COMPLETED);
        createdDeals = steps.dealSteps().addDeals(Collections.singletonList(dealForAdd), clientInfo);

        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        MassResult<Long> result = dealService.archiveDeals(clientInfo.getClientId(), dealIds, Applicability.FULL);

        Assertions.assertThat(result.getValidationResult().hasAnyErrors()).isFalse();

        List<Deal> actualDealList = dealService
                .getDeals(clientInfo.getClientId(), singletonList(result.getResult().get(0).getResult()));

        Assertions.assertThat(actualDealList).allMatch(deal -> deal.getDirectStatus() == StatusDirect.ARCHIVED);
    }

}
