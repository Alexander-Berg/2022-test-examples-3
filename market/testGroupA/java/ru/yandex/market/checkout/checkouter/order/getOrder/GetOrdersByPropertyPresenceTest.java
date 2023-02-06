package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.util.Collection;
import java.util.EnumSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderProperty;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static java.util.Arrays.asList;
import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByPropertyPresenceTest extends AbstractWebTestBase {

    private static Long uid;
    private Order orderP1;
    private Order orderP1P2;
    private Order orderP2P3;
    private OrderProperty srcProperty1;
    private OrderProperty srcProperty2;
    private OrderProperty srcProperty3;
    @Autowired
    private TestSerializationService testSerializationService;
    private BiFunction<Collection<OrderProperty>, Collection<OrderProperty>, MockHttpServletRequestBuilder>
            requestProvider;

    public static Stream<Arguments> parameterizedTestData() {
        return of(
                new Object[]{
                        "POST /get-orders",
                        ((BiFunction<Collection<OrderProperty>, Collection<OrderProperty>,
                                MockHttpServletRequestBuilder>) (
                                GetOrdersByPropertyPresenceTest::createPostRequest
                        ))
                },
                new Object[]{
                        "GET /orders",
                        ((BiFunction<Collection<OrderProperty>, Collection<OrderProperty>,
                                MockHttpServletRequestBuilder>) (
                                GetOrdersByPropertyPresenceTest::createGetOrdersRequest
                        ))
                },
                new Object[]{
                        "GET /orders/by-uid",
                        ((BiFunction<Collection<OrderProperty>, Collection<OrderProperty>,
                                MockHttpServletRequestBuilder>) (
                                GetOrdersByPropertyPresenceTest::createGetByUidRequest
                        ))
                }
        ).map(Arguments::of);
    }

    private static MockHttpServletRequestBuilder createPostRequest(Collection<OrderProperty> havingProperties,
                                                                   Collection<OrderProperty> notHavingProperties
    ) {
        return MockMvcRequestBuilders.post("/get-orders")
                .content(
                        buildPostSearchRequest(havingProperties, notHavingProperties)
                )
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name());
    }

    private static MockHttpServletRequestBuilder createGetByUidRequest(Collection<OrderProperty> havingProperties,
                                                                       Collection<OrderProperty> notHavingProperties
    ) {
        final MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.get("/orders/by-uid" + "/" + uid);
        if (havingProperties != null) {
            havingProperties.forEach(
                    hp -> rb.param(CheckouterClientParams.HAVING_PROPERTY, hp.getName())
            );
        }

        if (notHavingProperties != null) {
            notHavingProperties.forEach(
                    np -> rb.param(CheckouterClientParams.NOT_HAVING_PROPERTY, np.getName())
            );
        }

        return rb
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name());
    }

    private static MockHttpServletRequestBuilder createGetOrdersRequest(Collection<OrderProperty> havingProperties,
                                                                        Collection<OrderProperty> notHavingProperties
    ) {
        final MockHttpServletRequestBuilder rb = MockMvcRequestBuilders.get("/orders");
        if (havingProperties != null) {
            havingProperties.forEach(
                    hp -> rb.param(CheckouterClientParams.HAVING_PROPERTY, hp.getName())
            );
        }

        if (notHavingProperties != null) {
            notHavingProperties.forEach(
                    np -> rb.param(CheckouterClientParams.NOT_HAVING_PROPERTY, np.getName())
            );
        }

        return rb
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                .param(CheckouterClientParams.RGB, Color.BLUE.name());
    }

    private static String buildPostSearchRequest(
            Collection<OrderProperty> havingProperties,
            Collection<OrderProperty> notHavingProperties) {
        OrderSearchRequest request = new OrderSearchRequest();

        if (havingProperties != null) {
            request.havingProperties =
                    havingProperties.stream().map(OrderProperty::getName).collect(Collectors.toList());
        }

        if (notHavingProperties != null) {
            request.notHavingProperties =
                    notHavingProperties.stream().map(OrderProperty::getName).collect(Collectors.toList());
        }

        request.rgbs = EnumSet.of(Color.BLUE);

        try {
            return new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException jpex) {
            throw new RuntimeException(jpex);
        }
    }

    @BeforeAll
    public void setUp() throws Exception {
        srcProperty1 = new OrderProperty(null, "testProperty1", "testValue1");
        srcProperty2 = new OrderProperty(null, "testProperty2", "testValue2");
        srcProperty3 = new OrderProperty(null, "testProperty3", "testValue3");
        final Order order1 = OrderProvider.getBlueOrder();
        uid = order1.getUid();
        order1.addProperty(srcProperty1);
        this.orderP1 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(order1));
        MatcherAssert.assertThat(orderP1.getPropertiesForJson(), hasEntry("testProperty1", "testValue1"));
        final Order order2 = OrderProvider.getBlueOrder();
        order2.addProperty(srcProperty1);
        order2.addProperty(srcProperty2);
        this.orderP1P2 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(order2));
        MatcherAssert.assertThat(orderP1P2.getPropertiesForJson(), hasEntry("testProperty2", "testValue2"));
        final Order order3 = OrderProvider.getBlueOrder();
        order3.addProperty(srcProperty2);
        order3.addProperty(srcProperty3);
        this.orderP2P3 = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(order3));
        MatcherAssert.assertThat(orderP2P3.getPropertiesForJson(), hasEntry("testProperty3", "testValue3"));
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: поиск по наличию проперти.")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void havingPropertiesTest(String caseName, BiFunction<Collection<OrderProperty>, Collection<OrderProperty>,
            MockHttpServletRequestBuilder> requestProvider) throws Exception {
        this.requestProvider = requestProvider;
        performTest(asList(srcProperty1), null, asList(orderP1, orderP1P2), asList(orderP2P3));
        performTest(asList(srcProperty2), null, asList(orderP1P2, orderP2P3), asList(orderP1));
        performTest(asList(srcProperty3), null, asList(orderP2P3), asList(orderP1, orderP1P2));
        performTest(asList(srcProperty2, srcProperty3), null, asList(orderP2P3), asList(orderP1, orderP1P2));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: поиск по наличию и отсутствию проперти.")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void havingAndNotHavingPropertiesTest(String caseName, BiFunction<Collection<OrderProperty>,
            Collection<OrderProperty>, MockHttpServletRequestBuilder> requestProvider) throws Exception {
        this.requestProvider = requestProvider;
        performTest(asList(srcProperty1), asList(srcProperty2), asList(orderP1), asList(orderP2P3, orderP1P2));
        performTest(asList(srcProperty2), asList(srcProperty3), asList(orderP1P2), asList(orderP1, orderP2P3));
        performTest(asList(srcProperty3), asList(srcProperty1), asList(orderP2P3), asList(orderP1, orderP1P2));
        performTest(asList(srcProperty2, srcProperty3), asList(srcProperty1),
                asList(orderP2P3), asList(orderP1, orderP1P2));
    }

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: поиск по наличию и отсутствию проперти.")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    public void notHavingPropertiesTest(String caseName, BiFunction<Collection<OrderProperty>,
            Collection<OrderProperty>, MockHttpServletRequestBuilder> requestProvider) throws Exception {
        this.requestProvider = requestProvider;
        performTest(null, asList(srcProperty1), asList(orderP2P3), asList(orderP1, orderP1P2));
        performTest(null, asList(srcProperty3), asList(orderP1, orderP1P2), asList(orderP2P3));
        performTest(null, asList(srcProperty2, srcProperty3), asList(orderP1), asList(orderP1P2, orderP2P3));
    }

    private void performTest(Collection<OrderProperty> havingProperties,
                             Collection<OrderProperty> notHavingProperties,
                             Collection<Order> expectedOrders,
                             Collection<Order> notExpectedOrders) throws Exception {
        performRequest(expectedOrders, notExpectedOrders, requestProvider.apply(havingProperties, notHavingProperties));
    }

    private void performRequest(Collection<Order> expectedOrders,
                                Collection<Order> notExpectedOrders,
                                MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(
                request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].id").isArray())
                .andExpect(jsonPath("$.orders[*].id").value(
                        hasItems(expectedOrders.stream()
                                .map(o -> o.getId().intValue())
                                .toArray(Integer[]::new)
                        )
                        )
                )
                .andExpect(jsonPath("$.orders[*].id").value(
                        Matchers.not(
                                hasItem(
                                        anyOf(
                                                notExpectedOrders.stream()
                                                        .map(o -> o.getId().intValue())
                                                        .map(Matchers::equalTo)
                                                        .collect(Collectors.toList())
                                        )
                                )
                        )
                ));
    }
}
