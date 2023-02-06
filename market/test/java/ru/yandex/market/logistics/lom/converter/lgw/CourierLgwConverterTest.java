package ru.yandex.market.logistics.lom.converter.lgw;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNullableByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.embedded.Fio;

@DisplayName("Конвертация информации о курьере между моделями LOM/LGW")
@ParametersAreNullableByDefault
class CourierLgwConverterTest extends AbstractTest {
    private static final String COLOR = "белый";
    private static final String MODEL = "ВАЗ 21013";
    private static final String CUSTOM_DESCRIPTION = "Синяя Skoda Kodiaq семиместная с прицепом";

    private final CourierLgwConverter converter = new CourierLgwConverter(new PhoneLgwConverter());

    @DisplayName("Конвертация информации о транспортном средстве из моделей LGW в модели LOM")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testConvertCarFromApi(
        @Nonnull String ignored,
        @Nonnull Courier courier,
        @Nonnull WaybillSegment.Courier expected,
        boolean isExpress
    ) {
        softly.assertThat(converter.convertCourierFromApi(courier, isExpress)).isEqualTo(expected);
    }

    @DisplayName("Конвертация информации о транспортном средстве из моделей LOM в модели LGW")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void testConvertCarToApi(
        @Nonnull String ignored,
        @Nonnull WaybillSegment.Courier courier,
        @Nonnull Courier expected
    ) {
        softly.assertThat(converter.convertCourierToApi(courier)).isEqualTo(expected);
    }

    static Stream<Arguments> testConvertCarFromApi() {
        return Stream.of(
            Arguments.of(
                "Экспресс. Пустой description, есть model и color",
                getCourier(null, COLOR, MODEL),
                getWaybillSegmentCourier(MODEL + " " + COLOR, COLOR, MODEL),
                true
            ),
            Arguments.of(
                "Экспресс. Пустой description, пустые model и color",
                getCourier(null, null, ""),
                getWaybillSegmentCourier(null, null, ""),
                true
            ),
            Arguments.of(
                "Экспресс. Пустой description, пустой model, непустой color",
                getCourier(null, COLOR, null),
                getWaybillSegmentCourier(COLOR, COLOR, null),
                true
            ),
            Arguments.of(
                "Экспресс. Непустой description, непустые model и color",
                getCourier(CUSTOM_DESCRIPTION, COLOR, MODEL),
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, COLOR, MODEL),
                true
            ),
            Arguments.of(
                "Экспресс. Непустой description, пустые model и color",
                getCourier(CUSTOM_DESCRIPTION, null, ""),
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, null, ""),
                true
            ),
            Arguments.of(
                "Не экспресс. Пустой description, есть model и color",
                getCourier(null, COLOR, MODEL),
                getWaybillSegmentCourier(null, COLOR, MODEL),
                false
            ),
            Arguments.of(
                "Не экспресс. Пустой description, пустые model и color",
                getCourier(null, null, ""),
                getWaybillSegmentCourier(null, null, ""),
                false
            ),
            Arguments.of(
                "Не экспресс. Пустой description, пустой model, непустой color",
                getCourier(null, COLOR, null),
                getWaybillSegmentCourier(null, COLOR, null),
                false
            ),
            Arguments.of(
                "Не экспресс. Непустой description, непустые model и color",
                getCourier(CUSTOM_DESCRIPTION, COLOR, MODEL),
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, COLOR, MODEL),
                false
            ),
            Arguments.of(
                "Не экспресс. Непустой description, пустые model и color",
                getCourier(CUSTOM_DESCRIPTION, null, ""),
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, null, ""),
                false
            )
        );
    }

    static Stream<Arguments> testConvertCarToApi() {
        return Stream.of(
            Arguments.of(
                "Пустой description, непустые model и color",
                getWaybillSegmentCourier(MODEL + " " + COLOR, COLOR, MODEL),
                getCourier(MODEL + " " + COLOR, COLOR, MODEL)
            ),
            Arguments.of(
                "Пустой description, пустые model и color",
                getWaybillSegmentCourier(null, null, ""),
                getCourier(null, null, "")
            ),
            Arguments.of(
                "Пустой description, пустой model, непустой color",
                getWaybillSegmentCourier(COLOR, COLOR, null),
                getCourier(COLOR, COLOR, null)
            ),
            Arguments.of(
                "Непустой description, непустые model и color",
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, COLOR, MODEL),
                getCourier(CUSTOM_DESCRIPTION, COLOR, MODEL)
            ),
            Arguments.of(
                "Непустой description, пустые model и color",
                getWaybillSegmentCourier(CUSTOM_DESCRIPTION, null, ""),
                getCourier(CUSTOM_DESCRIPTION, null, "")
            ),
            Arguments.of("Нет курьера", null, Courier.builder().build())
        );
    }

    @Nonnull
    private static WaybillSegment.Courier getWaybillSegmentCourier(String description, String color, String model) {
        return new WaybillSegment.Courier()
            .setUrl("http://url.lru")
            .setPerson(
                new Fio()
                    .setFirstName("Ефим")
                    .setLastName("Ефимов")
                    .setMiddleName("Ефимович")
            )
            .setPhone(new ru.yandex.market.logistics.lom.entity.embedded.Phone().setPhoneNumber("8-800-555-35-35"))
            .setVehicle(
                new WaybillSegment.Car()
                    .setNumber("a3695ab")
                    .setColor(color)
                    .setModel(model)
                    .setDescription(description)
            );
    }

    @Nonnull
    private static Courier getCourier(String description, String color, String model) {
        return new Courier(
            null,
            List.of(
                Person.builder("Ефим").setSurname("Ефимов").setPatronymic("Ефимович").build()),
            Phone.builder("8-800-555-35-35").build(),
            Car.builder("a3695ab").setDescription(description).setColor(color).setModel(model).build(),
            null,
            null,
            "http://url.lru"
        );
    }
}
