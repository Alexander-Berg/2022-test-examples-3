package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.yandexagencyorder.model.Status;
import ru.yandex.direct.core.entity.yandexagencyorder.model.YandexAgencyOrder;
import ru.yandex.direct.core.entity.yandexagencyorder.repository.YandexAgencyOrdersRepository;
import ru.yandex.direct.core.testing.data.TestYandexAgencyOrders;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyOrderParameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NotifyPromoOrderServiceTest {
    @Autowired
    private NotifyPromoOrderService notifyPromoOrderService;

    @Autowired
    private YandexAgencyOrdersRepository yandexAgencyOrdersRepository;

    @Autowired
    private UserSteps userSteps;
    private UserInfo user1;
    private List<Long> ids;
    private YandexAgencyOrder yandexAgencyOrder1;
    private Long orderId;

    @Before
    public void before() {
        user1 = userSteps.createDefaultUser();
        //тест проходит так быстро, что успевает сразу и создать и обновить заказ в течении 1 секунды
        //из-за этого время последнего изменения может не измениться
        //для этого делаем lastChanges "раньше" создание
        LocalDateTime oneMinuteBeforeNow = LocalDateTime.now().minusMinutes(1);
        orderId = user1.getClientInfo().getClientId().asLong() + 1000L;
        yandexAgencyOrder1 =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user1.getClientInfo().getClientId(),
                        orderId).withLastChange(oneMinuteBeforeNow);
    }

    @Test
    public void notifyPromoOrder_successCase() {
        ids = yandexAgencyOrdersRepository.addYandexAgencyOrders(user1.getShard(), Collections.singletonList(
                yandexAgencyOrder1));

        List<NotifyOrderParameters> notifyOrderParametersList = StreamEx.of(yandexAgencyOrder1)
                .map(yandexAgencyOrder -> new NotifyOrderParameters()
                        .withCampaignId(yandexAgencyOrder.getId())).toList();
        notifyPromoOrderService.notifyPromoOrder(notifyOrderParametersList);

        List<YandexAgencyOrder> actualOrders =
                yandexAgencyOrdersRepository.getYandexAgencyOrdersByOrderId(user1.getShard(), ids);

        checkOrder(actualOrders.get(0), yandexAgencyOrder1.withYaOrderStatus(
                Status.PAID));

        assertThat("LastChanged изменился", actualOrders.get(0).getLastChange(),
                not(yandexAgencyOrder1.getLastChange()));
    }

    @Test
    public void notifyPromoOrder_ordersNotFromRequestNotPaid() {
        ids = yandexAgencyOrdersRepository.addYandexAgencyOrders(user1.getShard(), Collections.singletonList(
                yandexAgencyOrder1));

        UserInfo user2 = userSteps.createDefaultUser();
        YandexAgencyOrder yandexAgencyOrder2 =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user2.getClientInfo().getClientId(),
                        orderId + 100L);
        List<Long> notPaidIds =
                yandexAgencyOrdersRepository
                        .addYandexAgencyOrders(user2.getShard(), Collections.singletonList(yandexAgencyOrder2));

        List<NotifyOrderParameters> notifyOrderParametersList = StreamEx.of(yandexAgencyOrder1)
                .map(yandexAgencyOrder -> new NotifyOrderParameters()
                        .withCampaignId(yandexAgencyOrder.getId())).toList();
        notifyPromoOrderService.notifyPromoOrder(notifyOrderParametersList);

        List<YandexAgencyOrder> actualNotPaidOrders =
                yandexAgencyOrdersRepository.getYandexAgencyOrdersByOrderId(user2.getShard(), notPaidIds);

        checkOrder(actualNotPaidOrders.get(0), yandexAgencyOrder2);
    }

    @Test
    public void notifyPromoOrder_orderResurrected() {
        ids = yandexAgencyOrdersRepository
                .addYandexAgencyOrders(user1.getShard(), Collections.singletonList(
                        yandexAgencyOrder1
                                .withYaOrderStatus(Status.RESURRECTED)
                                .withId(2L)));

        List<NotifyOrderParameters> notifyOrderParametersList = StreamEx.of(yandexAgencyOrder1)
                .map(yandexAgencyOrder -> new NotifyOrderParameters()
                        .withCampaignId(yandexAgencyOrder.getId())).toList();
        notifyPromoOrderService.notifyPromoOrder(notifyOrderParametersList);

        List<YandexAgencyOrder> actualOrders =
                yandexAgencyOrdersRepository.getYandexAgencyOrdersByOrderId(user1.getShard(), ids);

        checkOrder(actualOrders.get(0), yandexAgencyOrder1.withYaOrderStatus(Status.COMPLETED));
    }

    private void checkOrder(YandexAgencyOrder actualOrder, YandexAgencyOrder expectedOrder) {
        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("id"),
                newPath("yaOrderStatus"),
                newPath("clientId")
        );
        assertThat("Полученные заказы соответствуют ожиданиям",
                actualOrder,
                beanDiffer(expectedOrder).useCompareStrategy(strategy));

    }
}
