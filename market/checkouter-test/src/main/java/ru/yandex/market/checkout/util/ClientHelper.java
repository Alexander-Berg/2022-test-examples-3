package ru.yandex.market.checkout.util;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;

public final class ClientHelper {

    public static final long CALL_CENTER_OPERATOR_ID = 123123L;
    public static final long REFEREE_UID = 121212L;
    public static final long CRM_ROBOT_ID = 0L;

    private ClientHelper() {
    }

    public static ClientInfo shopClientFor(Order order) {
        return new ClientInfo(ClientRole.SHOP, order.getShopId());
    }

    public static ClientInfo shopUserClientFor(Order order) {
        return new ClientInfo(ClientRole.SHOP_USER, 12345L, order.getShopId());
    }

    public static ClientInfo businessClientFor(Order order) {
        return new ClientInfo(ClientRole.BUSINESS, order.getBusinessId());
    }

    public static ClientInfo businessUserClientFor(Order order) {
        return new ClientInfo(ClientRole.BUSINESS_USER, 12345L, null, order.getBusinessId());
    }

    public static ClientInfo userClientFor(Order order) {
        return new ClientInfo(ClientRole.USER, order.getBuyer().getUid());
    }

    public static ClientInfo callCenterOperatorFor(Order order) {
        return new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, CALL_CENTER_OPERATOR_ID);
    }

    public static ClientInfo crmRobotFor(Order order) {
        return new ClientInfo(ClientRole.CRM_ROBOT, CRM_ROBOT_ID);
    }
}
