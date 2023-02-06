package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.controller.order.OrderHistoryTestUtil;
import ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_UID;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Корректировки признака предоплаченности заказа")
@DatabaseSetup("/controller/admin/order/before/fully-prepaid-correction-base.xml")
class FullyPrepaidCorrectionTest extends AbstractContextualTest {

    @Autowired
    private TvmClientApi tvmClientApi;

    @Test
    @DisplayName("Заказ был в статусе DELIVERY_DELIVERED")
    @DatabaseSetup("/controller/admin/order/before/status-was-delivery-delivered.xml")
    void prepaidOrderMakePrepaidError() throws Exception {
        makePrepaid()
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("message").value(
                "Cannot update fullyPrepaid flag of order id = 1 because it's delivery segment status was or is OUT"
            ));
    }

    @Test
    @DisplayName("Успешный переход isFullyPrepaid true -> true")
    @DatabaseSetup(
        value = "/controller/admin/order/before/fully-prepaid-true.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/fully-prepaid-correction.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void prepaidOrderMakePrepaidSuccess() throws Exception {
        makePrepaid()
            .andExpect(status().isOk())
            .andExpect(noContent());
    }

    @Test
    @DisplayName("Успешный переход isFullyPrepaid false -> true")
    @DatabaseSetup(
        value = "/controller/admin/order/before/fully-prepaid-false.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/admin/order/after/fully-prepaid-correction.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void prepaidOrderMakeNonPrepaidSuccess() throws Exception {
        TvmClientApiTestUtil.mockTvmClientApi(tvmClientApi);
        makePrepaid()
            .andExpect(status().isOk())
            .andExpect(noContent());
        OrderHistoryTestUtil.assertOrderHistoryEvent(jdbcTemplate, ORDER_ID, USER_UID, null);
    }

    @Nonnull
    private ResultActions makePrepaid() throws Exception {
        return mockMvc.perform(
            post("/admin/orders/make-prepaid")
                .headers(toHttpHeaders(USER_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1}")
        );
    }
}
