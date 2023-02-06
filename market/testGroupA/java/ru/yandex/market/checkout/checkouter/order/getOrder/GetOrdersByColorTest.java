package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collections;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByColorTest extends AbstractWebTestBase {

    @Autowired
    private TestSerializationService testSerializationService;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{null, OrderProvider.SHOP_ID},
                new Object[]{Color.WHITE, OrderProvider.SHOP_ID},
                new Object[]{Color.BLUE, OrderProvider.SHOP_ID}
        ).map(Arguments::of);
    }

    @BeforeAll
    public void setUp() {
        Parameters green = new Parameters();
        green.setColor(Color.WHITE);

        Parameters blue = defaultBlueOrderParameters();
        blue.setColor(Color.BLUE);

        orderCreateHelper.createOrder(green);
        orderCreateHelper.createOrder(blue);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldFilterInGetOrders(Color color, long shopId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders")
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
        if (color != null) {
            builder = builder.param(CheckouterClientParams.RGB, color.name());
        }

        mockMvc.perform(builder)
                .andExpect(jsonPath("$.orders")
                        .value(hasSize(color == null ? 2 : 1))
                ).andExpect(jsonPath("$.orders[*].rgb")
                        .value(hasItem(ObjectUtils.firstNonNull(color, Color.BLUE).name()))
                );
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldFilterInGetOrdersByUid(Color color, long shopId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/by-uid/{uid}", BuyerProvider.UID);
        if (color != null) {
            builder = builder.param(CheckouterClientParams.RGB, color.name());
        }
        mockMvc.perform(builder)
                .andExpect(jsonPath("$.orders")
                        .value(hasSize(color == null ? 2 : 1)))
                .andExpect(jsonPath("$.orders[*].rgb")
                        .value(hasItem(ObjectUtils.firstNonNull(color, Color.BLUE).name())));
    }


    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldFilterInPostGetOrders(Color color, long shopId) throws Exception {
        OrderSearchRequest orderSearchRequest = new OrderSearchRequest();
        if (color != null) {
            orderSearchRequest.rgbs = Collections.singleton(color);
        }

        mockMvc.perform(
                post("/get-orders")
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .content(testSerializationService.serializeCheckouterObject(orderSearchRequest))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(
                jsonPath("$.orders").value(hasSize(color == null ? 2 : 1))
        ).andExpect(
                jsonPath("$.orders[*].rgb").value(hasItem(ObjectUtils.firstNonNull(color, Color.BLUE).name()))
        );
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void shouldCountByColor(Color color, long shopId) throws Exception {
        MockHttpServletRequestBuilder builder = get("/orders/count")
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());

        if (color != null) {
            builder.param(CheckouterClientParams.RGB, color.name());
        }

        mockMvc.perform(builder)
                .andExpect(jsonPath("$.value").value(color == null ? 2 : 1));
    }
}
