package ru.yandex.market.checkout.carter.health;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled
public class CarterTaskHealthControllerTest extends CarterMockedDbTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void tasksHealthSmokeTest() throws Exception {
        mockMvc.perform(get("/tasks/health/solomon"))
                .andDo(log())
                .andExpect(status().isOk());
    }

    @Test
    public void taskPeriodMillisTest() throws Exception {
        mockMvc.perform(get("/tasks/health/solomon"))
                .andDo(log())
                .andExpect(
                        jsonPath(
                                "$[?(@.id=='a_green_cart_cleaner_job')].periodMillis"
                        ).value(CoreMatchers.everyItem(CoreMatchers.equalTo(1800000)))
                )
                .andExpect(
                        jsonPath(
                                "$[?(@.id=='a_green_cart_cleaner_job')].type.expression.program"
                        ).value(CoreMatchers.everyItem(CoreMatchers.containsString("period = 'one_min'")))
                );
    }

    @Test
    public void taskCrucialNotificationChannelTest() throws Exception {
        mockMvc.perform(get("/tasks/health/solomon"))
                .andDo(log())
                .andExpect(
                        jsonPath(
                                "$[?(@.id=='a_green_cart_cleaner_job')].notificationChannels[0]"
                        ).value(CoreMatchers.everyItem(CoreMatchers.is("telegram")))
                );
    }
}
