package ru.yandex.market.checkout.helpers;

import javax.annotation.Nullable;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.json.Names;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.CLIENT_ROLE;

@WebTestHelper
public class OrdersGetHelper extends MockMvcAware {

    public OrdersGetHelper(WebApplicationContext webApplicationContext,
                           TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public PagedOrders getOrders(ClientInfo clientInfo) throws Exception {
        return getOrders(null, clientInfo);
    }

    public PagedOrders getOrders(@Nullable OrderSearchRequest orderSearchRequest, ClientInfo clientInfo)
            throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders")
                .param(CLIENT_ROLE, clientInfo.getRole().name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name(), Color.WHITE.name())
                .param(CheckouterClientParams.CLIENT_ID, clientInfo.getId() == null ? null :
                        String.valueOf(clientInfo.getId()))
                .param(CheckouterClientParams.UID, clientInfo.getUid() == null ? null :
                        String.valueOf(clientInfo.getUid()))
                .param(CheckouterClientParams.SHOP_ID, clientInfo.getShopId() == null ? null :
                        String.valueOf(clientInfo.getShopId()))
                .param(CheckouterClientParams.BUSINESS_ID, clientInfo.getBusinessId() == null ? null :
                        String.valueOf(clientInfo.getBusinessId()));

        if (orderSearchRequest != null) {
            if (orderSearchRequest.trackCodes != null) {
                orderSearchRequest.trackCodes.forEach(trackCode -> {
                    builder.param(Names.Track.TRACK_CODE, trackCode);
                });
            }

            if (orderSearchRequest.hasTrackCode != null) {
                builder.param(CheckouterClientParams.HAS_TRACK_CODE, Boolean.toString(orderSearchRequest.hasTrackCode));
            }
        }

        return performApiRequest(builder, PagedOrders.class);
    }
}
