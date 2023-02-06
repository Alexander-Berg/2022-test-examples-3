package ru.yandex.market.logistics.nesu.admin.logistic_point_availability;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopRole;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopShipmentType;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityDetailDto;
import ru.yandex.market.logistics.nesu.admin.model.response.LogisticPointAvailabilityDetailDto.LogisticPointAvailabilityDetailDtoBuilder;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/logistic-point-availability/before/prepare_data.xml")
class LogisticPointAvailabilityUpdateTest extends BaseLogisticPointAvailabilityTest {
    @Test
    @DisplayName("Получение записи редактирования конфигурации доступности складов для магазинов")
    void getDetail() throws Exception {
        getForm()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/get_detail.json"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
        verifyGetPartner(LOGISTIC_POINT_1_PARTNER_ID);
    }

    @Test
    @DisplayName("Получение записи редактирования с новыми полями ограничений")
    @DatabaseSetup(
        value = "/repository/logistic-point-availability/before/restrictions.xml",
        type = DatabaseOperation.REFRESH
    )
    void getDetailRestrictions() throws Exception {
        getForm()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/get_detail_restrictions.json"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
        verifyGetPartner(LOGISTIC_POINT_1_PARTNER_ID);
    }

    @Test
    @DisplayName(
        "Получение записи редактирования конфигурации доступности складов для магазинов "
            + "по несуществующему идентификатору"
    )
    void getDetailUnknownId() throws Exception {
        mockMvc.perform(get("/admin/logistic-point-availability/4"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [4]"));
    }

    @Test
    @DisplayName("Редактирование конфигурации доступности складов для магазинов")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/update_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void update() throws Exception {
        update(2, getDetailDtoBaseBuilder().shipmentType(AdminShopShipmentType.IMPORT).build())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/update_result.json"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
        verifyGetPartner(LOGISTIC_POINT_1_PARTNER_ID);
    }

    @Test
    @DisplayName("Редактирование конфигурации доступности: нарушено ограничение уникальности")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/before/prepare_data.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateNotUnique() throws Exception {
        update(2, getDetailDtoBaseBuilder().build())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(
                "Конфигурация для логистической точки с идентификатором 1, локации 21651, "
                + "типа отгрузки \"Забор\" и типа партнёра \"DropShip\" уже существует."
            ));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
    }

    @Test
    @DisplayName("Редактирование конфигурации доступности с новыми полями ограничений")
    @DatabaseSetup("/repository/logistic-point-availability/before/forbidden_cargo_types.xml")
    @ExpectedDatabase(
        value = "/repository/logistic-point-availability/after/update_result_restrictions.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateRestrictions() throws Exception {
        update(
            1,
            getDetailDtoBaseBuilder()
                .ordersPerPartnerLimit(100)
                .build()
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/logistic-point-availability/update_result_restrictions.json"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
        verifyGetPartner(LOGISTIC_POINT_1_PARTNER_ID);
    }

    @Test
    @DisplayName("Редактирование конфигурации доступности складов для магазинов по несуществующему идентификатору")
    void updateUnknownId() throws Exception {
        update(4, getDetailDtoBaseBuilder().build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTIC_POINT_AVAILABILITY] with ids [4]"));
    }

    @Test
    @DisplayName("Проставление несуществующего склада в конфигурацию доступности склада при редактировании")
    void updateUnknownLogisticPoint() throws Exception {
        when(lmsClient.getLogisticsPoint(LOGISTIC_POINT_1_ID)).thenReturn(Optional.empty());

        update(2, getDetailDtoBaseBuilder().logisticPointId(LOGISTIC_POINT_1_ID).build())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [LOGISTICS_POINT] with ids [1]"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
    }

    @Test
    @DisplayName("Не найден партнёр склада в конфигурацию доступности склада при редактировании")
    void updateUnknownLogisticPointPartner() throws Exception {
        when(lmsClient.getPartner(LOGISTIC_POINT_1_PARTNER_ID)).thenReturn(Optional.empty());

        update(1, getDetailDtoBaseBuilder().logisticPointId(LOGISTIC_POINT_1_ID).build())
            .andExpect(status().isInternalServerError())
            .andExpect(errorMessage("Missing [PARTNER] with ids [10]"));

        verifyGetLogisticPoint(LOGISTIC_POINT_1_ID);
        verifyGetPartner(LOGISTIC_POINT_1_PARTNER_ID);
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("updatingArgumentsWithNullRequiredFields")
    @DisplayName("Попытка редактирования конфигурации доступности складов с незаполненными обязательными полями")
    void updateLogisticPointAvailabilityBadRequest(
        @SuppressWarnings("unused") String caseName,
        LogisticPointAvailabilityDetailDto detailDto,
        String errorFieldName
    ) throws Exception {
        update(1, detailDto)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(List.of(
                fieldError(errorFieldName, "must not be null", "logisticPointAvailabilityDetailDto", "NotNull")
            )));
    }

    @Nonnull
    public static Stream<Arguments> updatingArgumentsWithNullRequiredFields() {
        return Stream.of(
            Arguments.of(
                "Не указан идентификатор логистической точка",
                getDetailDtoBaseBuilder().logisticPointId(null).build(),
                "logisticPointId"
            ),
            Arguments.of(
                "Не указан идентификатор локации",
                getDetailDtoBaseBuilder().locationId(null).build(),
                "locationId"
            ),
            Arguments.of(
                "Не указан тип магазина-партнёра",
                getDetailDtoBaseBuilder().shopRole(null).build(),
                "shopRole"
            ),
            Arguments.of(
                "Не указан тип отгрузки",
                getDetailDtoBaseBuilder().shipmentType(null).build(),
                "shipmentType"
            )
        );
    }

    @Nonnull
    private static LogisticPointAvailabilityDetailDtoBuilder getDetailDtoBaseBuilder() {
        return LogisticPointAvailabilityDetailDto.builder()
            .logisticPoint(new ReferenceObject(
                String.valueOf(LOGISTIC_POINT_1_ID),
                LOGISTIC_POINT_1_NAME,
                "lms/logistics-point"
            ))
            .partnerName(new ReferenceObject(
                String.valueOf(LOGISTIC_POINT_1_PARTNER_ID),
                LOGISTIC_POINT_1_PARTNER_NAME,
                "lms/partner"
            ))
            .logisticPointId(LOGISTIC_POINT_1_ID)
            .locationId(21651)
            .shipmentType(AdminShopShipmentType.WITHDRAW)
            .shopRole(AdminShopRole.DROPSHIP)
            .partnerLimit(10)
            .partnerCount(1)
            .enabled(true);
    }

    @Nonnull
    private ResultActions getForm() throws Exception {
        return mockMvc.perform(get("/admin/logistic-point-availability/1"));
    }

    @Nonnull
    private ResultActions update(long id, LogisticPointAvailabilityDetailDto request) throws Exception {
        return mockMvc.perform(request(HttpMethod.PUT, "/admin/logistic-point-availability/" + id, request));
    }
}
