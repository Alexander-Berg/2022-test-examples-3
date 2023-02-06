package ru.yandex.market.checkout.helpers;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.viewmodel.OrderChangesViewModel;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

@WebTestHelper
public class OrderChangesHelper extends MockMvcAware {

    public OrderChangesHelper(WebApplicationContext webApplicationContext,
                              TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public ResultActions getOrderChangesForActions(
            long orderId,
            ClientInfo clientInfo
    ) throws Exception {
        return performApiRequest(makeBuilder(orderId, null, clientInfo));
    }

    public ResultActions getOrderChangesForActions(
            long orderId,
            @Nullable
                    Long eventId,
            ClientInfo clientInfo
    ) throws Exception {
        MockHttpServletRequestBuilder builder = makeBuilder(orderId, eventId, clientInfo);

        return performApiRequest(builder);
    }

    public OrderChangesViewModel getOrderChanges(
            long orderId,
            ClientInfo clientInfo
    ) throws Exception {
        return performApiRequest(makeBuilder(orderId, null, clientInfo), OrderChangesViewModel.class);
    }

    public OrderChangesViewModel getOrderChanges(
            long orderId,
            @Nullable
                    Long eventId,
            ClientInfo clientInfo
    ) throws Exception {
        return performApiRequest(makeBuilder(orderId, eventId, clientInfo), OrderChangesViewModel.class);
    }

    @NotNull
    private static MockHttpServletRequestBuilder makeBuilder(
            long orderId,
            @Nullable Long eventId,
            ClientInfo clientInfo
    ) {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/orders/{orderId}/changes", orderId);
        if (eventId != null) {
            builder.param("eventId", String.valueOf(eventId));
        }
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);
        return builder;
    }
}
