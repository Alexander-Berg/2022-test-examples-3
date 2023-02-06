package ru.yandex.market.logistics.tarifficator.controller.shop;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.exception.http.ResourceType;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleCreateRequest;
import ru.yandex.market.logistics.tarifficator.model.dto.shop.PickupPointDeliveryRuleUpdateRequest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PickupPointDeliveryRuleStatus;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тестирование операций над правилами доставки ПВЗ магазинов")
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@ParametersAreNonnullByDefault
class PickupPointDeliveryRuleControllerTest extends AbstractContextualTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Удаление существующего правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/delete.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/after/deleteExisting.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deleteExisting() throws Exception {
        performDeleteRuleRequest(10000L).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Удаление несуществующего правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/delete.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/before/delete.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void deleteNonExisting() throws Exception {
        long ruleId = 10L;
        performDeleteRuleRequest(ruleId).andExpect(status().isNotFound())
            .andExpect(expectedResourceNotFoundError(
                    ResourceType.PICKUP_POINT_DELIVERY_RULE,
                    ruleId
                )
            );
    }

    @Test
    @DisplayName("Сохранение нового правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/after/save.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveNew() throws Exception {
        performSaveRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/save.request.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/pickup-point-delivery-rule/request/save.request.after.json",
                "updatedAt",
                "createdAt"
            ));
    }

    @Test
    @DisplayName("Сохранение нового правила доставки ПВЗ магазина со всеми полями")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/after/saveAllFields.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void saveAllFieldsNew() throws Exception {
        performSaveRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/saveAllFields.request.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/pickup-point-delivery-rule/request/saveAllFields.request.after.json",
                "updatedAt",
                "createdAt"
            ));
    }

    @Test
    @DisplayName("Сохранение правила доставки ПВЗ магазина с уже существуюшим идентификатором лог.точки в системе LMS")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    void saveAlreadyPresentLmsLogisticsPointId() throws Exception {
        performSaveRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/saveLmsIdPresent.request.before.json"
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/pickup-point-delivery-rule/request/saveLmsIdPresent.request.after.json",
                "updatedAt",
                "createdAt"
            ));
    }

    @Test
    @DisplayName("Сохранение правила доставки ПВЗ магазина с идентификатором несуществующего магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    void saveInvalidShopId() throws Exception {
        performSaveRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/saveInvalidShopId.request.json"
        ).andExpect(status().isNotFound())
            .andExpect(expectedResourceNotFoundError(ResourceType.SHOP_META_DATA, 1L));
    }

    @Test
    @DisplayName("Обновление правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/after/update.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void update() throws Exception {
        performUpdateRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/update.request.json",
            100000
        ).andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/pickup-point-delivery-rule/request/update.request.json",
                "updatedAt",
                "createdAt"
            ));
    }

    @Test
    @DisplayName("Обновление несуществующего правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/pickup-point-delivery-rule/before/save.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void updateNonExisting() throws Exception {
        performUpdateRuleRequest(
            "controller/shop/pickup-point-delivery-rule/request/update.request.json",
            1
        ).andExpect(status().isNotFound());
    }

    @Nonnull
    private ResultActions performDeleteRuleRequest(long id) throws Exception {
        return mockMvc.perform(delete(String.format("/shops/pickup-points/delivery-rule/%d", id)));
    }

    @Nonnull
    private ResultActions updateRuleRequest(String rule, long id) throws Exception {
        return mockMvc.perform(
            put(String.format("/shops/pickup-points/delivery-rule/%d", id))
                .contentType(MediaType.APPLICATION_JSON)
                .content(rule));
    }

    @Nonnull
    private ResultActions saveRuleRequest(String rule) throws Exception {
        return mockMvc.perform(
            post("/shops/pickup-points/delivery-rule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(rule));
    }

    @Nonnull
    private ResultActions performUpdateRuleRequest(String path, long id) throws Exception {
        return updateRuleRequest(extractFileContent(path), id);
    }

    @Nonnull
    private ResultActions performUpdateRuleRequest(
        PickupPointDeliveryRuleUpdateRequest rule, long id
    ) throws Exception {
        return updateRuleRequest(objectMapper.writeValueAsString(rule), id);
    }

    @Nonnull
    private ResultActions performSaveRuleRequest(String path) throws Exception {
        return saveRuleRequest(extractFileContent(path));
    }

    @Nonnull
    private ResultActions performSaveRuleRequest(PickupPointDeliveryRuleCreateRequest rule) throws Exception {
        return saveRuleRequest(objectMapper.writeValueAsString(rule));
    }

    @Nonnull
    private static ResultMatcher expectedValidationError(String field, String message) {
        return errorMessage(String.format(
            "Following validation errors occurred:\nField: '%s', message: '%s'",
            field,
            message
        ));
    }

    @Nonnull
    private ResultMatcher expectedResourceNotFoundError(ResourceType resourceType, long resourceId) {
        return errorMessage(
            String.format(
                "Failed to find [%s] with ids [[%d]]",
                resourceType,
                resourceId
            )
        );
    }

    @MethodSource({"invalidCreate", "invalidUpdate"})
    @DisplayName("Валидация при сохранении правила доставки ПВЗ магазина")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void invalidCreate(
        String field,
        String message,
        Consumer<PickupPointDeliveryRuleCreateRequest> operator
    ) throws Exception {
        PickupPointDeliveryRuleCreateRequest request = defaultRequest();
        operator.accept(request);
        performSaveRuleRequest(request).andExpect(expectedValidationError(field, message));
    }

    @MethodSource
    @DisplayName("Валидация при обновлении правила доставки ПВЗ магазина")
    @DatabaseSetup("/controller/shop/pickup-point-delivery-rule/before/save.before.xml")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void invalidUpdate(
        String field,
        String message,
        Consumer<PickupPointDeliveryRuleUpdateRequest> operator
    ) throws Exception {
        PickupPointDeliveryRuleUpdateRequest request = defaultRequest();
        operator.accept(request);
        performUpdateRuleRequest(request, 100000).andExpect(expectedValidationError(field, message));
    }

    @Nonnull
    private static Stream<Arguments> invalidCreate() {
        return Stream.<Triple<String, String, Consumer<PickupPointDeliveryRuleCreateRequest>>>of(
            Triple.of(
                "lmsLogisticsPointId",
                "must not be null",
                r -> r.setLmsLogisticsPointId(null)
            ),
            Triple.of(
                "shopId",
                "must not be null",
                r -> r.setShopId(null)
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }


    @Nonnull
    private static Stream<Arguments> invalidUpdate() {
        return Stream.<Triple<String, String, Consumer<PickupPointDeliveryRuleCreateRequest>>>of(
            Triple.of(
                "status",
                "must not be null",
                r -> r.setStatus(null)
            ),
            Triple.of(
                "pickupPointType",
                "must not be null",
                r -> r.setPickupPointType(null)
            ),
            Triple.of(
                "orderBeforeHour",
                "must be greater than or equal to 0",
                r -> r.setOrderBeforeHour(-1)
            ),
            Triple.of(
                "orderBeforeHour",
                "must be less than or equal to 24",
                r -> r.setOrderBeforeHour(25)
            ),
            Triple.of(
                "daysFrom",
                "must be greater than or equal to 0",
                r -> r.setDaysFrom(-1)
            ),
            Triple.of(
                "daysTo",
                "must be greater than or equal to 0",
                r -> r.setDaysTo(-1)
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Nonnull
    private PickupPointDeliveryRuleCreateRequest defaultRequest() {
        return (PickupPointDeliveryRuleCreateRequest) new PickupPointDeliveryRuleCreateRequest()
            .setShopId(774L)
            .setLmsLogisticsPointId(1L)
            .setStatus(PickupPointDeliveryRuleStatus.ACTIVE)
            .setPickupPointType("TERMINAL")
            .setOrderBeforeHour(1)
            .setDaysFrom(1)
            .setDaysTo(2);
    }

}
