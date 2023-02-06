package ru.yandex.market.checkout.checkouter.order.getOrder;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.helpers.CancellationRequestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByHasCancellationRequestTest extends AbstractWebTestBase {

    @Autowired
    private CancellationRequestHelper cancellationRequestHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private TestSerializationService testSerializationService;

    private Order orderWithoutCancellationRequest;
    private Order orderWithCancellationRequest;

    @BeforeAll
    public void setUp() throws Exception {
        super.setUpBase();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        orderWithCancellationRequest = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(orderWithCancellationRequest);
        cancellationRequestHelper.createCancellationRequest(
                orderWithCancellationRequest.getId(),
                new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, ""),
                new ClientInfo(ClientRole.USER, orderWithCancellationRequest.getBuyer().getUid())
        );

        orderWithoutCancellationRequest = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(orderWithoutCancellationRequest);
    }

    @AfterEach
    @Override
    public void tearDownBase() {

    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Test
    public void testHasCancellationRequestTrue() throws Exception {
        OrderSearchRequest request = new OrderSearchRequest();
        request.hasCancellationRequest = true;
        request.setRgbs(new Color[]{Color.BLUE});
        request.orderIds = Lists.newArrayList(orderWithCancellationRequest.getId(),
                orderWithoutCancellationRequest.getId());

        PagedOrders pagedOrders = postGetOrders(request, ClientInfo.SYSTEM);

        Assertions.assertTrue(pagedOrders.getItems().stream()
                .anyMatch(o -> o.getId().equals(orderWithCancellationRequest.getId())), "should contain order with " +
                "cancellation request");
        Assertions.assertTrue(pagedOrders.getItems().stream()
                .noneMatch(o -> o.getId().equals(orderWithoutCancellationRequest.getId())), "should not contain order" +
                " without cancellation request");
    }

    @Test
    public void testHasCancellationRequestFalse() throws Exception {
        OrderSearchRequest request = new OrderSearchRequest();
        request.hasCancellationRequest = false;
        request.setRgbs(new Color[]{Color.BLUE});
        request.orderIds = Lists.newArrayList(orderWithCancellationRequest.getId(),
                orderWithoutCancellationRequest.getId());

        PagedOrders pagedOrders = postGetOrders(request, ClientInfo.SYSTEM);

        Assertions.assertTrue(pagedOrders.getItems().stream()
                .anyMatch(o -> o.getId().equals(orderWithoutCancellationRequest.getId())), "should contain order " +
                "without cancellation request");
        Assertions.assertTrue(pagedOrders.getItems().stream()
                .noneMatch(o -> o.getId().equals(orderWithCancellationRequest.getId())), "should not contain order " +
                "with cancellation request");
    }

    private PagedOrders postGetOrders(OrderSearchRequest request, ClientInfo clientInfo) throws Exception {
        return testSerializationService.deserializeCheckouterObject(mockMvc.perform(post("/get-orders")
                .param("clientRole", clientInfo.getRole().name())
                .param("clientId", String.valueOf(clientInfo.getId()))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(testSerializationService.serializeCheckouterObject(request)))
                .andReturn()
                .getResponse()
                .getContentAsString(), PagedOrders.class);
    }
}
