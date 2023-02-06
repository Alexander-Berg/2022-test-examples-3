package ru.yandex.market.logistics.nesu.controller.document;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.PageSize;
import ru.yandex.market.logistics.nesu.dto.document.OrderLabelRequest;
import ru.yandex.market.logistics.nesu.jobs.producer.LabelsFileGenerationProducer;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация файла с ярлыками")
@DatabaseSetup("/controller/document/before/generate_labels_file_setup.xml")
class DocumentGenerateLabelsFileTest extends AbstractContextualTest {
    @Autowired
    private LomClient lomClient;

    @Autowired
    private LabelsFileGenerationProducer labelsFileGenerationProducer;

    @Test
    @DisplayName("Запуск формирования файла с ярлыками заказов")
    @ExpectedDatabase(
        value = "/controller/document/after/generate_labels_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateLabelsFile() throws Exception {
        Set<Long> ordersIds = Set.of(2L, 3L);

        doNothing().when(labelsFileGenerationProducer).produceTask(ordersIds, PageSize.A4, 3);
        mockLomSearchOrders(ordersIds);

        generateLabelsFile(ordersIds)
            .andExpect(status().isOk())
            .andExpect(content().string("3"));

        verify(labelsFileGenerationProducer).produceTask(ordersIds, PageSize.A4, 3L);
    }

    @Test
    @DisplayName("Существует процесс генерации файла для указанных параметров в статусе ERROR")
    @ExpectedDatabase(
        value = "/controller/document/after/generate_labels_file_error_found.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateLabelsFileLabelGenerationTaskExistsError() throws Exception {
        Set<Long> ordersIds = Set.of(1L, 2L);

        doNothing().when(labelsFileGenerationProducer).produceTask(ordersIds, PageSize.A4, 2);
        mockLomSearchOrders(ordersIds);

        generateLabelsFile(ordersIds)
            .andExpect(status().isOk())
            .andExpect(content().string("2"));

        verify(labelsFileGenerationProducer).produceTask(ordersIds, PageSize.A4, 2L);
    }

    @Test
    @DisplayName("Существует процесс генерации файла для указанных параметров")
    void generateLabelsFileLabelGenerationTaskExists() throws Exception {
        Set<Long> ordersIds = Set.of(1L);

        mockLomSearchOrders(ordersIds);

        generateLabelsFile(ordersIds)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Найдены не все запрашиваемые заказы")
    void generateLabelsFileOrderNotFound() throws Exception {
        Set<Long> ordersIds = Set.of(1L, 2L);

        mockLomSearchOrders(ordersIds, Set.of(1L));

        generateLabelsFile(ordersIds)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with ids [2] for senders with ids [10]"));
    }

    @Test
    @DisplayName("Магазин не найден")
    void generateLabelsFileShopNotFound() throws Exception {
        generateLabelsFile(Set.of(1L), PageSize.A4, 2L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [2]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("generateLabelsFileInvalidParameters")
    @DisplayName("Ошибки валидации при запуске формирования файла с ярлыками заказов")
    void generateLabelsFileValidationError(
        @SuppressWarnings("unused") String caseName,
        Set<Long> ordersIds,
        PageSize pageSize,
        ValidationErrorData error
    ) throws Exception {
        generateLabelsFile(ordersIds, pageSize, 1L)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(error));
    }

    @Nonnull
    private static Stream<Arguments> generateLabelsFileInvalidParameters() {
        return Stream.of(
            Arguments.of(
                "Коллекция идентификаторов заказов null",
                null,
                PageSize.A6,
                fieldError("ordersIds", "must not be empty", "orderLabelRequest", "NotEmpty")
            ),
            Arguments.of(
                "Пустая коллекция идентификаторов заказов",
                Set.of(),
                PageSize.A6,
                fieldError("ordersIds", "must not be empty", "orderLabelRequest", "NotEmpty")
            ),
            Arguments.of(
                "Коллекция идентификаторов заказов содержит null",
                Sets.newHashSet(1L, null),
                PageSize.A6,
                fieldError("ordersIds[]", "must not be null", "orderLabelRequest", "NotNull")
            ),
            Arguments.of(
                "Не передан формат листа",
                Set.of(1L),
                null,
                fieldError("pageSize", "must not be null", "orderLabelRequest", "NotNull")
            )
        );
    }

    private void mockLomSearchOrders(Set<Long> ordersIds) {
        mockLomSearchOrders(ordersIds, ordersIds);
    }

    private void mockLomSearchOrders(Set<Long> ordersIds, Set<Long> foundOrdersIds) {
        List<OrderDto> foundOrders = foundOrdersIds.stream()
            .map(id -> {
                OrderDto orderDto = new OrderDto();
                return orderDto.setId(id);
            })
            .collect(Collectors.toList());

        when(lomClient.searchOrders(
            safeRefEq(
                OrderSearchFilter.builder()
                    .senderIds(Set.of(10L))
                    .orderIds(ordersIds)
                    .platformClientId(PlatformClientId.YANDEX_DELIVERY.getId())
                    .build()
            ),
            safeRefEq(new Pageable(0, ordersIds.size(), null))
        )).thenReturn(PageResult.of(foundOrders, foundOrders.size(), 1, ordersIds.size()));
    }

    @Nonnull
    private ResultActions generateLabelsFile(Set<Long> ordersIds) throws Exception {
        return generateLabelsFile(ordersIds, PageSize.A4, 1L);
    }

    @Nonnull
    private ResultActions generateLabelsFile(Set<Long> ordersIds, PageSize pageSize, long shopId) throws Exception {
        OrderLabelRequest request = OrderLabelRequest.builder()
            .ordersIds(ordersIds)
            .pageSize(pageSize)
            .build();

        return mockMvc.perform(
            request(HttpMethod.POST, "/back-office/documents/labels/generate", request)
                .param("shopId", String.valueOf(shopId))
                .param("userId", "1")
        );
    }
}
