package ru.yandex.market.logistics.lom.controller.document.label;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.dto.OrderLabelRequestDto;
import ru.yandex.market.logistics.lom.model.enums.PageSize;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.market.logistics.lom.utils.TestUtils.validationErrorsJsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Генерация файлов с ярлыками заказов")
@DatabaseSetup("/controller/document/before/order_for_label.xml")
class LabelsGenerateFileTest extends AbstractContextualTest {
    private static final String LABELS_FILE_CONTENT = "labels file content";

    @Autowired
    private WwClient wwClient;

    @Test
    @DisplayName("Генерация файла с ярлыками")
    void generateLabelsFile() throws Exception {
        mockWwClientGenerateLabels(getLabelInfo());

        generateLabelsFile(Set.of(1L), PageSize.A6)
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(content().string(LABELS_FILE_CONTENT));
    }

    @Test
    @DatabaseSetup(
        value = "/controller/document/update/set_place_weight_to_null.xml",
        type = DatabaseOperation.UPDATE
    )
    @DisplayName("Успешная генерация для заказа с грузоместом без веса")
    void generateSuccessWherePlaceWithoutWeight() throws Exception {
        mockWwClientGenerateLabels(
            getLabelInfoBuilder()
                .place(
                    LabelInfo.PlaceInfo.builder()
                        .externalId("storage_unit_place_external_id")
                        .placeNumber(1)
                        .placesCount(1)
                        .build()
                )
                .build()
        );

        generateLabelsFile(Set.of(1L), PageSize.A6)
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"))
            .andExpect(content().string(LABELS_FILE_CONTENT));
    }

    @Test
    @DisplayName("Генерация файла с ярлыками, не найден заказ")
    void generateLabelsFileOrderNotFound() throws Exception {
        generateLabelsFile(Set.of(1L, 2L), PageSize.A6)
            .andExpect(status().isNotFound())
            .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(jsonPath("message").value("Failed to find [ORDER] with id [2]"));
    }

    @Test
    @DisplayName("Генерация файла с ярлыками, ошибка валидации при вызове WW-клиента")
    void generateLabelsFileWwClientError() throws Exception {
        mockWwClientGenerateLabelsError();

        generateLabelsFile(Set.of(1L), PageSize.A6)
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Error message"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Валидация запроса")
    @MethodSource("validateRequestArguments")
    void generateLabelsFileValidationError(
        String caseName,
        Set<Long> ordersIds,
        PageSize pageSize,
        String errorField,
        String errorMessage
    ) throws Exception {
        generateLabelsFile(ordersIds, pageSize)
            .andExpect(status().isBadRequest())
            .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"))
            .andExpect(validationErrorsJsonContent(errorField, errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> validateRequestArguments() {
        return Stream.of(
            Arguments.of(
                "Коллекция идентификаторов заказов null",
                null,
                PageSize.A6,
                "ordersIds",
                "must not be empty"
            ),
            Arguments.of(
                "Коллекция идентификаторов заказов пустая",
                Set.of(),
                PageSize.A6,
                "ordersIds",
                "must not be empty"
            ),
            Arguments.of(
                "Коллекция идентификаторов заказов содержит null",
                Sets.newHashSet(1L, null),
                PageSize.A6,
                "ordersIds[]",
                "must not be null"
            ),
            Arguments.of(
                "Не передан размер страницы",
                Set.of(1L),
                null,
                "pageSize",
                "must not be null"
            )
        );
    }

    private void mockWwClientGenerateLabels(LabelInfo labelInfo) {
        when(wwClient.generateLabels(
            List.of(labelInfo),
            DocumentFormat.PDF,
            ru.yandex.market.logistics.werewolf.model.enums.PageSize.A6
        )).thenReturn(LABELS_FILE_CONTENT.getBytes());
    }

    private void mockWwClientGenerateLabelsError() {
        when(wwClient.generateLabels(
            List.of(getLabelInfo()),
            DocumentFormat.PDF,
            ru.yandex.market.logistics.werewolf.model.enums.PageSize.A6
        )).thenThrow(new HttpTemplateException(HttpStatus.BAD_REQUEST.value(), "Error message"));
    }

    @Nonnull
    private LabelInfo getLabelInfo() {
        return getLabelInfoBuilder().build();
    }

    @Nonnull
    private LabelInfo.LabelInfoBuilder getLabelInfoBuilder() {
        return LabelInfo.builder()
            .platformClientId(3L)
            .barcode("2-LOinttest-1")
            .shipmentDate(LocalDate.of(2020, 1, 1))
            .deliveryService(
                LabelInfo.PartnerInfo.builder()
                    .legalName("ООО DPD")
                    .readableName("DPD")
                    .build()
            )
            .seller(
                LabelInfo.SellerInfo.builder()
                    .number("order_1_external_id")
                    .readableName("Магазин")
                    .legalName("ООО Магазин")
                    .build()
            )
            .place(
                LabelInfo.PlaceInfo.builder()
                    .externalId("storage_unit_place_external_id")
                    .placeNumber(1)
                    .placesCount(1)
                    .weight(BigDecimal.ONE)
                    .build()
            )
            .recipient(
                LabelInfo.RecipientInfo.builder()
                    .firstName("Иван")
                    .lastName("Иванов")
                    .phoneNumber("+79876543210")
                    .build()
            )
            .address(
                LabelInfo.AddressInfo.builder()
                    .country("Россия")
                    .locality("Москва")
                    .street("Ленина")
                    .house("5")
                    .zipCode("123321")
                    .build()
            );
    }

    @Nonnull
    private ResultActions generateLabelsFile(
        @Nullable Set<Long> ordersIds,
        @Nullable PageSize pageSize
    ) throws Exception {
        return mockMvc.perform(
            request(
                HttpMethod.PUT,
                "/orders/labels/generate",
                OrderLabelRequestDto.builder()
                    .ordersIds(ordersIds)
                    .pageSize(pageSize)
                    .build()
            )
                .accept("application/json; q=0.9", "application/pdf")
        );
    }
}
