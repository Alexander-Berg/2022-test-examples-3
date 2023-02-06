package ru.yandex.market.logistics.tarifficator.controller.shop;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/controller/shop/tariff/db/createTariff.before.xml")
@DisplayName("Тесты на контроллер по работе с тарифами доставки")
class RegionGroupTariffControllerTest extends AbstractContextualTest {
    private static final String URL_TEMPLATE = "/v2/shops/{shopId}/region-groups/{regionGroupId}/tariff?_user_id=100";

    @Test
    @DisplayName("Сохранение дефолтного тарифка")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createDefaultTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveDefaultTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createDefaultTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание тарифа ценового (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createPriceTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createPriceTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createPriceTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновление тарифа ценового (положительный сценарий)")
    @DatabaseSetup("/controller/shop/tariff/db/updatePriceTariff.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/updatePriceTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updatePriceTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/updatePriceTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание тарифа ценово-категорийного (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createCategoryPriceTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createCategoryPriceTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createCategoryPriceTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновление тарифа ценово-категорийного (положительный сценарий)")
    @DatabaseSetup("/controller/shop/tariff/db/updateCategoryPriceTariff.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/updateCategoryPriceTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateCategoryPriceTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/updateCategoryPriceTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание тарифа весово-категорийного (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createCategoryWeightTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createCategoryWeightTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createCategoryWeightTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Обновление тарифа весово-категорийного (положительный сценарий)")
    @DatabaseSetup("/controller/shop/tariff/db/updateCategoryWeightTariff.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/updateCategoryWeightTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateCategoryWeightTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/updateCategoryWeightTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание единого тарифа (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createUniformTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createUniformTariff() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createUniformTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Создание тарифа, где доставка берется из фида (положительный сценарий)")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/createFromFeedTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void createFromFeedTariff() throws Exception {
        performCreateTariff(774L, 1001L, "controller/shop/tariff/json/createFromFeedTariff.json")
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Смена типа тарифа (положительный сценарий)")
    @DatabaseSetup("/controller/shop/tariff/db/updateCategoryWeightTariff.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/tariff/db/changeToUniformTariff.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void changeTariffType() throws Exception {
        performCreateTariff(774L, 1000L, "controller/shop/tariff/json/createUniformTariff.json")
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов")
    @MethodSource("testBadRequestForCreateTariff")
    void testValidationErrors(
        @SuppressWarnings("unused") String testName,
        String requestPath,
        String validationErrorMessage
    ) throws Exception {
        performCreateTariff(774L, 1000L, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(StringUtils.chomp(extractFileContent(validationErrorMessage))));
    }

    private static Stream<Arguments> testBadRequestForCreateTariff() {
        return Stream.of(
            Arguments.of(
                "Валидация javax форматов",
                "controller/shop/tariff/json/invalidTariff1.json",
                "controller/shop/tariff/txt/jacksonValidation.txt"
            ),
            Arguments.of(
                "Валидация ценового тарифа",
                "controller/shop/tariff/json/invalidPriceTariff1.json",
                "controller/shop/tariff/txt/invalidPriceTariff1.txt"
            ),
            Arguments.of(
                "Валидация весового тарифа",
                "controller/shop/tariff/json/invalidWeightTariff1.json",
                "controller/shop/tariff/txt/invalidWeightTariff1.txt"
            ),
            Arguments.of(
                "Валидация категорийного тарифа",
                "controller/shop/tariff/json/invalidCategoryTariff1.json",
                "controller/shop/tariff/txt/invalidCategoryTariff1.txt"
            )
        );
    }

    @Test
    @DisplayName("Успешное получение тарифа")
    @DatabaseSetup("/controller/shop/tariff/db/getRegionGroupTariff.before.xml")
    void getRegionGroupTariffSuccessTest() throws Exception {
        var shopId = 774L;
        var regionGroupId = 1000L;

        mockMvc.perform(get(URL_TEMPLATE, shopId, regionGroupId))
            .andExpect(status().isOk())
            .andExpect(
                content().json(extractFileContent("controller/shop/tariff/json/getRegionGroupTariff.json"))
            );
    }

    @Test
    @DisplayName("Получение несуществующего тарифа - 404")
    void getNonExistentRegionGroupTariffTest() throws Exception {
        var shopId = 0L;
        var regionGroupId = 0L;

        mockMvc.perform(get(URL_TEMPLATE, shopId, regionGroupId))
            .andExpect(status().isNotFound());
    }

    @Nonnull
    private ResultActions performCreateTariff(
        long shopId,
        long regionGroupId,
        String requestFilePath
    ) throws Exception {
        return mockMvc.perform(
            post(URL_TEMPLATE, shopId, regionGroupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }
}
