package ru.yandex.market.adv.content.manager.controller.juggler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.adv.content.manager.AbstractContentManagerTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Date: 09.12.2021
 * Project: adv-content-manager
 *
 * @author alexminakov
 */
@DisplayName("Тесты на endpoint GET /juggler/moderation/status.")
class GetJugglerModerationStatusTest extends AbstractContentManagerTest {

    @Autowired
    private MockMvc mvc;

    @DisplayName("Вернули код 0 и сообщение OK.")
    @DbUnitDataSet(
            before = "GetJugglerModerationStatusTest/csv/" +
                    "checkModerationStatus_allEnded_emptyList.before.csv"
    )
    @Test
    void checkModerationStatus_allEnded_emptyList() throws Exception {
        checkModerationStatus("checkModerationStatus_allEnded_emptyList");
    }

    @DisplayName("Вернули код 2 и сообщение об ошибочных модерациях.")
    @DbUnitDataSet(
            before = "GetJugglerModerationStatusTest/csv/" +
                    "checkModerationStatus_expectStickModeration_emptyList.before.csv"
    )
    @Test
    void checkModerationStatus_expectStickModeration_emptyList() throws Exception {
        checkModerationStatus("checkModerationStatus_expectStickModeration_emptyList");
    }

    private void checkModerationStatus(String methodName) throws Exception {
        mvc.perform(
                        get("/juggler/moderation/status")
                                .contentType(MediaType.TEXT_PLAIN_VALUE)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN_VALUE))
                .andExpect(content().string(
                                loadFile("GetJugglerModerationStatusTest/json/response/" + methodName + ".txt")
                                        .trim()
                        )
                );
    }
}
