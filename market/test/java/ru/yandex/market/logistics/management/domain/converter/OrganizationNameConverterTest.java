package ru.yandex.market.logistics.management.domain.converter;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.converter.lgw.OrganizationNameConverter;
import ru.yandex.market.logistics.management.domain.entity.LegalInfo;

class OrganizationNameConverterTest extends AbstractTest {
    private static final OrganizationNameConverter ORGANIZATION_NAME_CONVERTER = new OrganizationNameConverter();

    private static Stream<Arguments> shortNameArguments() {
        return Stream.of(
            Arguments.of(
                "ип Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "ИП Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "Индивидуальный предприниматель Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "ИНДИВИДУАЛЬНЫЙ ПРЕДПРИНИМАТЕЛЬ Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                null,
                "ИП",
                null
            ),
            Arguments.of(
                "ооо Тест",
                "ООО",
                "Тест"
            ),
            Arguments.of(
                "Общество с ограниченной ответственностью Тест",
                "ООО",
                "Тест"
            ),
            Arguments.of(
                "ООО Тест",
                "ООО",
                "Тест"
            ),
            Arguments.of(
                "ООО \"Тест\"",
                "ООО",
                "Тест"
            ),
            Arguments.of(
                "ООО \"Тест \"Тест\"",
                "ООО",
                "Тест \"Тест\""
            ),
            Arguments.of(
                null,
                "ООО",
                null
            ),
            Arguments.of(
                "ООО Тест",
                null,
                "Тест"
            ),
            Arguments.of(
                null,
                null,
                null
            )
        );
    }

    private static Stream<Arguments> longNameArguments() {
        return Stream.of(
            Arguments.of(
                "ип Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "ИП Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "Индивидуальный предприниматель Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                "ИНДИВИДУАЛЬНЫЙ ПРЕДПРИНИМАТЕЛЬ Тест",
                "ИП",
                "Индивидуальный предприниматель Тест"
            ),
            Arguments.of(
                null,
                "ИП",
                null
            ),
            Arguments.of(
                "ооо Тест",
                "ООО",
                "ООО Тест"
            ),
            Arguments.of(
                "Общество с ограниченной ответственностью Тест",
                "ООО",
                "ООО Тест"
            ),
            Arguments.of(
                "ООО Тест",
                "ООО",
                "ООО Тест"
            ),
            Arguments.of(
                "ООО \"Тест\"",
                "ООО",
                "ООО Тест"
            ),
            Arguments.of(
                "ООО \"Тест \"Тест\"",
                "ООО",
                "ООО Тест \"Тест\""
            ),
            Arguments.of(
                null,
                "ООО",
                null
            ),
            Arguments.of(
                "ООО Тест",
                null,
                "ООО Тест"
            ),
            Arguments.of(
                null,
                null,
                null
            )
        );
    }

    @ParameterizedTest
    @MethodSource("shortNameArguments")
    void getShortName(String incorporation, String legalForm, String expectedShortName) {
        softly.assertThat(ORGANIZATION_NAME_CONVERTER.getShortName(
            new LegalInfo()
                .setIncorporation(incorporation)
                .setLegalForm(legalForm)
        ))
            .isEqualTo(expectedShortName);
    }

    @ParameterizedTest
    @MethodSource("longNameArguments")
    void getLongName(String incorporation, String legalForm, String expectedShortName) {
        softly.assertThat(ORGANIZATION_NAME_CONVERTER.getLongName(
            new LegalInfo()
                .setIncorporation(incorporation)
                .setLegalForm(legalForm)
        ))
            .isEqualTo(expectedShortName);
    }
}
