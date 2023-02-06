package ru.yandex.market.checkout.checkouter.checkout;

import java.nio.charset.StandardCharsets;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

public class CreateOrderAssessorTest extends AbstractWebTestBase {

    private static final long ASSESSOR_UID = BuyerProvider.ASSESSOR_UID;

    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    @Qualifier("mbiCurator")
    private CuratorFramework curatorFramework;

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-118
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @DisplayName("Создание заказа неасессором")
    @Test
    public void shouldNotSetAssessorFlagIfOrderIsNotAssessor() throws Exception {
        // given that
        Parameters parameters = new Parameters();

        // when
        Order order = orderCreateHelper.createOrder(parameters);

        // expect:
        Assertions.assertFalse(order.getBuyer().getAssessor());
    }

    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-117
     */
    @Epic(Epics.CHECKOUT)
    @Story(Stories.CHECKOUT)
    @Test
    public void shouldSetAssessorFlagIfOrderIsAssessor() throws Exception {
        // given that
        createOrSetUids();

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setUid(ASSESSOR_UID);

        // when
        Order order = orderCreateHelper.createOrder(parameters);

        // expect:
        Assertions.assertTrue(order.getBuyer().getAssessor());
    }


    /**
     * @link https://testpalm.yandex-team.ru/testcase/checkouter-117
     */
    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @Test
    public void shouldSerializeAssessorForShop() throws Exception {
        // given that
        createOrSetUids();

        Parameters parameters = new Parameters();
        parameters.getBuyer().setUid(ASSESSOR_UID);

        // when
        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        Order order = orderGetHelper.getOrder(orderId, new ClientInfo(ClientRole.SHOP,
                parameters.getOrder().getShopId()));

        // expect:
        Assertions.assertNull(order.getBuyer().getAssessor());
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.ORDERS_ORDER_ID)
    @Test
    public void shouldSerializeAssessorForBusiness() throws Exception {
        // given that
        createOrSetUids();

        Parameters parameters = new Parameters();
        parameters.getBuyer().setUid(ASSESSOR_UID);

        // when
        Long orderId = orderCreateHelper.createOrder(parameters).getId();
        Order order = orderGetHelper.getOrder(orderId, new ClientInfo(ClientRole.BUSINESS,
                parameters.getOrder().getBusinessId()));

        // expect:
        Assertions.assertNull(order.getBuyer().getAssessor());
    }

    public void createOrSetUids() throws Exception {
        byte[] data = ("[" + ASSESSOR_UID + "]").getBytes(StandardCharsets.UTF_8);
        if (curatorFramework.checkExists().forPath("/checkout/assessor/uids") == null) {
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath("/checkout/assessor/uids", data);
        } else {
            curatorFramework.setData()
                    .forPath("/checkout/assessor/uids", data);
        }
    }
}
