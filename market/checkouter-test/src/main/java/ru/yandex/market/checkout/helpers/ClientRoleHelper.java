package ru.yandex.market.checkout.helpers;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BUSINESS_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;

/**
 * @author mmetlov
 */
public final class ClientRoleHelper {

    public static final String CLIENT_ROLE_PARAM = "clientRole";

    private ClientRoleHelper() {
    }

    public static void addClientRoleParameters(ClientRole clientRole, MockHttpServletRequestBuilder builder) {
        switch (clientRole) {
            case SYSTEM:
                builder.param(CLIENT_ROLE_PARAM, "SYSTEM");
                break;
            case BUSINESS:
                builder.param(CLIENT_ROLE_PARAM, "SYSTEM");
                break;
            case SHOP_USER:
                builder.param(CLIENT_ROLE_PARAM, "SHOP_USER")
                        .param("shopId", Long.toString(OrderProvider.SHOP_ID))
                        .param("clientId", "12345");
                break;
            case BUSINESS_USER:
                builder.param(CLIENT_ROLE_PARAM, "BUSINESS_USER")
                        .param("bisinessId", Long.toString(OrderProvider.BUSINESS_ID))
                        .param("clientId", "12345");
                break;
            case SHOP:
                builder.param(CLIENT_ROLE_PARAM, "SHOP")
                        .param("clientId", Long.toString(OrderProvider.SHOP_ID));
                break;
            case USER:
                builder.param(CLIENT_ROLE_PARAM, "USER")
                        .param("clientId", Long.toString(BuyerProvider.UID));
                break;
            default:
                throw new IllegalArgumentException("Client role " + clientRole + " is not supported yet");
        }
    }

    public static void addClientInfoParameters(ClientInfo clientInfo, MockHttpServletRequestBuilder builder) {
        builder.param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CLIENT_ID, clientInfo.getId() == null ? null : clientInfo.getId().toString())
                .param(SHOP_ID, clientInfo.getShopId() == null ? null : clientInfo.getShopId().toString())
                .param(BUSINESS_ID, clientInfo.getBusinessId() == null ? null : clientInfo.getBusinessId().toString());
    }
}
