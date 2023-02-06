package ru.yandex.market.replenishment.autoorder.api;

import java.time.LocalDateTime;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockLogin
public class DemandAutoprocessingControllerTest extends ControllerTest {

    private static final LocalDateTime MOCK_DATE = LocalDateTime.of(2022, 2, 24, 4, 0);

    @Before
    public void mockMethods() {
        setTestTime(MOCK_DATE);
    }

    @Test
    @DbUnitDataSet(before = "DemandAutoprocessingControllerTest.simple.before.csv")
    public void getWithTomorrowDate() throws Exception {
        mockMvc.perform(get("/api/v1/autoprocessing/trace?demandType=TYPE_1P&demandId=2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.success").isArray())
            .andExpect(jsonPath("$.success").isNotEmpty())
            .andExpect(jsonPath("$.success").value(Matchers
                .hasItems("Спец Заказ не xdock и не mono xdock или явно разрешенные ответственные",
                    "Заказ не нулевой суммы", "Заказ не содержит позиций со слишком большим стоком")))
            .andExpect(jsonPath("$.failure").isArray())
            .andExpect(jsonPath("$.failure").isNotEmpty())
            .andExpect(jsonPath("$.failure").value(Matchers
                .containsInAnyOrder("Дата заказа на сегодня", "Включен автозаказ в лог параметрах","quota exceed for 1")));
    }

    @Test
    @DbUnitDataSet(before = "DemandAutoprocessingControllerTest.simple.before.csv")
    public void getAutoprocessDemand() throws Exception {
        mockMvc.perform(get("/api/v1/autoprocessing/trace?demandType=TYPE_1P&demandId=1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$.success").isArray())
            .andExpect(jsonPath("$.success", hasItems(
                "Дата заказа на сегодня",
                "Включен автозаказ в лог параметрах",
                "Спец Заказ не xdock и не mono xdock или явно разрешенные ответственные",
                "Заказ не нулевой суммы",
                "Заказ не содержит позиций со слишком большим стоком")))
            .andExpect(jsonPath("$.failure").isArray())
            .andExpect(jsonPath("$.failure").isEmpty());
    }
}
