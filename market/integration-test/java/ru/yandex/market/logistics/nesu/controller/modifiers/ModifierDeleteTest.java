package ru.yandex.market.logistics.nesu.controller.modifiers;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Удаление модификаторов опций доставки")
@DatabaseSetup({
    "/repository/shop-deliveries-availability/setup.xml",
    "/controller/modifier/modifier_setup.xml",
    "/controller/modifier/modifier_available_directly_delivery.xml"
})
class ModifierDeleteTest extends AbstractContextualTest {

    @Autowired
    private ModifierUploadTaskProducer producer;

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(producer);
    }

    @Test
    @DisplayName("Удаление модификатора")
    @ExpectedDatabase(
        value = "/controller/modifier/deleted_modifier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void deleteModifier() throws Exception {
        deleteModifier(1, 1)
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Не найден сендер")
    void deleteModifierSenderNotFound() throws Exception {
        deleteModifier(1, 2)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SENDER] with ids [2]"));
    }

    @Test
    @DisplayName("Не найден модификатор")
    void deleteModifierNotFound() throws Exception {
        deleteModifier(2, 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DELIVERY_OPTION_MODIFIER] with ids [2]"));
    }

    @Test
    @DisplayName("Попытка удаления модификатора другого сендера")
    void deleteNotAccessibleModifier() throws Exception {
        deleteModifier(1, 11)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [DELIVERY_OPTION_MODIFIER] with ids [1]"));
    }

    @Nonnull
    private ResultActions deleteModifier(long modifierId, long senderId) throws Exception {
        return mockMvc.perform(
            delete("/back-office/settings/modifiers/" + modifierId)
                .param("shopId", "1")
                .param("senderId", String.valueOf(senderId))
                .param("userId", "1")
        );
    }

}
