package ru.yandex.market.tpl.carrier.planner.controller.manual;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseAddress;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseRepository;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.service.manual.ManualTimezonesTool;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


class ManualToolsControllerTest extends BasePlannerWebTest {

    @Autowired
    OrderWarehouseRepository orderWarehouseRepository;

    @Autowired
    @Qualifier("timezonesToolMock")
    ManualTimezonesTool manualTimezonesTool;

    @Test
    @Disabled
    @SneakyThrows
    void shouldInvokeTimezonesTool() {

        createOrderWarehouseWith(null, null);
        createOrderWarehouseWith(120564, null);
        createOrderWarehouseWith(null, "Europe/Moscow");
        createOrderWarehouseWith(120564, "Europe/Moscow");

        manualTimezonesTool.invokeTool();

        Assertions.assertEquals(0, orderWarehouseRepository.findAll().stream()
                .filter(ow -> ow.getRegionId() == null || ow.getTimezone() == null)
                .count());
    }

    private OrderWarehouse createOrderWarehouseWith(Integer regionId, String timezone) {
        BigDecimal lat = new BigDecimal("55.675250");
        BigDecimal lon = new BigDecimal("37.567405");
        OrderWarehouseAddress address = new OrderWarehouseAddress("abc", "cde", "efg", "ads", "asd", "12", "2", "1",
                1, lon, lat);
        OrderWarehouse ow = new OrderWarehouse(
                "123",
                "OOO inc",
                address,
                Map.of(),
                List.of("123123123"),
                "test",
                "Ivan Ivanych",
                regionId,
                timezone
        );
        return orderWarehouseRepository.save(ow);
    }

    @Test
    @Disabled
    @SneakyThrows
    void shouldTestGeocoderAndGeobase() {
        OrderWarehouse ow1 = createOrderWarehouseAt("55.675250", "37.567405"); // msk
        OrderWarehouse ow2 = createOrderWarehouseAt("55.030204", "82.920430"); // nsk
        OrderWarehouse ow3 = createOrderWarehouseAt("-22.912957", "-43.233993"); //rio

        mockMvc.perform(post("/tools/timezones"))
                .andExpect(status().isOk());
        Assertions.assertEquals(120564, ow1.getRegionId());
        Assertions.assertEquals(65, ow2.getRegionId());
        Assertions.assertEquals(21221, ow3.getRegionId());

        Assertions.assertEquals("Europe/Moscow", ow1.getTimezone());
        Assertions.assertEquals("Asia/Novosibirsk", ow2.getTimezone());
        Assertions.assertEquals("America/Sao_Paulo", ow3.getTimezone());
    }

    private OrderWarehouse createOrderWarehouseAt(String latitude, String longitude) {
        BigDecimal lat = new BigDecimal(latitude);
        BigDecimal lon = new BigDecimal(longitude);
        OrderWarehouse ow = new OrderWarehouse(
                "123",
                "OOO inc",
                new OrderWarehouseAddress("abc", "cde", "efg", "ads", "asd", "12", "2", "1", 1, lon, lat),
                Map.of(),
                List.of("123123123"),
                "test",
                "Ivan Ivanych",
                null,
                null
        );
        return orderWarehouseRepository.save(ow);
    }
}
