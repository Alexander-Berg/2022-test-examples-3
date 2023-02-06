package ru.yandex.market.logistics.lom.controller.order;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.lom.utils.TestUtils.toHttpHeaders;

@DatabaseSetup("/controller/admin/order/before/untie-from-shipment.xml")
public class UntieOrderFromShipmentTestImpl extends AbstractUntieOrderFromShipmentTest {
    @Nonnull
    @Override
    public ResultActions untieOrderFromShipment(long orderId) throws Exception {
        return mockMvc.perform(
            post(String.format("/orders/%d/untie-from-shipment", orderId))
                .headers(toHttpHeaders(USER_HEADERS))
        );
    }
}
