package ru.yandex.market.core.orginfo.cleaner;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.orginfo.model.OrganizationType;

import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.swapCase;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;
import static ru.yandex.market.core.orginfo.model.OrganizationType.AO;
import static ru.yandex.market.core.orginfo.model.OrganizationType.IP;
import static ru.yandex.market.core.orginfo.model.OrganizationType.OAO;
import static ru.yandex.market.core.orginfo.model.OrganizationType.OOO;
import static ru.yandex.market.core.orginfo.model.OrganizationType.OTHER;
import static ru.yandex.market.core.orginfo.model.OrganizationType.ZAO;

/**
 * Unit-тесты для {@link OrganizationNameCleaner}.
 *
 * @author Vladislav Bauer
 */
class OrganizationNameCleanerTest {

    private static Stream<Arguments> clearOrganizationNameData() {
        return Stream.of(
                // Эзотеризм
                of("\"", "\""),
                of("\"\"", null),
                of("ООО \"\"", null),

                // ООО
                of("ООО Быки и коровы", "Быки и коровы"),
                of("ООО \"Быки и коровы\"", "Быки и коровы"),
                of("ООО 'Быки и коровы'", "Быки и коровы"),
                of("ООО ’Быки и коровы’", "Быки и коровы"),
                of("ООО “Быки и коровы“", "Быки и коровы"),
                of("ООО “Быки и коровы”", "Быки и коровы"),
                of("ООО «Быки и коровы»", "Быки и коровы"),
                of("ООО. Быки и коровы", "Быки и коровы"),
                of("ООО.Быки и коровы", "Быки и коровы"),
                of("О.О.О. Быки и коровы", "Быки и коровы"),
                of("О.О.О.Быки и коровы", "Быки и коровы"),
                of("Общество с Ограниченной Ответственностью Быки и коровы", "Быки и коровы"),
                of("ООО! Быки", "ООО! Быки"),
                of("ООО ООО", "ООО"),
                of("ООО.", null),

                // ОАО
                of("ОАО Пипидастр", "Пипидастр"),
                of("ОАО \"Пипидастр\"", "Пипидастр"),
                of("ОАО. Пипидастр", "Пипидастр"),
                of("ОАО.Пипидастр", "Пипидастр"),
                of("О.А.О. Пипидастр", "Пипидастр"),
                of("О.А.О.Пипидастр", "Пипидастр"),
                of("Открытое Акционерное Общество Пипидастр", "Пипидастр"),
                of("ОАОПипидастры", "ОАОПипидастры"),
                of("ОАО ОАО", "ОАО"),
                of("ОАО.", null),

                // ЗАО
                of("ЗАО Вувузела", "Вувузела"),
                of("ЗАО \"Вувузела\"", "Вувузела"),
                of("ЗАО. Вувузела", "Вувузела"),
                of("ЗАО.Вувузела", "Вувузела"),
                of("З.А.О. Вувузела", "Вувузела"),
                of("З.А.О.Вувузела", "Вувузела"),
                of("Закрытое Акционерное Общество Вувузела", "Вувузела"),
                of("ЗАОЖ", "ЗАОЖ"),
                of("ЗАО ЗАО", "ЗАО"),
                of("ЗАО.", null),

                // АО
                of("АО Ололо", "Ололо"),
                of("АО \"Ололо\"", "Ололо"),
                of("АО. Ололо", "Ололо"),
                of("АО.Ололо", "Ололо"),
                of("А.О. Ололо", "Ололо"),
                of("А.О.Ололо", "Ололо"),
                of("Акционерное Общество Ололо", "Ололо"),
                of("АОАОлоло", "АОАОлоло"),
                of("АО АО", "АО"),
                of("АО.", null),

                // ИП
                of("ИП Майкл Майерс", "Майкл Майерс"),
                of("ИП \"Майкл Майерс\"", "Майкл Майерс"),
                of("ИП. Майкл Майерс", "Майкл Майерс"),
                of("ИП.Майкл Майерс", "Майкл Майерс"),
                of("И.П. Майкл Майерс", "Майкл Майерс"),
                of("И.П.Майкл Майерс", "Майкл Майерс"),
                of("Индивидуальный предприниматель Майкл Майерс", "Майкл Майерс"),
                of("ИпМэн", "ИпМэн"),
                of("ИП ИП", "ИП"),
                of("ИП.", null),

                // ЧП
                of("ЧП Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("ЧП \"Джейсон Вурхиз\"", "Джейсон Вурхиз"),
                of("ЧП. Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("ЧП.Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("Ч.П. Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("Ч.П.Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("Частный предприниматель Джейсон Вурхиз", "Джейсон Вурхиз"),
                of("ЧПлащ", "ЧПлащ"),
                of("ЧП ЧП", "ЧП"),
                of("ЧП.", null)
        );
    }

    @ParameterizedTest
    @MethodSource("clearOrganizationNameData")
    void clearOrganizationName(String origin, String expected) {
        for (OrganizationType type : OrganizationType.values()) {
            if (!OrganizationNameCleaner.UNKNOWN_TYPES.contains(type)) {
                checkCleaner(type, expected, origin);
            }
        }
    }

    private static Stream<Arguments> clearOrganizationNameEmptyData() {
        return Stream.of(
                // Пустые наименования
                of(null, null),
                of("", null),
                of(" ", null),
                of("  ", null)
        );
    }

    @ParameterizedTest
    @MethodSource("clearOrganizationNameEmptyData")
    void clearOrganizationNameEmpty(String origin, String expected) {
        for (OrganizationType type : OrganizationType.values()) {
            checkCleaner(type, expected, origin);
        }
    }

    private static Stream<Arguments> clearOrganizationNameUnknownData() {
        return Stream.of(
                of("Организация \"Планета обезьян\""),
                of("Детский садик Лапушок"),
                of("ООО \"ИП\""),
                of("ИП \"ООО\""),
                of("ОАО \"ООО\""),
                of("ЗАО \"Сардонический смех \""),
                of("АО \"Мандиблы\""),
                of("ЧП Остап Бендер"),
                of("\"\""),
                of("ООО \"\"")
        );
    }

    @ParameterizedTest
    @MethodSource("clearOrganizationNameUnknownData")
    void clearOrganizationNameUnknown(String value) {
        for (OrganizationType type : OrganizationNameCleaner.UNKNOWN_TYPES) {
            checkCleaner(type, value, value);
        }
    }

    private static void checkCleaner(OrganizationType type, String expected, String origin) {
        checkWithSpaces(type, origin, expected);
        checkWithSpaces(type, lowerCase(origin), lowerCase(expected));
        checkWithSpaces(type, upperCase(origin), upperCase(expected));
        checkWithSpaces(type, swapCase(origin), swapCase(expected));
        checkWithSpaces(type, capitalize(origin), capitalize(expected));
    }

    private static void checkWithSpaces(OrganizationType type, String raw, String expected) {
        assertThat(OrganizationNameCleaner.clearOrganizationName(raw, type)).isEqualTo(expected);
        if (raw != null) {
            assertThat(OrganizationNameCleaner.clearOrganizationName(SPACE + raw, type))
                    .isEqualTo(expected);
            assertThat(OrganizationNameCleaner.clearOrganizationName(raw + SPACE, type))
                    .isEqualTo(expected);
            assertThat(OrganizationNameCleaner.clearOrganizationName(SPACE + raw + SPACE, type))
                    .isEqualTo(expected);
        }
    }

    private static Stream<Arguments> normalizeOrganizationNameData() {
        return Stream.of(
                // ООО
                of("ООО Быки и коровы", OOO, "Быки и коровы"),
                of("Быки и коровы", OOO, "Быки и коровы"),
                of("ООО Быки и коровы", OOO, "Быки и коровы"),
                of("ООО \"Быки и коровы\"", OOO, "Быки и коровы"),
                of("ООО 'Быки и коровы'", OOO, "Быки и коровы"),
                of("ООО ’Быки и коровы’", OOO, "Быки и коровы"),
                of("ООО “Быки и коровы“", OOO, "Быки и коровы"),
                of("ООО “Быки и коровы”", OOO, "Быки и коровы"),
                of("ООО «Быки и коровы»", OOO, "Быки и коровы"),
                of("ОАО Быки и коровы", OOO, "Быки и коровы"),
                of(null, OOO, null),

                // ОАО
                of("ОАО Быки и коровы", OAO, "Быки и коровы"),
                of("Быки и коровы", OAO, "Быки и коровы"),
                of("ОАО Быки и коровы", OAO, "Быки и коровы"),
                of("ОАО \"Быки и коровы\"", OAO, "Быки и коровы"),
                of("ООО Быки и коровы", OAO, "Быки и коровы"),
                of(null, OAO, null),

                // ЗАО
                of("ЗАО Быки и коровы", ZAO, "Быки и коровы"),
                of("Быки и коровы", ZAO, "Быки и коровы"),
                of("ЗАО Быки и коровы", ZAO, "Быки и коровы"),
                of("ЗАО \"Быки и коровы\"", ZAO, "Быки и коровы"),
                of("ОАО Быки и коровы", ZAO, "Быки и коровы"),
                of(null, ZAO, null),

                // АО
                of("АО Быки и коровы", AO, "Быки и коровы"),
                of("Быки и коровы", AO, "Быки и коровы"),
                of("АО Быки и коровы", AO, "Быки и коровы"),
                of("АО \"Быки и коровы\"", AO, "Быки и коровы"),
                of("АО Быки и коровы", AO, "Быки и коровы"),
                of(null, AO, null),

                // ИП
                of("ИП Быковский", IP, "Индивидуальный предприниматель Быковский"),
                of("Быковский", IP, "Индивидуальный предприниматель Быковский"),
                of("ИП Быковский", IP, "Индивидуальный предприниматель Быковский"),
                of("Индивидуальный предприниматель Быковский", IP, "Индивидуальный " +
                        "предприниматель Быковский"),
                of(null, IP, null),

                // Иное
                of("ООО Быки и коровы", OTHER, "ООО Быки и коровы"),
                of("ОАО Быки и коровы", OTHER, "ОАО Быки и коровы"),
                of("ЗАО Быки и коровы", OTHER, "ЗАО Быки и коровы"),
                of("ИП Быки и коровы", OTHER, "ИП Быки и коровы"),
                of("Быки и коровы", OTHER, "Быки и коровы"),
                of(null, OTHER, null),

                // Всякое
                of("OOO English Letters", null, "OOO English Letters"),
                of("ООО Быки и коровы", null, "ООО Быки и коровы"),
                of("Быки и коровы", null, "Быки и коровы"),
                of(null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeOrganizationNameData")
    void normalizeOrganizationName(
            String organizationName,
            OrganizationType organizationType,
            String expected
    ) {
        assertThat(OrganizationNameCleaner.normalizeOrganizationName(
                organizationName,
                organizationType
        )).isEqualTo(expected);
    }

    private static Stream<Arguments> normalizeOrganizationNameWithOrgTypeData() {
        return Stream.of(
                // ООО
                of("ООО Быки и коровы", OOO, null, "ООО Быки и коровы"),
                of("Быки и коровы", OOO, null, "ООО Быки и коровы"),
                of("ООО Быки и коровы", OOO, "OOOO", "ООО Быки и коровы"),
                of("ООО \"Быки и коровы\"", OOO, null, "ООО Быки и коровы"),
                of("ООО 'Быки и коровы'", OOO, null, "ООО Быки и коровы"),
                of("ООО ’Быки и коровы’", OOO, null, "ООО Быки и коровы"),
                of("ООО “Быки и коровы“", OOO, null, "ООО Быки и коровы"),
                of("ООО “Быки и коровы”", OOO, null, "ООО Быки и коровы"),
                of("ООО «Быки и коровы»", OOO, null, "ООО Быки и коровы"),
                of("ОАО Быки и коровы", OOO, null, "ООО Быки и коровы"),
                of(null, OOO, null, null),

                // ОАО
                of("ОАО Быки и коровы", OAO, null, "ОАО Быки и коровы"),
                of("Быки и коровы", OAO, null, "ОАО Быки и коровы"),
                of("ОАО Быки и коровы", OAO, "ОАООА", "ОАО Быки и коровы"),
                of("ОАО \"Быки и коровы\"", OAO, null, "ОАО Быки и коровы"),
                of("ООО Быки и коровы", OAO, null, "ОАО Быки и коровы"),
                of(null, OAO, null, null),

                // ЗАО
                of("ЗАО Быки и коровы", ZAO, null, "ЗАО Быки и коровы"),
                of("Быки и коровы", ZAO, null, "ЗАО Быки и коровы"),
                of("ЗАО Быки и коровы", ZAO, "ЗАОАО", "ЗАО Быки и коровы"),
                of("ЗАО \"Быки и коровы\"", ZAO, null, "ЗАО Быки и коровы"),
                of("ОАО Быки и коровы", ZAO, null, "ЗАО Быки и коровы"),
                of(null, ZAO, null, null),

                // АО
                of("АО Быки и коровы", AO, null, "АО Быки и коровы"),
                of("Быки и коровы", AO, null, "АО Быки и коровы"),
                of("АО Быки и коровы", AO, "ЗАОАО", "АО Быки и коровы"),
                of("АО \"Быки и коровы\"", AO, null, "АО Быки и коровы"),
                of("АО Быки и коровы", AO, null, "АО Быки и коровы"),
                of(null, AO, null, null),

                // ИП
                of("ИП Быковский", IP, null, "Индивидуальный предприниматель Быковский"),
                of("Быковский", IP, null, "Индивидуальный предприниматель Быковский"),
                of("ИП Быковский", IP, "АОAO", "Индивидуальный предприниматель Быковский"),
                of("Индивидуальный предприниматель Быковский", IP, null, "Индивидуальный " +
                        "предприниматель Быковский"),
                of(null, IP, null, null),

                // Иное
                of("ООО Быки и коровы", OTHER, null, "ООО Быки и коровы"),
                of("ОАО Быки и коровы", OTHER, null, "ОАО Быки и коровы"),
                of("ЗАО Быки и коровы", OTHER, null, "ЗАО Быки и коровы"),
                of("ИП Быки и коровы", OTHER, null, "ИП Быки и коровы"),
                of("Быки и коровы", OTHER, "МоеОО", "МоеОО Быки и коровы"),
                of(null, OTHER, null, null),

                // Всякое
                of("OOO English Letters", null, null, "OOO English Letters"),
                of("ООО Быки и коровы", null, null, "ООО Быки и коровы"),
                of("Быки и коровы", null, null, "Быки и коровы"),
                of(null, null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("normalizeOrganizationNameWithOrgTypeData")
    void normalizeOrganizationNameWithOrgType(
            String organizationName,
            OrganizationType organizationType,
            String manualOrganizationType,
            String expected
    ) {
        assertThat(OrganizationNameCleaner.normalizeOrganizationNameWithOrgType(
                organizationName,
                organizationType,
                manualOrganizationType
        )).isEqualTo(expected);
    }
}
