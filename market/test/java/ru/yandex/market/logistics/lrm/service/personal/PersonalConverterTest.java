package ru.yandex.market.logistics.lrm.service.personal;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.service.meta.model.CourierClientReturnMeta;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.CommonTypeEnum;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.MultiTypeStoreResponseItem;

@DisplayName("Тест конвертера Personal")
@ParametersAreNonnullByDefault
class PersonalConverterTest extends LrmTest {

    private final PersonalConverter personalConverter = new PersonalConverter();

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Тест конвертера строки с ФИО в формат Personal")
    void toFullName(String ignored, String fullName, FullName resultFullName) {
        softly.assertThat(personalConverter.toFullName(fullName))
            .isEqualTo(resultFullName);
    }

    private static Stream<Arguments> toFullName() {
        return Stream.of(
            Arguments.of(
                "Строка из 4 слов",
                "Иванов Иван Иванович Младший",
                new FullName().surname("Иванов").forename("Иван").patronymic("Иванович Младший")
            ),
            Arguments.of(
                "Строка из 3 слов",
                "Иванов Иван Иванович",
                new FullName().surname("Иванов").forename("Иван").patronymic("Иванович")
            ),
            Arguments.of(
                "Строка из 2 слов",
                "Иванов Иван",
                new FullName().surname("Иванов").forename("Иван")
            ),
            Arguments.of(
                "Строка из 1 слова",
                "Иван",
                new FullName().forename("Иван")
            ),
            Arguments.of(
                "Строка из 0 слов",
                "",
                null
            ),
            Arguments.of(
                "Нет строки",
                null,
                null
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Тест конвертера данных клиента из Personal в формат меты")
    void toClient(
        String ignored,
        CourierClientReturnMeta.Client presentClient,
        Map<CommonType, MultiTypeStoreResponseItem> encodeMap,
        CourierClientReturnMeta.Client resultClient
    ) {
        softly.assertThat(personalConverter.encodeClient(presentClient, encodeMap))
            .isEqualTo(resultClient);
    }

    private static Stream<Arguments> toClient() {
        return Stream.of(
            Arguments.of(
                "Есть все данные",
                CourierClientReturnMeta.Client.builder()
                    .fullName("full name")
                    .phone("phone")
                    .email("email")
                    .build(),
                Map.of(
                    new CommonType().fullName(new FullName().surname("full").forename("name")),
                    new MultiTypeStoreResponseItem()
                        .id("personal-full-name-id")
                        .value(new CommonType().fullName(new FullName().surname("full").forename("name"))),
                    new CommonType().email("email"),
                    new MultiTypeStoreResponseItem()
                        .id("personal-email-id")
                        .value(new CommonType().email("email")),
                    new CommonType().phone("phone"),
                    new MultiTypeStoreResponseItem()
                        .id("personal-phone-id")
                        .value(new CommonType().phone("phone"))
                ),
                CourierClientReturnMeta.Client.builder()
                    .personalFullNameId("personal-full-name-id")
                    .personalEmailId("personal-email-id")
                    .personalPhoneId("personal-phone-id")
                    .build()
            ),
            Arguments.of(
                "Есть только ФИО",
                CourierClientReturnMeta.Client.builder()
                    .fullName("full name")
                    .build(),
                Map.of(
                    new CommonType().fullName(new FullName().surname("full").forename("name")),
                    new MultiTypeStoreResponseItem()
                        .id("personal-full-name-id")
                        .value(new CommonType().fullName(new FullName().surname("full").forename("name")))
                ),
                CourierClientReturnMeta.Client.builder()
                    .personalFullNameId("personal-full-name-id")
                    .build()
            ),
            Arguments.of(
                "Есть только эл. почта",
                CourierClientReturnMeta.Client.builder()
                    .email("email")
                    .build(),
                Map.of(
                    new CommonType().email("email"),
                    new MultiTypeStoreResponseItem()
                        .id("personal-email-id")
                        .value(new CommonType().email("email"))
                ),
                CourierClientReturnMeta.Client.builder()
                    .personalEmailId("personal-email-id")
                    .build()
            ),
            Arguments.of(
                "Есть только номер телефона",
                CourierClientReturnMeta.Client.builder()
                    .phone("phone")
                    .build(),
                Map.of(
                    new CommonType().phone("phone"),
                    new MultiTypeStoreResponseItem()
                        .id("personal-phone-id")
                        .value(new CommonType().phone("phone"))
                ),
                CourierClientReturnMeta.Client.builder()
                    .personalPhoneId("personal-phone-id")
                    .build()
            )
        );
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка при сохранении в Personal")
    void personalError(
        CommonTypeEnum type,
        String personalError,
        CourierClientReturnMeta.Client client,
        CommonType personalValue
    ) {
        softly.assertThatThrownBy(() ->
                personalConverter.encodeClient(
                    client,
                    Map.of(
                        personalValue,
                        new MultiTypeStoreResponseItem()
                            .value(personalValue)
                            .error(personalError)
                    )))
            .hasMessage("Error storing in Personal for %s: %s".formatted(type, personalError));
    }

    private static Stream<Arguments> personalError() {
        return Stream.of(
            Arguments.of(
                CommonTypeEnum.FULL_NAME,
                "Cannot store full name",
                CourierClientReturnMeta.Client.builder().fullName("full name").build(),
                new CommonType().fullName(new FullName().surname("full").forename("name"))
            ),
            Arguments.of(
                CommonTypeEnum.PHONE,
                "Cannot store phone",
                CourierClientReturnMeta.Client.builder().phone("phone").build(),
                new CommonType().phone("phone")
            ),
            Arguments.of(
                CommonTypeEnum.EMAIL,
                "Cannot store email",
                CourierClientReturnMeta.Client.builder().email("email").build(),
                new CommonType().email("email")
            )
        );
    }
}
