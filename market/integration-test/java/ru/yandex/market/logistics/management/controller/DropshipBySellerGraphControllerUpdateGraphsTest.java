package ru.yandex.market.logistics.management.controller;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.util.NestedServletException;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.entity.request.dropshipBySeller.UpdateDeliveryServiceCalendarRequest;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.management.util.TestUtil.hasResolvedExceptionContainingMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Батчевое обновление графов для DBS-партнеров")
@DatabaseSetup("/data/controller/dropshipBySellerGraph/before/service_codes.xml")
class DropshipBySellerGraphControllerUpdateGraphsTest extends AbstractContextualAspectValidationTest {
    private static final List<UpdateDeliveryServiceCalendarRequest> EMPTY_REQUEST = List.of();
    private static final List<UpdateDeliveryServiceCalendarRequest> LIST_OF_NULL = Collections.singletonList(null);

    @Autowired
    private ObjectMapper objectMapper;

    @SneakyThrows
    @JpaQueriesCount(0)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME + " {1}")
    @MethodSource("validateRequestBodyArguments")
    @DisplayName("Ошибки валидации в ручке обновления графа для дбс")
    void validateRequestBody(
        List<UpdateDeliveryServiceCalendarRequest> request,
        String message
    ) {
        updateDropshipBySellerGraphs(objectMapper.writeValueAsString(request))
            .andExpect(status().isBadRequest())
            .andExpect(hasResolvedExceptionContainingMessage(message));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestBodyArguments() {
        return Stream.of(
            Arguments.of(EMPTY_REQUEST, "must not be empty"),
            Arguments.of(LIST_OF_NULL, "must not be null")
        );
    }

    // TODO DELIVERY-44883 Нестабильная работа интеграционных тестов из-за кеширования кодов логистических сервисов
    //  Тест не работает, если запускать его одного из-за расхождения счётчика в JpaQueriesCount,
    //  но работает, если запускать его вместе с соседним тестами
    @Test
    @SneakyThrows
    @JpaQueriesCount(103)
    @DisplayName("Успешное обновление дбс графа")
    @DatabaseSetup("/data/controller/dropshipBySellerGraph/before/dbs_partners.xml")
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/after/all_partners_graph_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void allLogisticSegmentsWillBeCreatedForAllPartners() {
        updateDropshipBySellerGraphs(extractFileContent(
            "data/controller/dropshipBySellerGraph/request/update_batch.json"
        ))
            .andExpect(status().isOk());
    }

    @Test
    @JpaQueriesCount(51)
    @DisplayName("Неуспешное обновление дбс графа: есть дублирующийся WAREHOUSE сегмент")
    @DatabaseSetup("/data/controller/dropshipBySellerGraph/before/dbs_partners_with_incorrect_segment.xml")
    @ExpectedDatabase(
        value = "/data/controller/dropshipBySellerGraph/before/dbs_partners_with_incorrect_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failToUpdateGraphsBecauseOfDuplicateSegments() {
        softly.assertThatCode(
                () ->
                    updateDropshipBySellerGraphs(extractFileContent(
                        "data/controller/dropshipBySellerGraph/request/update_batch.json"
                    ))
            )
            .isInstanceOf(NestedServletException.class)
            .hasMessage(
                "Request processing failed; "
                    + "nested exception is java.lang.IllegalStateException: "
                    + "DBS partner 49851 expected to have exactly one active logistic segment of type WAREHOUSE"
            );
    }

    @Nonnull
    @SneakyThrows
    private ResultActions updateDropshipBySellerGraphs(String requestBody) {
        return mockMvc.perform(
            put("/externalApi/dropship-by-seller/graphs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        );
    }
}
