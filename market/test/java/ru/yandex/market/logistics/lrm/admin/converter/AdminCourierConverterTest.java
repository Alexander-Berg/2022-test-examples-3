package ru.yandex.market.logistics.lrm.admin.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.front.library.dto.FormattedTextObject;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.model.entity.embedded.ReturnCourierFields;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация данных о курьере")
class AdminCourierConverterTest extends LrmTest {

    private final AdminCourierConverter courierConverter = new AdminCourierConverter(new AdminTextObjectConverter());

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Конвертация данных о курьере")
    void convertCourier(
        String displayName,
        @Nullable ReturnCourierFields courier,
        FormattedTextObject expectedResult
    ) {
        softly.assertThat(courierConverter.convertToTextObject(courier))
            .isEqualTo(expectedResult);
    }

    @Nonnull
    private static Stream<Arguments> convertCourier() {
        return Stream.of(
            Arguments.of(
                "В конвертер передали null",
                null,
                FormattedTextObject.of(null)
            ),
            Arguments.of(
                "Все поля не заполнены",
                courier(),
                FormattedTextObject.of("")
            ),
            Arguments.of(
                "Все поля заполнены",
                courier().setName("Name").setCarNumber("a123bc54").setUid("UUID"),
                FormattedTextObject.of(
                    """
                        Имя : Name
                        Государственный номер автомобиля : a123bc54
                        UUID : UUID\
                        """
                )
            ),
            Arguments.of(
                "Заполнено только одно поле",
                courier().setName("Only name"),
                FormattedTextObject.of("Имя : Only name")
            )
        );
    }

    @Nonnull
    private static ReturnCourierFields courier() {
        return new ReturnCourierFields();
    }
}
