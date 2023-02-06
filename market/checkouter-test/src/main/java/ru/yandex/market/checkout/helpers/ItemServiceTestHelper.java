package ru.yandex.market.checkout.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.request.ItemServiceReplaceRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceConfirmViewModel;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceTimeslotsRequest;
import ru.yandex.market.checkout.checkouter.viewmodel.ItemServiceViewModel;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebTestHelper
public class ItemServiceTestHelper extends MockMvcAware {

    @Autowired
    public ItemServiceTestHelper(WebApplicationContext webApplicationContext,
                                 TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public ResultActions confirm(long orderId, long itemServiceId,
                                 ItemServiceConfirmViewModel itemServiceConfirmViewModel) throws Exception {
        return confirm(orderId, itemServiceId, itemServiceConfirmViewModel, ClientRole.SYSTEM);
    }

    public ResultActions confirm(long orderId, long itemServiceId,
                                 ItemServiceConfirmViewModel itemServiceConfirmViewModel,
                                 ClientRole clientRole) throws Exception {
        return confirm(orderId, itemServiceId, itemServiceConfirmViewModel, clientRole, null);
    }

    public ResultActions confirm(long orderId, long itemServiceId,
                                 ItemServiceConfirmViewModel itemServiceConfirmViewModel,
                                 ClientRole clientRole, Long clientId) throws Exception {
        var requestBuilder = post("/orders/{orderId}/services/{itemServiceId}/confirm", orderId, itemServiceId)
                .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(itemServiceConfirmViewModel));
        if (clientId != null) {
            requestBuilder.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }
        return mockMvc.perform(requestBuilder);
    }

    public ResultActions replace(long orderId, long itemServiceId,
                                 ItemServiceReplaceRequest itemServiceReplaceRequest) throws Exception {
        return replace(orderId, itemServiceId, itemServiceReplaceRequest, ClientRole.SYSTEM, null);
    }

    public ResultActions replace(long orderId, long itemServiceId,
                                 ItemServiceReplaceRequest itemServiceReplaceRequest,
                                 ClientRole clientRole) throws Exception {
        return replace(orderId, itemServiceId, itemServiceReplaceRequest, clientRole, null);
    }

    public ResultActions replace(long orderId, long itemServiceId,
                                 ItemServiceReplaceRequest itemServiceReplaceRequest,
                                 ClientRole clientRole, Long clientId) throws Exception {
        var requestBuilder = post("/orders/{orderId}/services/{itemServiceId}/replace", orderId, itemServiceId)
                .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(itemServiceReplaceRequest));
        if (clientId != null) {
            requestBuilder.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }
        return mockMvc.perform(requestBuilder);
    }

    public ResultActions updateOrderItemServiceStatus(long orderId, long itemServiceId,
                                                      ItemServiceViewModel itemServiceViewModel) throws Exception {
        var requestBuilder = post("/orders/{orderId}/services/{itemServiceId}/status", orderId, itemServiceId)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(itemServiceViewModel));
        return mockMvc.perform(requestBuilder);
    }

    public ResultActions getTimeslots(ItemServiceTimeslotsRequest request) throws Exception {
        return getTimeslots(request, ClientRole.SYSTEM, null, Color.WHITE);
    }

    public ResultActions getTimeslots(ItemServiceTimeslotsRequest request,
                                      ClientRole clientRole,
                                      Long clientId, Color rgb) throws Exception {
        var requestBuilder = post("/services/timeslots")
                .param(CheckouterClientParams.CLIENT_ROLE, clientRole.name())
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(request));
        if (clientId != null) {
            requestBuilder.param(CheckouterClientParams.CLIENT_ID, clientId.toString());
        }
        if (rgb != null) {
            requestBuilder.param(CheckouterClientParams.RGB, rgb.name());
        }
        return mockMvc.perform(requestBuilder);
    }
}
