package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.controller.order.AbstractUntieOrderFromShipmentTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;

@DisplayName("Отвязывание заказа от отгрузки через админку")
@DatabaseSetup("/controller/admin/order/before/untie-from-shipment.xml")
class UntieOrderFromShipmentTest extends AbstractUntieOrderFromShipmentTest {
    @Nonnull
    public ResultActions untieOrderFromShipment(long orderId) throws Exception {
        return mockMvc.perform(
            post("/admin/orders/untie-from-shipment")
                .headers(toHttpHeaders(USER_HEADERS))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + orderId + "}")
        );
    }
}
