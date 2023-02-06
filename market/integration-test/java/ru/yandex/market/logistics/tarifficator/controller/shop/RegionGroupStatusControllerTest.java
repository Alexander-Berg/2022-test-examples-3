package ru.yandex.market.logistics.tarifficator.controller.shop;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/controller/shop/region-group-status/db/handleStatuses.before.xml")
@DisplayName("Тесты на контроллер, ответственный за работу со статусами региональных групп")
public class RegionGroupStatusControllerTest extends AbstractContextualTest {
    private static final String GET_URL_TEMPLATE =
        "/v2/shops/{shopId}/region-groups/{regionGroupId}/status?_user_id=100";

    @Test
    @DisplayName("Тестирование валидационных ошибок при обновлении статуса региональной группы")
    void testValidationErrorsOnStatusesUpdate() throws Exception {
        performUpdateStatuses(774L, "controller/shop/region-group-status/json/updateStatuses.invalid.json")
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                StringUtils.chomp(extractFileContent("controller/shop/region-group-status/txt/validation.txt"))
            ));
    }

    @Test
    @DisplayName("Тест обновления статусов (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/region-group-status/db/handleStatuses.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testStatusUpdate() throws Exception {
        performUpdateStatuses(774L, "controller/shop/region-group-status/json/updateStatuses.valid.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Тест обновления статусов, пустой список обновлений (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/region-group-status/db/handleStatuses.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testStatusUpdateEmptyStatusesList() throws Exception {
        performUpdateStatuses(774L, "controller/shop/region-group-status/json/updateStatuses.empty.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Тест обновления статусов, группа не найдена")
    void testStatusUpdateGroupNotFound() throws Exception {
        performUpdateStatuses(774L, "controller/shop/region-group-status/json/updateStatuses.groupNotFound.json")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_REGION_GROUP] with ids [[1]]"));
    }

    @Test
    @DisplayName("Тест успешного получения статуса")
    @DatabaseSetup("/controller/shop/region-group-status/db/getRegionGroupStatus.before.xml")
    void getRegionGroupStatusSuccessTest() throws Exception {
        var shopId = 774L;
        var regionGroupId = 1000L;

        mockMvc.perform(get(GET_URL_TEMPLATE, shopId, regionGroupId))
            .andExpect(status().isOk())
            .andExpect(
                content().json(extractFileContent(
                    "controller/shop/region-group-status/json/getRegionGroupStatus.expected.json"
                ))
            );
    }

    @Test
    @DisplayName("Тест получения статуса несуществующей региональной группы - 404")
    void getNonExistentRegionGroupStatusTest() throws Exception {
        var shopId = 0L;
        var regionGroupId = 0L;

        mockMvc.perform(get(GET_URL_TEMPLATE, shopId, regionGroupId))
            .andExpect(status().isNotFound())
            .andExpect(
                errorMessage(String.format("Failed to find [SHOP_REGION_GROUP] with ids [[%d]]", regionGroupId))
            );
    }

    @Nonnull
    private ResultActions performUpdateStatuses(
        long shopId,
        String requestFilePath
    ) throws Exception {
        return mockMvc.perform(
            post("/v2/shops/{shopId}/region-groups/statuses?_user_id=100", shopId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }
}
