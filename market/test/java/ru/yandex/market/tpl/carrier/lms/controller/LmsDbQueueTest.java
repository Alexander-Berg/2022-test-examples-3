package ru.yandex.market.tpl.carrier.lms.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.dbqueue.controller.DbQueueSlug;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LmsDbQueueTest extends LmsControllerTest {

    @SneakyThrows
    @Test
    public void shouldGetDbQueuePage() {
        mockMvc.perform(get(DbQueueSlug.TASKS))
                .andExpect(status().isOk());
    }
}
