package ru.yandex.market.logistics.tarifficator.controller.shop;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Интеграционный тест для контроллера RegionGroupDeliveryServicesController")
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml")
public class RegionGroupDeliveryServiceControllerTest extends AbstractContextualTest {

    private static final String SHOP_SERVICES_URL_TEMPLATE = "/v2/shops/{shopId}/delivery-services";
    private static final String REGION_GROUP_SERVICES_URL_TEMPLATE =
        "/v2/shops/{shopId}/region-groups/{regionGroupId}/delivery-services";
    private static final String REGION_GROUP_SERVICE_URL_TEMPLATE =
        "/v2/shops/{shopId}/region-groups/{regionGroupId}/delivery-services/{deliveryServiceId}";

    @Nonnull
    static Stream<Arguments> badCreateRequests() {
        return Stream.of(
            Arguments.of(
                "Создание службы доставки с невалидным идентификатором",
                99,
                "controller/shop/delivery-services/request/createDeliveryService.json",
                "Wrong delivery service"
            ),
            Arguments.of(
                "Создание службы доставки с невалидной стратегией курьерской доставки",
                1002,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidCourierStrategy.json",
                "Invalid delivery service strategy"
            ),
            Arguments.of(
                "Создание службы доставки с невалидной стратегией доставки в ПВЗ",
                1002,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidPickupStrategy.json",
                "Invalid delivery service strategy"
            ),
            Arguments.of(
                "Создание службы доставки с невалидной комбинацией стратегий доставки",
                1002,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidStrategiesCombination.json",
                "Invalid service strategies combination"
            ),
            Arguments.of(
                "Создание службы доставки с невалидным модификатором доставки",
                1002,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidModifier.json",
                "Following validation errors occurred:\n"
                    + "Field: 'courierDeliveryModifiers[0].action', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> badModifyRequests() {
        return Stream.of(
            Arguments.of(
                "Изменение службы доставки с невалидным идентификатором",
                99,
                "controller/shop/delivery-services/request/modifyDeliveryService.json",
                "Wrong delivery service"
            ),
            Arguments.of(
                "Изменение службы доставки с невалидной стратегией курьерской доставки",
                1001,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidCourierStrategy.json",
                "Invalid delivery service strategy"
            ),
            Arguments.of(
                "Изменение службы доставки с невалидной стратегией доставки в ПВЗ",
                1001,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidPickupStrategy.json",
                "Invalid delivery service strategy"
            ),
            Arguments.of(
                "Изменение службы доставки с невалидной комбинацией стратегий доставки",
                1001,
                "controller/shop/delivery-services/request/createDeliveryServiceNonValidStrategiesCombination.json",
                "Invalid service strategies combination"
            ),
            Arguments.of(
                "Изменение службы доставки с невалидным модификатором доставки",
                1001,
                "controller/shop/delivery-services/request/modifyDeliveryServiceNonValidModifier.json",
                "Following validation errors occurred:\n"
                    + "Field: 'pickupDeliveryModifiers[1].action', message: 'must not be null'"
            )
        );
    }

    @Nonnull
    static Stream<Arguments> badBatchModifyRequests() {
        return Stream.of(
            Arguments.of(
                "Пакетное изменение с пустыми списками на модификацию и удаление",
                "controller/shop/delivery-services/request/batchModifyDeliveryServicesEmptyList.json",
                "Expected at least one delivery service to update or delete"
            ),
            Arguments.of(
                "Пакетное изменение с одновременным изменением и удалением одной и той же доставки",
                "controller/shop/delivery-services/request/batchModifyDeliveryServicesModifyAndDeleteSameDS.json",
                "Delivery services cannot be updated and deleted simultaneously"
            ),
            Arguments.of(
                "Пакетное изменение с двойным изменением одной и той же доставки",
                "controller/shop/delivery-services/request/batchModifyDeliveryServicesMultipleModify.json",
                "Delivery services cannot be updated more than once"
            )
        );
    }

    @Test
    @DisplayName("Получение служб доставки для магазина")
    void getShopDeliveryServices() throws Exception {
        performGetShopDeliveryServices(774)
            .andExpect(
                content().json(
                    extractFileContent("controller/shop/delivery-services/response/getShopDeliveryServices.json")
                )
            );
    }

    @Test
    @DisplayName("Получение служб доставки для несуществующего магазина")
    void getNonExistentShopDeliveryServices() throws Exception {
        performGetShopDeliveryServices(88888)
            .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Получение служб доставки для региональной группы")
    void getRegionGroupDeliveryServices() throws Exception {
        performGetRegionGroupDeliveryServices(774, 102)
            .andExpect(
                content().json(
                    extractFileContent(
                        "controller/shop/delivery-services/response/getRegionGroupDeliveryServices.json"
                    )
                )
            );
    }

    @Test
    @DisplayName("Получение служб доставки для региональной группы другого магазина")
    void getNonExistentRegionGroupDeliveryServices() throws Exception {
        performGetRegionGroupDeliveryServices(775, 102)
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Создание новой службы доставки в региональной группе")
    void createDeliveryService() throws Exception {
        performSelectDeliveryService(
            774,
            101,
            1002,
            "controller/shop/delivery-services/request/createDeliveryService.json"
        );
        performGetRegionGroupDeliveryServices(774, 101)
            .andExpect(
                content().json(
                    extractFileContent(
                        "controller/shop/delivery-services/response/createDeliveryService.json"
                    )
                )
            );
    }

    @Test
    @DisplayName("Создание службы доставки с несуществующей региональной группой")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createDeliveryServiceWithNonExistentRegionGroup() throws Exception {
        performSelectDeliveryService(
            774,
            22222,
            1002,
            "controller/shop/delivery-services/request/createDeliveryService.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_REGION_GROUP] with ids [[22222]]"));
    }

    @Test
    @DisplayName("Попытка создать связь с СД для региональной группы другого магазина")
    void testCreateDeliveryServiceLinkForAnotherShop() throws Exception {
        performSelectDeliveryService(
            775,
            101,
            1002,
            "controller/shop/delivery-services/request/createDeliveryService.json"
        )
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов при создании")
    @MethodSource("badCreateRequests")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createDeliveryServiceWithValidationError(
        String testName,
        long deliveryServiceId,
        String requestPath,
        String errorMessage
    ) throws Exception {
        performSelectDeliveryService(774, 101, deliveryServiceId, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Test
    @DisplayName("Изменение существующей службы доставки в региональной группе")
    void modifyDeliveryService() throws Exception {
        performSelectDeliveryService(
            774,
            101,
            1001,
            "controller/shop/delivery-services/request/modifyDeliveryService.json"
        );
        performGetRegionGroupDeliveryServices(774, 101)
            .andExpect(
                content()
                    .json(
                        extractFileContent(
                            "controller/shop/delivery-services/response/modifyDeliveryService.json"
                        )
                    )
            );
    }

    @Test
    @DisplayName("Изменение существующей службы доставки с дефолтными стратегиями")
    void modifyDeliveryServiceWithDefaultStrategies() throws Exception {
        performSelectDeliveryService(
            774,
            101,
            1001,
            "controller/shop/delivery-services/request/modifyDeliveryServiceWithDefaultStrategies.json"
        );
        performGetRegionGroupDeliveryServices(774, 101)
            .andExpect(
                content()
                    .json(
                        extractFileContent(
                            "controller/shop/delivery-services/response/modifyDeliveryServiceWithDefaultStrategies.json"
                        )
                    )
            );
    }

    @Test
    @DisplayName("Изменение существующей службы доставки с несуществующей региональной группой")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void modifyDeliveryServiceWithNonExistentRegionGroup() throws Exception {
        performSelectDeliveryService(
            774,
            11111,
            1001,
            "controller/shop/delivery-services/request/modifyDeliveryService.json"
        )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP_REGION_GROUP] with ids [[11111]]"));
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов при обновлении")
    @MethodSource("badModifyRequests")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void modifyDeliveryServiceWithValidationError(
        String testName,
        long deliveryServiceId,
        String requestPath,
        String errorMessage
    ) throws Exception {
        performSelectDeliveryService(774, 101, deliveryServiceId, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Test
    @DisplayName("Изменение нескольких существующих служб доставки в региональной группе")
    void modifyDeliveryServices() throws Exception {
        performModifyDeliveryServices(
            774,
            102,
            "controller/shop/delivery-services/request/batchModifyDeliveryServices.json"
        );
        performGetRegionGroupDeliveryServices(774, 102)
            .andExpect(
                content()
                    .json(
                        extractFileContent(
                            "controller/shop/delivery-services/response/batchModifyDeliveryServices.json"
                        )
                    )
            );
    }

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @DisplayName("Тестирование невалидных запросов пакетного обновления/удаления")
    @MethodSource("badBatchModifyRequests")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void modifyDeliveryServicesWithValidationError(
        String testName,
        String requestPath,
        String errorMessage
    ) throws Exception {
        performModifyDeliveryServices(774, 102, requestPath)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(errorMessage));
    }

    @Test
    @DisplayName("Удаление службы доставки")
    void deleteDeliveryService() throws Exception {
        performDeleteDeliveryService(774, 102, 1001);
        performGetRegionGroupDeliveryServices(774, 102)
            .andExpect(
                content().json(
                    extractFileContent(
                        "controller/shop/delivery-services/response/deleteDeliveryService.json"
                    )
                )
            );
    }

    @Test
    @DisplayName("Удаление службы доставки для несуществующей региональной группы")
    void deleteDeliveryServiceWithNonExistentRegionGroup() throws Exception {
        performDeleteDeliveryService(774, 33333, 1001)
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Удаление несуществующей службы доставки")
    @ExpectedDatabase(
        value = "/controller/shop/delivery-services/db/regionGroupDeliveryServiceController.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteNonExistentDeliveryService() throws Exception {
        performDeleteDeliveryService(774, 102, 44444)
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Попытка удалить связь с СД для региональной группы другого магазина")
    void testDeleteLinkWithDeliveryServiceOfAnotherShop() throws Exception {
        performDeleteDeliveryService(775, 102, 1001)
            .andExpect(status().isNotFound());
    }

    @Nonnull
    private ResultActions performGetShopDeliveryServices(long shopId) throws Exception {
        return mockMvc.perform(
            get(SHOP_SERVICES_URL_TEMPLATE, shopId)
                .param("_user_id", String.valueOf(221))
        );
    }

    @Nonnull
    private ResultActions performGetRegionGroupDeliveryServices(long shopId, long regionGroupId) throws Exception {
        return mockMvc.perform(
            get(REGION_GROUP_SERVICES_URL_TEMPLATE, shopId, regionGroupId)
                .param("_user_id", String.valueOf(221))
        );
    }

    @Nonnull
    private ResultActions performSelectDeliveryService(
        long shopId,
        long regionGroupId,
        long deliveryServiceId,
        String requestFilePath
    ) throws Exception {
        return mockMvc.perform(
            put(REGION_GROUP_SERVICE_URL_TEMPLATE, shopId, regionGroupId, deliveryServiceId)
                .param("_user_id", String.valueOf(221))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }

    @Nonnull
    private ResultActions performModifyDeliveryServices(
        long shopId,
        long regionGroupId,
        String requestFilePath
    ) throws Exception {
        return mockMvc.perform(
            post(REGION_GROUP_SERVICES_URL_TEMPLATE, shopId, regionGroupId)
                .param("_user_id", String.valueOf(221))
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestFilePath))
        );
    }

    @Nonnull
    private ResultActions performDeleteDeliveryService(
        long shopId,
        long regionGroupId,
        long deliveryServiceId
    ) throws Exception {
        return mockMvc.perform(
            delete(REGION_GROUP_SERVICE_URL_TEMPLATE, shopId, regionGroupId, deliveryServiceId)
                .param("_user_id", String.valueOf(221))
        );
    }

}
