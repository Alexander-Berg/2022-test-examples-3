package ru.yandex.direct.core.entity.yandexagencyorder.repository;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.yandexagencyorder.model.Status;
import ru.yandex.direct.core.entity.yandexagencyorder.model.YandexAgencyOrder;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestYandexAgencyOrders;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class YandexAgencyOrdersRepositoryTest {
    @Autowired
    private YandexAgencyOrdersRepository yandexAgencyOrdersRepository;

    @Autowired
    private UserSteps userSteps;
    private UserInfo user;
    private List<Long> ids;
    private YandexAgencyOrder yandexAgencyOrder;

    @Before
    public void before() {
        user = userSteps.createDefaultUser();
        yandexAgencyOrder =
                TestYandexAgencyOrders.defaultYandexAgencyOrder(user.getClientInfo().getClientId(),
                        user.getClientInfo().getClientId().asLong());
        ids = yandexAgencyOrdersRepository
                .addYandexAgencyOrders(user.getShard(), Collections.singletonList(yandexAgencyOrder));
    }

    @Test
    public void getNotifyOrders() {
        List<YandexAgencyOrder> actualOrders =
                yandexAgencyOrdersRepository.getYandexAgencyOrdersByOrderId(user.getShard(), ids);
        checkOrder(actualOrders.get(0), yandexAgencyOrder);
    }

    @Test
    public void updateStatus() {
        AppliedChanges<YandexAgencyOrder> changes =
                new ModelChanges<>(yandexAgencyOrder.getId(), YandexAgencyOrder.class)
                        .process(Status.PAID, YandexAgencyOrder.YA_ORDER_STATUS)
                        .applyTo(yandexAgencyOrder);

        yandexAgencyOrdersRepository.updateYandexOrder(user.getShard(), Collections.singletonList(changes));

        YandexAgencyOrder expectedOrder = yandexAgencyOrder.withYaOrderStatus(Status.PAID);
        List<YandexAgencyOrder> actualOrders =
                yandexAgencyOrdersRepository.getYandexAgencyOrdersByOrderId(user.getShard(), ids);

        checkOrder(actualOrders.get(0), expectedOrder);
    }

    private void checkOrder(YandexAgencyOrder actualOrder, YandexAgencyOrder expectedOrders) {
        CompareStrategy strategy = DefaultCompareStrategies.onlyFields(
                newPath("id"),
                newPath("yaOrderStatus"),
                newPath("clientId")
        );
        assertThat("Полученные заказы соответствуют ожиданиям",
                actualOrder,
                beanDiffer(expectedOrders).useCompareStrategy(strategy));
    }
}
