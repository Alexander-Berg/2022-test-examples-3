package ru.yandex.market.replenishment.autoorder.api;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@WithMockLogin
public class GeneratedDataControllerTest extends ControllerTest {

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "GeneratedDataControllerTest_getStatus.csv")
    public void getStatusTest() {
        mockMvc.perform(get("/api/v2/generated_data/12512"))
            .andExpect(status().isOk())
            .andExpect(re -> log.debug("response - {}", re.getResponse().getContentAsString()))
            .andExpect(content().json("{\"id\":12512,\"status\":\"PENDING\"}"));
    }

    @Test
    @SneakyThrows
    @DbUnitDataSet(before = "GeneratedDataControllerTest_getStatus.csv")
    public void getStatusEmptyTest() {
        mockMvc.perform(get("/api/v2/generated_data/124"))
            .andExpect(status().isNotFound())
            .andExpect(re -> log.debug("response - {}", re.getResponse().getContentAsString()))
            .andExpect(content().json("{\"message\":\"Файл не найден №124\"}"));
    }
}
