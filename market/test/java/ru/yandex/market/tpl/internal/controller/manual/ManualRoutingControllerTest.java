package ru.yandex.market.tpl.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.common.db.queue.model.DbQueue;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
class ManualRoutingControllerTest extends BaseTplIntWebTest {

    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    @SneakyThrows
    void verifyOrdersSc() {
        //when
        mockMvc.perform(
                get("/manual/verifyOrdersSc")
                        .param("shiftDate", "2021-12-07")
                        .param("sortingCenterId", "123456")
                        .contentType(MediaType.APPLICATION_JSON)

        ).andExpect(status().is2xxSuccessful());

        //then
        dbQueueTestUtil.assertQueueHasSize(QueueType.VERIFICATION_SHIFT_WITH_SC, 1);
    }
}
