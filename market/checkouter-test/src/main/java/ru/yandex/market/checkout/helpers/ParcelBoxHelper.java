package ru.yandex.market.checkout.helpers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxes;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class ParcelBoxHelper extends MockMvcAware {

    public ParcelBoxHelper(WebApplicationContext webApplicationContext,
                           TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public List<ParcelBox> putBoxes(long orderId,
                                    long parcelId,
                                    List<ParcelBox> boxes,
                                    ClientInfo clientInfo,
                                    ResultActionsContainer container) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(new ParcelBoxes(boxes));
        MockHttpServletRequestBuilder builder = put(
                "/orders/{order-id}/delivery/parcels/{parcel-id}/boxes",
                orderId,
                parcelId
        );
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        ParcelBoxes parcelBoxes = performApiRequest(
                builder
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content),
                ParcelBoxes.class,
                container
        );

        return parcelBoxes == null ? null : parcelBoxes.getBoxes();
    }

    public List<ParcelBox> putBoxes(long orderId,
                                    long parcelId,
                                    List<ParcelBox> boxes,
                                    ClientInfo clientInfo) throws Exception {
        return putBoxes(orderId, parcelId, boxes, clientInfo, null);
    }

    public ResultActions putItems(long orderId,
                                  long parcelId,
                                  long boxId,
                                  List<ParcelBoxItem> items,
                                  ClientInfo clientInfo) throws Exception {
        String content = testSerializationService.serializeCheckouterObject(items);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(
                "/orders/{order-id}/delivery/parcels/{parcel-id}/boxes/{box-id}/items",
                orderId,
                parcelId,
                boxId
        );
        ClientRoleHelper.addClientInfoParameters(clientInfo, builder);

        ResultActions resultActions = performApiRequest(
                builder
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(content)
        );
        return resultActions.andExpect(status().isOk());
    }

    public void arrangeBoxes(Order order) throws Exception {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(order.getItems().stream()
                .map(oi -> {
                    ParcelBoxItem parcelBoxItem = new ParcelBoxItem();
                    parcelBoxItem.setItemId(oi.getId());
                    parcelBoxItem.setCount(oi.getCount());
                    return parcelBoxItem;
                }).collect(Collectors.toList()));

        putBoxes(
                order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                Collections.singletonList(parcelBox),
                ClientInfo.SYSTEM
        );
    }

    @Nonnull
    public ParcelBox provideOneBoxForOrder(Order order) {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(order.getItems()
                .stream()
                .map(oi -> {
                    ParcelBoxItem parcelBoxItem = new ParcelBoxItem();
                    parcelBoxItem.setItemId(oi.getId());
                    parcelBoxItem.setCount(oi.getCount());
                    return parcelBoxItem;
                }).collect(Collectors.toList()));
        return parcelBox;
    }
}
