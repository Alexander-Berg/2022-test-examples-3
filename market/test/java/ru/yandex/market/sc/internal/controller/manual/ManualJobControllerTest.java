package ru.yandex.market.sc.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author: dbryndin
 * @date: 1/24/22
 */
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManualJobControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final ConfigurationService configurationService;

    @BeforeEach
    void init() {
        testFactory.storedSortingCenter(1);
    }

    @Test
    @SneakyThrows
    void successSendSegmentFfStatusHistoryToSqs() {
        configurationService.mergeValue(ConfigurationProperties.SEND_STATUS_CARGO_EVENTS_TO_SQS_ENABLED, true);
        configurationService.mergeValue(ConfigurationProperties.LAST_SENT_SEGMENT_FF_STATUS_HISTORY_ITEM_ID, 0);

        mockMvc.perform(
                MockMvcRequestBuilders.post("/manual/job/sendSegmentFfStatusHistoryToSqs")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }
}
