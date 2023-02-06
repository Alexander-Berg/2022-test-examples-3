package ru.yandex.market.tpl.core.service.sqs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.les.dto.TplClientDto;
import ru.yandex.market.logistics.les.dto.TplDimensionsDto;
import ru.yandex.market.logistics.les.dto.TplLocationDto;
import ru.yandex.market.logistics.les.dto.TplRequestIntervalDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressItemDto;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressReasonType;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCreateRequestEvent;
import ru.yandex.market.tpl.common.util.exception.TplIllegalArgumentException;
import ru.yandex.market.tpl.core.service.sqs.processor.CreateClientReturnValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CreateClientReturnValidatorTest {

    @Test
    void testAllFieldsAreValid() {
        var event = TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                        .builder()
                        .interval(
                                getInterval(LocalDate.now(), LocalTime.now(), LocalDate.now().plusDays(1),
                                        LocalTime.now())
                        )
                        .build()
        );

        assertDoesNotThrow(() -> CreateClientReturnValidator.validate(event));
    }

    @ParameterizedTest(name = "invalid field " + "{1}")
    @MethodSource("invalidUpperLevelFields")
    void testUpperFieldsAreNull(TplReturnAtClientAddressCreateRequestEvent event, String invalidField) {
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> CreateClientReturnValidator.validate(event));

        assertThat(exception.getMessage()).contains(invalidField);
    }

    @ParameterizedTest(name = "invalid interval " + "{1}")
    @MethodSource("invalidIntervals")
    void testInvalidInterval(TplReturnAtClientAddressCreateRequestEvent event, String invalidField) {
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> CreateClientReturnValidator.validate(event));

        assertThat(exception.getMessage()).contains(invalidField);
    }

    @ParameterizedTest(name = "invalid item")
    @MethodSource("invalidItems")
    void testInvalidItem(TplReturnAtClientAddressCreateRequestEvent event, String invalidField) {
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> CreateClientReturnValidator.validate(event));

        assertThat(exception.getMessage()).contains(invalidField);
    }

    @ParameterizedTest(name = "invalid location " + "{1}")
    @MethodSource("invalidLocations")
    void testInvalidLocation(TplReturnAtClientAddressCreateRequestEvent event, String invalidField) {
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> CreateClientReturnValidator.validate(event));

        assertThat(exception.getMessage()).contains(invalidField);
    }

    @Test
    void testInvalidClient() {
        var event = TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                        .builder()
                        .interval(
                                getInterval(LocalDate.now(), LocalTime.now(),
                                        LocalDate.now().plusDays(1), LocalTime.now())
                        )
                        .client(new TplClientDto(null, "email", "phone"))
                        .build()
        );
        var exception =
                assertThrows(TplIllegalArgumentException.class, () -> CreateClientReturnValidator.validate(event));

        assertThat(exception.getMessage()).contains("clientDto.fullName");
    }


    private static Stream<Arguments> invalidUpperLevelFields() {
        return Stream.of(
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .returnId(null)
                                        .build()
                        ),
                        "event.returnId"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .deliveryServiceId(null)
                                        .build()
                        ),
                        "event.deliveryServiceId"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(null)
                                        .build()
                        ),
                        "event.items"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .location(null)
                                        .build()
                        ),
                        "event.location"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .client(null)
                                        .build()
                        ),
                        "event.client"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .build()
                        ),
                        "event.interval"
                )
        );
    }

    private static Stream<Arguments> invalidIntervals() {
        return Stream.of(
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(null, LocalTime.now(), LocalDate.now().plusDays(1),
                                                        LocalTime.now())
                                        )
                                        .build()
                        ), "intervalDto.localDateFrom"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), null, LocalDate.now().plusDays(1),
                                                        LocalTime.now())
                                        )
                                        .build()
                        ), "intervalDto.localTimeFrom"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), null)
                                        )
                                        .build()
                        ), "intervalDto.localTimeTo"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().minusDays(1), LocalTime.now())
                                        )
                                        .build()
                        ), "intervalTo cannot be before intervalFrom"
                )
        );
    }

    private static Stream<Arguments> invalidItems() {
        return Stream.of(
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of())
                                        .build()
                        ), "items cannot be empty"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        null,
                                                        "CategoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, 1L, 1L, 1L
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "itemDto.name"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, 1L, 1L, 1L
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "itemDto.name"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        null,
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "dimensionsDto"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                null, 1L, 1L, 1L
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "dimensionsDto.weight"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, null, 1L, 1L
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "dimensionsDto.length"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, 1L, null, 1L
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "dimensionsDto.width"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, 1L, 1L, null
                                                        ),
                                                        TplReturnAtClientAddressReasonType.BAD_QUALITY,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "dimensionsDto.height"
                ),
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1), LocalTime.now())
                                        )
                                        .items(List.of(
                                                new TplReturnAtClientAddressItemDto(
                                                        345L,
                                                        "itemSku",
                                                        "itemName",
                                                        "categoryName",
                                                        "description",
                                                        "photoUrl",
                                                        "detailsUrl",
                                                        new TplDimensionsDto(
                                                                1L, 1L, 1L, 1L
                                                        ),
                                                        null,
                                                        "Bad quality",
                                                        null,
                                                        null
                                                )
                                        ))
                                        .build()
                        ), "itemDto.returnReason"
                )
        );
    }

    private static Stream<Arguments> invalidLocations() {
        return Stream.of(
                Arguments.of(
                        TplReturnAtClientAddressCreateRequestEventGenerateService.generateEvent(
                                TplReturnAtClientAddressCreateRequestEventGenerateService.TplReturnAtClientAddressCreateRequestEventGenerateParam
                                        .builder()
                                        .interval(
                                                getInterval(LocalDate.now(), LocalTime.now(),
                                                        LocalDate.now().plusDays(1),
                                                        LocalTime.now())
                                        )
                                        .location(new TplLocationDto(
                                                "", "Street", "house",
                                                "1", "123", "5", "1236", BigDecimal.ONE, BigDecimal.TEN,
                                                null
                                        ))
                                        .build()
                        ), "locationDto.city"
                )
        );
    }

    private static TplRequestIntervalDto getInterval(LocalDate now, LocalTime now1, LocalDate arriveLocalDateTo,
                                                     LocalTime now2) {
        return new TplRequestIntervalDto(
                now,
                now1,
                arriveLocalDateTo,
                now2
        );
    }

}
