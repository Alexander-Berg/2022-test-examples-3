package ru.yandex.market.checkout.checkouter.order.getOrder;

import java.nio.charset.StandardCharsets;

import io.qameta.allure.junit4.Tag;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.GetOrdersUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.json.Names.Buyer.ASSESSOR;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedGetRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.parameterizedPostRequest;
import static ru.yandex.market.checkout.helpers.utils.GetOrdersUtils.resultMatcherCount;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GetOrdersByAssessorTest extends AbstractWebTestBase {

    private Order orderWithAssessor;
    private Order orderWithoutAssessor;

    @Autowired
    @Qualifier("mbiCurator")
    private CuratorFramework curatorFramework;

    @BeforeAll
    public void init() throws Exception {
        super.setUpBase();
        createOrSetUids();
        Parameters parameters = new Parameters(BuyerProvider.getBuyerAssessor());
        orderWithAssessor = orderCreateHelper.createOrder(parameters);

        parameters = new Parameters(BuyerProvider.getBuyer());
        orderWithoutAssessor = orderCreateHelper.createOrder(parameters);
    }

    @AfterEach
    @Override
    public void tearDownBase() {
    }

    @AfterAll
    public void tearDownAll() {
        super.tearDownBase();
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы по assessor для GET /orders/by-uid")
    @Test
    public void getByAssessorByUidTest() throws Exception {
        mockMvc.perform(
                parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.ASSESSOR_UID + "?" + ASSESSOR + "={assessor}")
                        .build(Boolean.TRUE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.RGB, Color.BLUE.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                contains(hasEntry("id", orderWithAssessor.getId().intValue()))
                        )
                );

        mockMvc.perform(
                parameterizedGetRequest("/orders/by-uid/" + BuyerProvider.UID + "?" + ASSESSOR + "={assessor}")
                        .build(Boolean.FALSE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name())
                        .param(CheckouterClientParams.RGB, Color.BLUE.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                contains(hasEntry("id", orderWithoutAssessor.getId().intValue()))
                        )
                );
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы по assessor для GET /orders")
    @Test
    public void getByAssessorByGetGetOrdersTest() throws Exception {
        getByAssessorTest(parameterizedGetRequest("/orders?" + ASSESSOR + "={assessor}&rgb=BLUE,WHITE"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Получить заказы по assessor для POST /get-orders")
    @Test
    public void getByAssessorByPostGetOrdersTest() throws Exception {
        getByAssessorTest(parameterizedPostRequest("/get-orders",
                "{\"assessor\": %s, \"rgbs\":[\"BLUE\",\"WHITE\"]}"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("Посчитать заказы по assessor для POST /get-orders")
    @Test
    public void getByAssessorCountsTest() throws Exception {
        mockMvc.perform(
                get("/orders/count")
                        .param(ASSESSOR, Boolean.TRUE.toString())
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));

        mockMvc.perform(
                get("/orders/count")
                        .param(ASSESSOR, Boolean.FALSE.toString())
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(resultMatcherCount(1));
    }

    private void getByAssessorTest(GetOrdersUtils.ParameterizedRequest<Boolean> request) throws Exception {
        mockMvc.perform(
                request
                        .build(Boolean.TRUE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                contains(hasEntry("id", orderWithAssessor.getId().intValue()))
                        )
                );

        mockMvc.perform(
                request
                        .build(Boolean.FALSE)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.orders[*]",
                                contains(hasEntry("id", orderWithoutAssessor.getId().intValue()))
                        )
                );
    }

    private void createOrSetUids() throws Exception {
        byte[] data = ("[" + BuyerProvider.ASSESSOR_UID + "]").getBytes(StandardCharsets.UTF_8);
        if (curatorFramework.checkExists().forPath("/checkout/assessor/uids") == null) {
            curatorFramework.create()
                    .creatingParentsIfNeeded()
                    .forPath("/checkout/assessor/uids", data);
        } else {
            curatorFramework.setData()
                    .forPath("/checkout/assessor/uids", data);
        }
    }
}
