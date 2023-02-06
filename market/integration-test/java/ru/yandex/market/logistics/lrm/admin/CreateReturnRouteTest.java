package ru.yandex.market.logistics.lrm.admin;

import java.util.UUID;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.front.library.dto.ReferenceObject;
import ru.yandex.market.logistics.lrm.admin.model.enums.AdminReferenceObjectType;
import ru.yandex.market.logistics.lrm.admin.model.request.CreateReturnRouteDto;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.lrm.service.route.RouteService;
import ru.yandex.market.logistics.lrm.utils.RestAssuredTestUtils;
import ru.yandex.market.logistics.lrm.utils.ValidationErrorFields;

import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DisplayName("Создание возвратного маршрута для сегмента")
@DatabaseSetup("/database/admin/create-route/before/prepare.xml")
class CreateReturnRouteTest extends ReturnRouteChangingTest {

    private static final String GET_ROUTE_CREATE_FORM_PATH = "/admin/return-routes/new?parentId=";
    private static final String CREATE_ROUTE_PATH = "/admin/return-routes";
    private static final String CORRECT_ROUTE_PATH = "json/admin/change-route/create/request/route.json";

    private static final long SEGMENT_WITH_ROUTE_ID = 1;
    private static final long SEGMENT_WITHOUT_ROUTE_ID = 2;

    @Autowired
    private RouteService routeService;

    @MethodSource
    @DisplayName("Невалидный json маршрута")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void invalidRoute(
        @SuppressWarnings("unused") String displayName,
        String jsonPath,
        String errorMessagePath
    ) {
        RestAssuredTestUtils.assertJsonResponse(
            createRoute(SEGMENT_WITHOUT_ROUTE_ID, jsonPath),
            HttpStatus.EXPECTATION_FAILED.value(),
            errorMessagePath
        );
    }

    @Test
    @SneakyThrows
    @DisplayName("Маршрут успешно создан")
    @ExpectedDatabase(
        value = "/database/admin/create-route/after/route_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successCreation() {
        RestAssuredTestUtils.assertJsonResponse(
            createRoute(
                SEGMENT_WITHOUT_ROUTE_ID,
                createReturnRouteDto(SEGMENT_WITHOUT_ROUTE_ID, extractFileContent(CORRECT_ROUTE_PATH))
            ),
            "json/admin/change-route/create/response/created_route.json"
        );

        ReturnRouteHistoryTableDescription.ReturnRouteHistory routeHistory = routeService.findRouteByUuid(
            UUID.fromString(TEST_UUID)
        );
        softly.assertThat(routeHistory.route())
            .isEqualTo(objectMapper.readTree(extractFileContent(CORRECT_ROUTE_PATH)));
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибки валидации в теле запроса на создание маршрута")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createRouteValidationOnDtoErrors(
        @SuppressWarnings("unused") String displayName,
        CreateReturnRouteDto createRequest,
        String errorField
    ) {
        RestAssuredTestUtils.assertValidationErrors(
            createRoute(SEGMENT_WITHOUT_ROUTE_ID, createRequest),
            ValidationErrorFields.builder()
                .code("NotNull")
                .field(errorField)
                .message("must not be null")
                .objectName("createReturnRouteDto")
                .errorsPrefix(ValidationErrorFields.DEFAULT_ERRORS_PREFIX)
                .build()
        );
    }

    @Nonnull
    private static Stream<Arguments> createRouteValidationOnDtoErrors() {
        return Stream.of(
            Arguments.of(
                "returnSegment = null",
                CreateReturnRouteDto.builder()
                    .route(FormattedTextObject.of(""))
                    .build(),
                "returnSegment"
            ),
            Arguments.of(
                "route = null",
                CreateReturnRouteDto.builder()
                    .returnSegment(new ReferenceObject())
                    .build(),
                "route"
            )
        );
    }

    @Test
    @DisplayName("Невалидные данные запроса на создание: У сегмента уже есть маршрут")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void invalidCreateRequestConditions() {
        RestAssuredTestUtils.assertUnprocessableEntityServerError(
            createRoute(
                SEGMENT_WITH_ROUTE_ID,
                createReturnRouteDto(SEGMENT_WITH_ROUTE_ID, extractFileContent(CORRECT_ROUTE_PATH))
            ),
            "У сегмента уже существует маршрут"
        );
    }

    @Test
    @DisplayName("Создание маршрута для несуществующего сегмента")
    @ExpectedDatabase(value = "/database/tasks/no_tasks.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    void createRouteForNonExistingSegment() {
        RestAssuredTestUtils.assertNotFoundError(
            createRoute(
                123456789L,
                CreateReturnRouteDto.builder()
                    .returnSegment(new ReferenceObject())
                    .route(FormattedTextObject.of("{}"))
                    .build()
            ),
            "Failed to find RETURN_SEGMENT with ids [123456789]"
        );
    }

    @Test
    @DisplayName("Получение формы создания возвратного маршрута")
    void getRouteCreationForm() {
        RestAssuredTestUtils.assertJsonResponse(
            RestAssured.get(GET_ROUTE_CREATE_FORM_PATH + SEGMENT_WITHOUT_ROUTE_ID),
            "json/admin/change-route/create/response/creation_form.json"
        );
    }

    @Test
    @DisplayName("Получение формы создания возвратного маршрута: у сегмента есть маршрут")
    void getRouteCreationFormForSegmentWithRoute() {
        RestAssuredTestUtils.assertUnprocessableEntityServerError(
            RestAssured.get(GET_ROUTE_CREATE_FORM_PATH + SEGMENT_WITH_ROUTE_ID),
            "У сегмента уже существует маршрут"
        );
    }

    @Test
    @DisplayName("Получение формы создания возвратного маршрута для несуществующего сегмента")
    void getRouteCreationFormForNonExistingSegment() {
        RestAssuredTestUtils.assertNotFoundError(
            RestAssured.get(GET_ROUTE_CREATE_FORM_PATH + 1234567),
            "Failed to find RETURN_SEGMENT with ids [1234567]"
        );
    }

    @Nonnull
    private Response createRoute(Long segmentId, String requestJsonPath) {
        CreateReturnRouteDto requestBody = CreateReturnRouteDto.builder()
            .returnSegment(new ReferenceObject(
                segmentId.toString(),
                segmentId.toString(),
                AdminReferenceObjectType.LRM_RETURN_SEGMENT.getSlug()
            ))
            .route(FormattedTextObject.of(extractFileContent(requestJsonPath)))
            .build();
        return createRoute(segmentId, requestBody);
    }

    @Nonnull
    private Response createRoute(Long segmentId, CreateReturnRouteDto createReturnRouteDto) {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .queryParam("parentId", segmentId)
            .body(createReturnRouteDto)
            .post(CREATE_ROUTE_PATH);
    }

    @Nonnull
    private static CreateReturnRouteDto createReturnRouteDto(Long segmentId, String routeText) {
        return CreateReturnRouteDto.builder()
            .returnSegment(new ReferenceObject(
                segmentId.toString(),
                segmentId.toString(),
                AdminReferenceObjectType.LRM_RETURN_SEGMENT.getSlug()
            ))
            .route(FormattedTextObject.of(routeText))
            .build();
    }
}
