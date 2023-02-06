package ru.yandex.market.logistics.tarifficator.controller.shop;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты на контроллер по работе с информацией о настроенной доставке магазином")
public class ShopDeliveryStateControllerTest extends AbstractContextualTest {
    private static final String GET_URL_TEMPLATE = "/v2/shops/delivery/state";

    @Test
    @DatabaseSetup("/controller/shop/state/db/getState.before.xml")
    @DisplayName("Успешное получение информации о доставке")
    void getDeliveryState() throws Exception {
        mockMvc.perform(post(GET_URL_TEMPLATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/shop/state/json/getState.request.json"))
            )
            .andExpect(status().isOk())
            .andExpect(
                content().json(extractFileContent(
                    "controller/shop/state/json/getState.response.json"
                ))
            );
    }

    @Test
    @DisplayName("Валидационная ошибка (пустой список магазинов в запросе)")
    void getDeliveryStateInvalidRequest() throws Exception {
        mockMvc.perform(post(GET_URL_TEMPLATE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/shop/state/json/getState.invalid.request.json"))
            )
            .andExpect(status().isBadRequest())
            .andExpect(
                content().json(extractFileContent(
                    "controller/shop/state/json/getState.invalid.response.json"
                ))
            );
    }

    @Test
    @DisplayName("Тестирование возвращения состояния доставки")
    @DatabaseSetup("/controller/shop/state/db/getDeliverySummary.before.xml")
    void testGetDeliverySummary() throws Exception {
        performDeliverySummaryRequest(774L)
            .andExpect(status().isOk())
            .andExpect(
                jsonContent("controller/shop/state/json/getDeliverySummary.response.json")
            );
    }

    @Nonnull
    private ResultActions performDeliverySummaryRequest(long shopId) throws Exception {
        return mockMvc.perform(
            get("/v2/shops/" + shopId + "/delivery/summary")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
