package ru.yandex.market.logistics.lrm.admin.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.model.entity.embedded.ReturnDestinationPointFields;
import ru.yandex.market.logistics.lrm.model.entity.embedded.ReturnPickupPointFields;
import ru.yandex.market.logistics.lrm.model.entity.enums.DestinationPointType;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация данных точек возврата")
class AdminReturnPointConverterTest extends LrmTest {

    private final AdminReturnPointConverter destinationPointConverter = new AdminReturnPointConverter(
        new AdminTextObjectConverter()
    );

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация данных точки назначения")
    void convertDestinationPoint(
        @SuppressWarnings("unused") String displayName,
        ReturnDestinationPointFields destinationPoint,
        FormattedTextObject expectedResult
    ) {
        softly.assertThat(destinationPointConverter.convertToTextObject(destinationPoint))
            .isEqualTo(expectedResult);
    }

    @Nonnull
    private static Stream<Arguments> convertDestinationPoint() {
        return Stream.of(
            Arguments.of(
                "В конвертер передали null",
                null,
                FormattedTextObject.of(null)
            ),
            Arguments.of(
                "Все поля не заполнены",
                destinationPoint(),
                FormattedTextObject.of("")
            ),
            Arguments.of(
                "Все поля заполнены",
                destinationPoint().setType(DestinationPointType.SHOP).setShopId(123L).setPartnerId(345L),
                FormattedTextObject.of(
                    """
                        Тип точки : SHOP
                        Партнёр : 345
                        Идентификатор магазина : 123\
                        """
                )
            ),
            Arguments.of(
                "Заполнено только одно поле",
                destinationPoint().setType(DestinationPointType.FULFILLMENT),
                FormattedTextObject.of("Тип точки : FULFILLMENT")
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Конвертация данных ПВЗ")
    void convertPickupPoint(
        @SuppressWarnings("unused") String displayName,
        ReturnPickupPointFields pickupPoint,
        FormattedTextObject expectedResult
    ) {
        softly.assertThat(destinationPointConverter.convertToTextObject(pickupPoint)).isEqualTo(expectedResult);
    }

    @Nonnull
    private static Stream<Arguments> convertPickupPoint() {
        return Stream.of(
            Arguments.of(
                "В конвертер передали null",
                null,
                FormattedTextObject.of(null)
            ),
            Arguments.of(
                "Все поля не заполнены",
                pickupPoint(),
                FormattedTextObject.of("")
            ),
            Arguments.of(
                "Все поля заполнены",
                pickupPoint().setPartnerId(100L).setExternalId("ext-id"),
                FormattedTextObject.of(
                    """
                        Партнёр : 100
                        Внешний идентификатор : ext-id\
                        """
                )
            ),
            Arguments.of(
                "Заполнено только одно поле",
                pickupPoint().setPartnerId(100L),
                FormattedTextObject.of("Партнёр : 100")
            )
        );
    }

    @Nonnull
    private static ReturnDestinationPointFields destinationPoint() {
        return new ReturnDestinationPointFields();
    }

    @Nonnull
    private static ReturnPickupPointFields pickupPoint() {
        return new ReturnPickupPointFields();
    }
}
