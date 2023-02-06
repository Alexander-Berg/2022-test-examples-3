package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelBoxHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class ParcelBoxTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderPayHelper payHelper;
    @Autowired
    private ParcelBoxHelper parcelBoxHelper;

    @Test
    public void shouldCreateBlueOrderWithOneBox() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);

        assertThat(order.getDelivery().getParcels().get(0).getBoxes(),
                contains(allOf(
                        hasProperty("id", notNullValue()),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(actualDeliveryResult.getWeight().movePointRight(3)
                                .setScale(0, RoundingMode.CEILING).longValue())),
                        hasProperty("width", is(actualDeliveryResult.getDimensions().get(1).longValue())),
                        hasProperty("height", is(actualDeliveryResult.getDimensions().get(2).longValue())),
                        hasProperty("depth", is(actualDeliveryResult.getDimensions().get(0).longValue()))
                )));

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);

        assertThat(orderFromDB.getDelivery().getParcels().get(0).getBoxes(),
                contains(allOf(
                        hasProperty("id", notNullValue()),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(actualDeliveryResult.getWeight().movePointRight(3)
                                .setScale(0, RoundingMode.CEILING).longValue())),
                        hasProperty("width", is(actualDeliveryResult.getDimensions().get(1).longValue())),
                        hasProperty("height", is(actualDeliveryResult.getDimensions().get(2).longValue())),
                        hasProperty("depth", is(actualDeliveryResult.getDimensions().get(0).longValue()))
                )));
    }

    @Test
    public void shouldPutBoxes() throws Exception {
        Order order = createAndPayOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");
        parcelBox1.setWeight(1L);
        parcelBox1.setWidth(2L);
        parcelBox1.setHeight(3L);
        parcelBox1.setDepth(4L);

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setExternalId("exId");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setWeight(10L);

        List<ParcelBox> parcelBoxes = parcelBoxHelper.putBoxes(order.getId(), parcelId,
                Arrays.asList(parcelBox1, parcelBox2, parcelBox3), ClientInfo.SYSTEM);

        assertBoxes(parcelBoxes);

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);

        assertBoxes(orderFromDB.getDelivery().getParcels().get(0).getBoxes());
    }

    @Test
    public void shouldAllowEmptyBoxList() throws Exception {
        Order order = createAndPayDropshipOrder();
        long parcelId = order.getDelivery().getParcels().get(0).getId();

        List<ParcelBox> parcelBoxes = parcelBoxHelper.putBoxes(
                order.getId(),
                parcelId,
                List.of(),
                ClientInfo.SYSTEM
        );

        assertThat(parcelBoxes, empty());
        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);
        List<ParcelBox> boxes = orderFromDB.getDelivery().getParcels().get(0).getBoxes();
        assertThat(boxes, empty());
    }

    @Test
    public void shouldNotCreateInitialBox() {
        Order order = createAndPayDropshipOrder();
        Parcel parcel = order.getDelivery().getParcels().get(0);

        assertThat(parcel.getBoxes(), empty());
    }

    @Test
    public void shouldPutBoxesWithCorrectItem() throws Exception {
        Order order = createAndPayOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        long itemId = order.getItems().iterator().next().getId();

        ParcelBoxItem parcelItem = new ParcelBoxItem();
        parcelItem.setItemId(itemId);
        parcelItem.setCount(1);

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(List.of(parcelItem));

        List<ParcelBox> parcelBoxes = parcelBoxHelper.putBoxes(order.getId(), parcelId,
                List.of(parcelBox), ClientInfo.SYSTEM);

        assertOneBox(parcelBoxes, itemId);

        Order orderFromDB = client.getOrder(order.getId(), ClientRole.SYSTEM, 1L);

        assertOneBox(orderFromDB.getDelivery().getParcels().get(0).getBoxes(), itemId);
    }

    @Test
    public void wrongItemCountCausesPutBoxesReturn400() throws Exception {
        Order order = createAndPayOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        long itemId = order.getItems().iterator().next().getId();

        ParcelBoxItem parcelItem = new ParcelBoxItem();
        parcelItem.setItemId(itemId);
        parcelItem.setCount(2);

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(List.of(parcelItem));

        parcelBoxHelper.putBoxes(order.getId(), parcelId, List.of(parcelBox), ClientInfo.SYSTEM,
                new ResultActionsContainer()
                        .andExpect(status().is(400))
                        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                        .andExpect(jsonPath("$.message").value("Order items don't match boxes items"))
        );
    }

    @Test
    public void wrongItemIdCausesPutBoxesReturn400() throws Exception {
        Order order = createAndPayOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        long itemId = order.getItems().iterator().next().getId();

        ParcelBoxItem parcelItem = new ParcelBoxItem();
        parcelItem.setItemId(itemId + 1);
        parcelItem.setCount(1);

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setItems(List.of(parcelItem));

        parcelBoxHelper.putBoxes(order.getId(), parcelId, List.of(parcelBox), ClientInfo.SYSTEM,
                new ResultActionsContainer()
                        .andExpect(status().is(400))
                        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                        .andExpect(jsonPath("$.message")
                                .value("Order items don't match boxes items"))
        );
    }

    @Test
    public void shouldPutBoxesInCorrectStatuses() throws Exception {
        Set<OrderStatus> successfulStatuses =
                EnumSet.of(OrderStatus.PROCESSING, OrderStatus.DELIVERY, OrderStatus.PICKUP, OrderStatus.CANCELLED);

        setFixedTime(INTAKE_AVAILABLE_DATE);

        orderStatusHelper.proceedAllStatusesAndCheck(
                () -> yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                        .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                        .withDeliveryType(DeliveryType.PICKUP)
                        .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                        .withPartnerInterface(true)
                        .withColor(Color.BLUE)
                        .build(),

                (order, status) -> {
                    List<ParcelBox> boxes = parcelBoxHelper.putBoxes(order.getId(),
                            order.getDelivery().getParcels().get(0).getId(),
                            List.of(new ParcelBox()),
                            ClientInfo.SYSTEM,
                            new ResultActionsContainer().andExpect(
                                    status().is(successfulStatuses.contains(status) ? 200 : 400)));

                    ParcelBox patchParcelBox = new ParcelBox();
                    patchParcelBox.setId(
                            Optional.ofNullable(boxes).flatMap(b -> b.stream().findFirst())
                                    .map(ParcelBox::getId).orElse(1L));
                });
    }

    @Test
    public void shouldGet400WhenPutBoxesWithSameFulfilmentId() throws Exception {
        Order order = createAndPayOrder();

        long parcelId = order.getDelivery().getParcels().get(0).getId();

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setFulfilmentId("ffId");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setFulfilmentId("ffId");

        parcelBoxHelper.putBoxes(order.getId(),
                parcelId,
                Arrays.asList(parcelBox1, parcelBox2),
                ClientInfo.SYSTEM,
                new ResultActionsContainer().andExpect(status().is(HttpStatus.SC_BAD_REQUEST)));
    }


    private Order createAndPayOrder() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);
        return order;
    }

    private Order createAndPayDropshipOrder() {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = DropshipDeliveryHelper.getDropshipPrepaidParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        payHelper.payForOrder(order);
        return order;
    }

    private void assertBoxes(List<ParcelBox> parcelBoxes) {
        assertThat(parcelBoxes, containsInAnyOrder(
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", is("ffId")),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(1L)),
                        hasProperty("width", is(2L)),
                        hasProperty("height", is(3L)),
                        hasProperty("depth", is(4L))
                ),
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", is("exId")),
                        hasProperty("weight", nullValue()),
                        hasProperty("width", nullValue()),
                        hasProperty("height", nullValue()),
                        hasProperty("depth", nullValue())
                ),
                allOf(
                        hasProperty("id"), notNullValue(),
                        hasProperty("fulfilmentId", nullValue()),
                        hasProperty("externalId", nullValue()),
                        hasProperty("weight", is(10L)),
                        hasProperty("width", nullValue()),
                        hasProperty("height", nullValue()),
                        hasProperty("depth", nullValue())
                )
        ));
    }

    private void assertOneBox(List<ParcelBox> parcelBoxes, long itemId) {
        assertNotNull(parcelBoxes);
        assertEquals(1, parcelBoxes.size());
        assertNotNull(parcelBoxes.get(0).getItems());
        assertEquals(1, parcelBoxes.get(0).getItems().size());
        assertThat(parcelBoxes.get(0).getItems().get(0),
                allOf(
                        hasProperty("itemId", is(itemId)),
                        hasProperty("count", is(1))
                )
        );
    }
}
