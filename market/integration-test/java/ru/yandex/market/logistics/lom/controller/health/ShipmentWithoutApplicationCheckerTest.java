package ru.yandex.market.logistics.lom.controller.health;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/controller/health/shipments.xml")
@DisplayName("Проверка наличия отгрузок без заявок")
class ShipmentWithoutApplicationCheckerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Нет подходящих некорректных отгрузок")
    void checkWhenNoIncorrectShipments() throws Exception {
        clock.setFixed(Instant.parse("2019-06-10T00:00:00.00Z"), ZoneOffset.UTC);

        checkShipmentsWithoutApplications("0;OK");
    }

    @Test
    @DisplayName("Проверка до 21:15. Есть одна отгрузка без заявки на сегодня")
    void checkWhenThereIsShipmentForToday() throws Exception {
        clock.setFixed(Instant.parse("2019-06-11T21:00:00.00Z"), ZoneOffset.UTC);

        checkShipmentsWithoutApplications(
            "2;Shipments [1] do not have applications or applications in error status"
        );
    }

    @Test
    @DisplayName("Проверка после 21:15. Есть одна отгрузка без заявки на сегодня и одна на завтра")
    void checkWhenThereAreShipmentsForTodayAndTomorrow() throws Exception {
        clock.setFixed(Instant.parse("2019-06-11T21:30:00.00Z"), ZoneOffset.UTC);

        checkShipmentsWithoutApplications(
            "2;Shipments [1, 2] do not have applications or applications in error status"
        );
    }

    @Test
    @DisplayName(
        "Проверка до 21:15. Есть одна отгрузка с заявкой в статусе error на сегодня и " +
            "две отгрузки без заявок в прошлом"
    )
    void checkWhenApplicationInErrorStatus() throws Exception {
        clock.setFixed(Instant.parse("2019-06-13T21:00:00.00Z"), ZoneOffset.UTC);

        checkShipmentsWithoutApplications(
            "2;Shipments [1, 2, 4] do not have applications or applications in error status"
        );
    }

    private void checkShipmentsWithoutApplications(String response) throws Exception {
        mockMvc.perform(get("/health/checkShipmentsWithoutApplications"))
            .andExpect(status().isOk())
            .andExpect(content().string(response));
    }
}
