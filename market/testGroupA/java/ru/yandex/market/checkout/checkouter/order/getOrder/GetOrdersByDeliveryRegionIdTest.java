package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.viewmodel.RecentOrderViewModel;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.MultiCartProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByDeliveryRegionIdTest extends AbstractWebTestBase {

    private static final Long UID = 2234562L;
    @Autowired
    private TestSerializationService testSerializationService;

    @BeforeAll
    public void setUp() {
        createOrder(213L);
        createOrder(2L);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }


    private Order createOrder(long regionId) {
        MultiCart multiCart = MultiCartProvider
                .createBuilder()
                .order(OrderProvider.getBlueOrder())
                .regionId(regionId)
                .build();

        Buyer buyer = BuyerProvider.getBuyer();
        buyer.setUid(UID);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(multiCart, buyer);

        Order order = orderCreateHelper.createOrder(parameters);
        MatcherAssert.assertThat(order.getDelivery().getRegionId(), CoreMatchers.is(regionId));
        return order;
    }

    @Test
    public void shouldGetOrdersByDeliveryRegion() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(MockMvcRequestBuilders.get("/orders/by-uid/{userId}", UID)
                .param(CheckouterClientParams.RGB, Color.BLUE.name())
                .param(CheckouterClientParams.DELIVERY_REGION_ID, "213"))
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        PagedOrders pagedOrders = testSerializationService.deserializeCheckouterObject(response.getContentAsString(),
                PagedOrders.class);

        MatcherAssert.assertThat(pagedOrders.getItems(), hasSize(1));
        MatcherAssert.assertThat(pagedOrders.getItems().iterator().next().getDelivery().getRegionId(),
                CoreMatchers.is(213L));
    }

    @Test
    public void shouldGetRecentOrdersByDeliveryRegion() throws Exception {
        MockHttpServletResponse response = mockMvc.perform(
                MockMvcRequestBuilders.get("/orders/by-uid/{userId}/recent", UID)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.DELIVERY_REGION_ID, "213")
                        .param(CheckouterClientParams.STATUS, OrderStatus.UNPAID.name())
                        .param(CheckouterClientParams.OPTIONAL_PARTS, OptionalOrderPart.DELIVERY.name())
        )
                .andDo(log())
                .andExpect(status().isOk())
                .andReturn().getResponse();

        List<RecentOrderViewModel> recentOrderViewModels =
                testSerializationService.deserializeCheckouterObject(response.getContentAsString(),
                        new TypeReference<List<RecentOrderViewModel>>() {
                        }.getType(), null);

        MatcherAssert.assertThat(recentOrderViewModels, hasSize(1));
        MatcherAssert.assertThat(recentOrderViewModels.get(0).getDelivery().getRegionId(), CoreMatchers.is(213L));
    }
}
