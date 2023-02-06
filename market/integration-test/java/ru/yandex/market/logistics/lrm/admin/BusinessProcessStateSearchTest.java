package ru.yandex.market.logistics.lrm.admin;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import io.restassured.RestAssured;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.lrm.admin.model.enums.AdminBusinessProcessStatus;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminBusinessProcessType;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminEntityType;
import ru.yandex.market.logistics.lrm.admin.model.filter.AdminBusinessProcessStateSearchFilterDto;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.lrm.utils.ValidationErrorFields;

import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Поиск фоновых процессов в админке по фильтру")
@ParametersAreNonnullByDefault
@DatabaseSetup("/database/admin/search-business-process-state/before/setup.xml")
class BusinessProcessStateSearchTest extends AbstractAdminIntegrationTest {

    private static final String GET_BUSINESS_PROCESS_STATES_PATH = "/admin/business-process-states";

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск бизнес-процессов по фильтру")
    void searchByFilter(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFilterDto filter,
        String expectedJsonPath,
        @SuppressWarnings("unused") String expectedJsonPathForEntity
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given().params(toParams(filter)).get(GET_BUSINESS_PROCESS_STATES_PATH),
            expectedJsonPath
        );
    }

    @Nonnull
    private static Stream<Arguments> searchByFilter() {
        return Stream.of(
            Arguments.of(
                "По пустому фильтру - возвращаются все бизнес процессы",
                filter(),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "По всем статусам бизнес-процессов",
                filter().setStatuses(Set.of(AdminBusinessProcessStatus.values())),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "По нескольким статусам бизнес-процессов",
                filter()
                    .setStatuses(Set.of(AdminBusinessProcessStatus.CREATED, AdminBusinessProcessStatus.REQUEST_SENT)),
                "json/admin/search-business-process-state/multiply_statuses.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "По типу бизнес-процесса",
                filter().setTypes(Set.of(AdminBusinessProcessType.DELETE_SEGMENT_IN_SC)),
                "json/admin/search-business-process-state/by_business_process_type.json",
                "json/admin/search-business-process-state/empty.json"
            ),
            Arguments.of(
                "По идентификатору запроса",
                filter().setRequestId("test-request-id-2"),
                "json/admin/search-business-process-state/by_request_id.json",
                "json/admin/search-business-process-state/empty.json"
            ),
            Arguments.of(
                "Только по идентификатору связанной сущности - возвращаются все бизнес-процессы",
                filter().setEntityId(1L),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "Только по типу связанной сущности - возвращаются все бизнес-процессы",
                filter().setEntityType(AdminEntityType.RETURN_SEGMENT),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "По типу и идентификатору связанной сущности",
                filter()
                    .setEntityId(1L)
                    .setEntityType(AdminEntityType.RETURN_SEGMENT),
                "json/admin/search-business-process-state/by_segment_id.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "По идентификатору сегмента",
                filter().setSegmentId(1L),
                "json/admin/search-business-process-state/by_segment_id.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "Поиск по дате создания",
                filter()
                    .setCreatedFrom(LocalDate.of(2021, 11, 1))
                    .setCreatedTo(LocalDate.of(2021, 11, 20)),
                "json/admin/search-business-process-state/by_created.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "Поиск с пустым сетом статусов",
                filter().setStatuses(Set.of()),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            ),
            Arguments.of(
                "Поиск с пустым сетом типов",
                filter().setTypes(Set.of()),
                "json/admin/search-business-process-state/all_business_processes.json",
                "json/admin/search-business-process-state/with_return_segment.json"
            )
        );
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() {
        RestAssuredTestUtils.assertJsonParameter(
            RestAssured.given()
                .params("size", "2")
                .get(GET_BUSINESS_PROCESS_STATES_PATH),
            "totalCount",
            6
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидации фильтра")
    void validationErrors(
        @SuppressWarnings("unused") String displayName,
        String validationErrorParamKey,
        List<String> validationErrorParamValues,
        ValidationErrorFields validationErrorFields
    ) {
        MultiValueMap<String, String> paramMultiMap = new LinkedMultiValueMap<>();
        paramMultiMap.add("empty", "false");
        paramMultiMap.addAll(validationErrorParamKey, validationErrorParamValues);

        RestAssuredTestUtils.assertValidationErrors(
            RestAssured.given()
                .params(paramMultiMap)
                .get(GET_BUSINESS_PROCESS_STATES_PATH),
            validationErrorFields
        );
    }

    @Nonnull
    private static Stream<Arguments> validationErrors() {
        return Stream.of(
            Arguments.of(
                "Null в списке типов",
                "types",
                List.of("DELETE_SEGMENT_IN_SC", ""),
                validationErrorFieldsBuilder()
                    .field("types[]")
                    .build()
            ),
            Arguments.of(
                "Null в списке статусов",
                "statuses",
                List.of("CREATED", ""),
                validationErrorFieldsBuilder()
                    .field("statuses[]")
                    .build()
            )
        );
    }

    @MethodSource("searchByFilter")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Поиск бизнес-процессов по фильтру для сущности")
    void searchByFilterForEntity(
        @SuppressWarnings("unused") String displayName,
        AdminBusinessProcessStateSearchFilterDto filter,
        @SuppressWarnings("unused") String expectedJsonPath,
        String expectedJsonPathForEntity
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given().params(toParams(filter.setEntityId(1L).setEntityType(AdminEntityType.RETURN_SEGMENT)))
                .get(GET_BUSINESS_PROCESS_STATES_PATH),
            expectedJsonPathForEntity
        );
    }

    @Test
    @DisplayName("Поиск бизнес-процессов по фильтру для сущности, с типом сущности не связан процесс")
    void searchByFilterForEntityNoLink() {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.given().params(toParams(filter().setReturnId(123L)))
                .get(GET_BUSINESS_PROCESS_STATES_PATH),
            "json/admin/search-business-process-state/empty.json"
        );
    }

    @Nonnull
    private static ValidationErrorFields.ValidationErrorFieldsBuilder validationErrorFieldsBuilder() {
        return ValidationErrorFields.builder()
            .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
            .objectName("adminBusinessProcessStateSearchFilterDto")
            .code("NotNull")
            .message("must not be null");
    }

    @Nonnull
    private static AdminBusinessProcessStateSearchFilterDto filter() {
        return new AdminBusinessProcessStateSearchFilterDto();
    }
}
