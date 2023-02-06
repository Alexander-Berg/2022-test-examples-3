package ru.yandex.market.logistics.nesu.controller.settings;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты настроек средних размеров заказов АПИ SettingsController")
class SettingsControllerMeasuresTest extends AbstractSettingsControllerTest {

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
    @DisplayName("Получение информации о средних габаритах и весе заказа сендера по умолчанию")
    @DatabaseSetup("/service/sender/before/get_senders.xml")
    void getDefaultAverageOrderMeasures() throws Exception {
        mockMvc.perform(get("/back-office/settings/sender/measures/default"))
            .andExpect(jsonContent("controller/settings/sender/sender_default_average_measures.json"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Получение пустой информации о средних габаритах и весе заказа сендера")
    @DatabaseSetup("/service/sender/before/get_senders.xml")
    void getEmptyAverageOrderMeasures() throws Exception {
        getMeasures(2L)
            .andExpect(content().json("{}"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Получение информации о средних габаритах и весе заказа сендера")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void getAverageOrderMeasures() throws Exception {
        getMeasures(1L)
            .andExpect(jsonContent("controller/settings/sender/sender_custom_average_measures.json"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание средних значений заказа сендера")
    @DatabaseSetup("/service/sender/before/sender_without_average_measures.xml")
    void createAverageOrderMeasures() throws Exception {
        getMeasures(1L)
            .andExpect(content().json("{}"))
            .andExpect(status().isOk());

        String requestPath = "controller/settings/sender/updated_sender_average_measures.json";
        createOrUpdateMeasures(requestPath)
            .andExpect(status().isOk());

        getMeasures(1L)
            .andExpect(jsonContent(requestPath))
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Обновление средних значений заказа сендера")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void updateAverageOrderMeasures() throws Exception {
        getMeasures(1L)
            .andExpect(jsonContent("controller/settings/sender/sender_custom_average_measures.json"))
            .andExpect(status().isOk());

        String requestPath = "controller/settings/sender/updated_sender_average_measures.json";
        createOrUpdateMeasures(requestPath)
            .andExpect(status().isOk());

        getMeasures(1L)
            .andExpect(jsonContent(requestPath))
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Обновление с округлением веса до десятых долей")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void updateAverageOrderMeasuresWeightRounding() throws Exception {
        createOrUpdateMeasures("controller/settings/sender/rounding_sender_average_measures.json")
            .andExpect(status().isOk());

        getMeasures(1L)
            .andExpect(jsonContent("controller/settings/sender/rounding_sender_average_measures_response.json"))
            .andExpect(status().isOk());

        verify(producer).produceTask(1);
    }

    @Test
    @DisplayName("Попытка использования отрицательных размеров")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void updateAverageOrderMeasuresNegative() throws Exception {
        createOrUpdateMeasures("controller/settings/sender/negative_sender_average_measures.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError("height", "must be greater than 0", "averageOrderMeasuresDto", "Positive"),
                fieldError("length", "must be greater than 0", "averageOrderMeasuresDto", "Positive"),
                fieldError("weight", "must be greater than 0", "averageOrderMeasuresDto", "Positive"),
                fieldError("width", "must be greater than 0", "averageOrderMeasuresDto", "Positive")
            )));
    }

    @Test
    @DisplayName("Попытка использования размеров больше 500 см")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void updateAverageOrderMeasuresOversize() throws Exception {
        String errorMessage = "must be less than or equal to 500";
        createOrUpdateMeasures("controller/settings/sender/oversize_sender_average_measures.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(
                    "height",
                    errorMessage,
                    "averageOrderMeasuresDto",
                    "Max",
                    Map.of("value", 500)
                ),
                fieldError(
                    "length",
                    errorMessage,
                    "averageOrderMeasuresDto",
                    "Max",
                    Map.of("value", 500)
                ),
                fieldError(
                    "width",
                    errorMessage,
                    "averageOrderMeasuresDto",
                    "Max",
                    Map.of("value", 500)
                )
            )));
    }

    @Test
    @DisplayName("Попытка использования большого веса заказа")
    @DatabaseSetup("/service/sender/before/sender_with_average_measures.xml")
    void updateAverageOrderMeasuresWeightOversize() throws Exception {
        createOrUpdateMeasures("controller/settings/sender/oversize_weight_sender_average_measures.json")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(fieldError(
                "weight",
                "must be less than or equal to 1500",
                "averageOrderMeasuresDto",
                "Max",
                Map.of("value", 1500)
            )));
    }

    @Nonnull
    private ResultActions createOrUpdateMeasures(String requestPath) throws Exception {
        return mockMvc.perform(
            post("/back-office/settings/sender/measures")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        );
    }

    @Nonnull
    private ResultActions getMeasures(Long senderId) throws Exception {
        return mockMvc.perform(
            get("/back-office/settings/sender/measures")
                .param("userId", "1")
                .param("shopId", "1")
                .param("senderId", senderId.toString())
        );
    }
}
