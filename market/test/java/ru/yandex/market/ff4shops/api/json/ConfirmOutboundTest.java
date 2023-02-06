package ru.yandex.market.ff4shops.api.json;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.api.model.ConfirmOutboundDto;
import ru.yandex.market.ff4shops.api.model.OutboundBoxDto;
import ru.yandex.market.ff4shops.api.model.OutboundItemDto;
import ru.yandex.market.ff4shops.util.FF4ShopsUrlBuilder;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

@DisplayName("Подтверждение отправки")
class ConfirmOutboundTest extends AbstractJsonControllerFunctionalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DbUnitDataSet(
            before = "confirmOutboundNotConfirmed.before.csv",
            after = "confirmOutboundNotConfirmed.after.csv"
    )
    @DisplayName("Отправка существует и ещё не подтверждена")
    void testConfirmOutboundIfNotConfirmed() throws JsonProcessingException {
        ResponseEntity<String> response = confirmOutbound(createConfirmOutboundDto());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DbUnitDataSet(
            before = "confirmOutboundAlreadyConfirmed.before.csv",
            after = "confirmOutboundAlreadyConfirmed.before.csv"
    )
    @DisplayName("Отправка существует, но уже подтверждена")
    void testConfirmOutboundAlreadyConfirmed() {

        HttpClientErrorException.Conflict exception = catchThrowableOfType(
                () -> confirmOutbound(createConfirmOutboundDto()),
                HttpClientErrorException.Conflict.class
        );

        assertThat(exception).hasMessage("409 Conflict");
        assertThat(exception.getResponseBodyAsString())
                .contains("Outbound with yandexId=1 is already confirmed");
    }

    @Test
    @DbUnitDataSet
    @DisplayName("Отправка не существует")
    void testConfirmOutboundNotExist() {

        HttpClientErrorException.NotFound exception = catchThrowableOfType(
                () -> confirmOutbound(createConfirmOutboundDto()),
                HttpClientErrorException.NotFound.class
        );

        assertThat(exception).hasMessage("404 Not Found");
        assertThat(exception.getResponseBodyAsString())
                .contains("Outbound with yandexId=1 is not found");
    }

    @DisplayName("Тело запроса невалидно")
    @ParameterizedTest(name = "[{index}] {0} {2}")
    @MethodSource("validateConfirmDtoArguments")
    void validateRequestDto(
            String path,
            Consumer<ConfirmOutboundDto> dtoModifier,
            String errorMessage
    ) {
        ConfirmOutboundDto dto = createConfirmOutboundDto();
        dtoModifier.accept(dto);

        HttpClientErrorException.BadRequest exception = catchThrowableOfType(
                () -> confirmOutbound(dto),
                HttpClientErrorException.BadRequest.class
        );

        assertThat(exception).hasMessage("400 Bad Request");
        assertThat(exception.getResponseBodyAsString())
                .contains("Following validation errors occurred:")
                .contains("Field: '" + path + "', message: '" + errorMessage + "'");
    }

    private static Stream<Arguments> validateConfirmDtoArguments() {
        return Stream.<Triple<String, Consumer<ConfirmOutboundDto>, String>>of(
                Triple.of("outboundYandexId", dto -> dto.setOutboundYandexId(""), "must not be blank"),
                Triple.of("externalId", dto -> dto.setExternalId(""), "must not be blank"),
                Triple.of("orderIds", dto -> dto.setOrderIds(null), "must not be empty"),
                Triple.of("orderIds", dto -> dto.setOrderIds(List.of()), "must not be empty"),
                Triple.of(
                        "orderIds[0]",
                        dto -> dto.setOrderIds(Lists.newArrayList(null, 1L)),
                        "must not be null"
                ),

                Triple.of(
                        "items[0].width",
                        dto -> dto.setItems(List.of(createOutboundItemDto(-1, 10, 10))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "items[0].height",
                        dto -> dto.setItems(List.of(createOutboundItemDto(10, -1, 10))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "items[0].length",
                        dto -> dto.setItems(List.of(createOutboundItemDto(10, 10, -1))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "items[0].weightGross",
                        dto -> {
                            OutboundItemDto itemDto = createOutboundItemDto();
                            itemDto.setWeightGross(new BigDecimal(-1));
                            dto.setItems(List.of(itemDto));
                        },
                        "must be greater than or equal to 0"),
                Triple.of(
                        "items[0].price",
                        dto -> {
                            OutboundItemDto itemDto = createOutboundItemDto();
                            itemDto.setPrice(new BigDecimal(-1));
                            dto.setItems(List.of(itemDto));
                        },
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "items[0].count",
                        dto -> {
                            OutboundItemDto itemDto = createOutboundItemDto();
                            itemDto.setCount(-1);
                            dto.setItems(List.of(itemDto));
                        },
                        "must be greater than or equal to 0"
                ),

                Triple.of(
                        "boxes[0].width",
                        dto -> dto.setBoxes(List.of(createOutboundBoxDto(-1, 10, 10))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "boxes[0].height",
                        dto -> dto.setBoxes(List.of(createOutboundBoxDto(10, -1, 10))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "boxes[0].length",
                        dto -> dto.setBoxes(List.of(createOutboundBoxDto(10, 10, -1))),
                        "must be greater than or equal to 0"
                ),
                Triple.of(
                        "boxes[0].weightGross",
                        dto -> {
                            OutboundBoxDto boxDto = createOutboundBoxDto();
                            boxDto.setWeightGross(new BigDecimal(-1));
                            dto.setBoxes(List.of(boxDto));
                        },
                        "must be greater than or equal to 0"
                )
        )
                .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    private static ConfirmOutboundDto createConfirmOutboundDto() {
        ConfirmOutboundDto dto = new ConfirmOutboundDto();
        dto.setExternalId("externalId");
        dto.setOutboundYandexId("1");
        dto.setBoxes(List.of(createOutboundBoxDto()));
        dto.setItems(List.of(createOutboundItemDto()));
        dto.setOrderIds(List.of(1L, 2L, 3L));
        return dto;
    }

    private static OutboundBoxDto createOutboundBoxDto() {
        OutboundBoxDto dto = createOutboundBoxDto(10, 10, 10);
        dto.setWeightGross(BigDecimal.ONE);
        return dto;
    }

    private static OutboundItemDto createOutboundItemDto() {
        OutboundItemDto dto = createOutboundItemDto(30, 30, 30);
        dto.setWeightGross(BigDecimal.ONE);
        dto.setPrice(BigDecimal.TEN);
        dto.setCount(100);
        dto.setName("name");
        return dto;
    }

    private static OutboundItemDto createOutboundItemDto(int width, int height, int length) {
        OutboundItemDto dto = new OutboundItemDto();
        dto.setWidth(width);
        dto.setHeight(height);
        dto.setLength(length);
        return dto;
    }

    private static OutboundBoxDto createOutboundBoxDto(int width, int height, int length) {
        OutboundBoxDto dto = new OutboundBoxDto();
        dto.setWidth(width);
        dto.setHeight(height);
        dto.setLength(length);
        return dto;
    }

    private ResponseEntity<String> confirmOutbound(ConfirmOutboundDto confirmOutboundDto) throws JsonProcessingException {
        String referenceUrl = FF4ShopsUrlBuilder.confirmOutbound(randomServerPort);

        return FunctionalTestHelper.postForEntity(
                referenceUrl,
                MAPPER.writeValueAsString(confirmOutboundDto),
                FunctionalTestHelper.jsonHeaders()
        );
    }
}
