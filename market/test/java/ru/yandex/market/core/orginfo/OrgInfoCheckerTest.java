package ru.yandex.market.core.orginfo;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.model.RegionConstants;
import ru.yandex.market.core.orginfo.model.OrganizationType;

/**
 * Тесты на валидацию ОГРН, УНП, регистрационных номеров
 *
 * @author fbokovikov
 */
public class OrgInfoCheckerTest extends FunctionalTest {

    private static final Long GLOBAL = 450L;

    @Autowired
    private OrganizationInfoChecker organizationInfoChecker;

    static Stream<Arguments> args() {
        return Stream.of(
                //корректное значение (с правильной контрольной суммой, 13 цифр)
                Arguments.of("5077746887312", RegionConstants.RUSSIA, OrganizationType.ZAO, true),
                Arguments.of("5077746887312", RegionConstants.RUSSIA, OrganizationType.AO, true),
                Arguments.of("5077746887312", RegionConstants.RUSSIA, OrganizationType.OOO, true),
                Arguments.of("5077746887312", RegionConstants.RUSSIA, OrganizationType.OTHER, true),
                //сломалась контрольная сумма
                Arguments.of("5077746887313", RegionConstants.RUSSIA, OrganizationType.OOO, false),
                //корректное значение (с правильной контрольной суммой, 15 цифр)
                Arguments.of("304500116000157", RegionConstants.RUSSIA, OrganizationType.IP, true),
                Arguments.of("304500116000157", RegionConstants.RUSSIA, OrganizationType.NONE, true),
                //сломалась контрольная сумма
                Arguments.of("304500116000158", RegionConstants.RUSSIA, OrganizationType.IP, false),
                //ИНН физика ок
                Arguments.of("437730231589", RegionConstants.RUSSIA, OrganizationType.PHYSIC, true),
                //ИНН физика со сломанной контрольной суммой
                Arguments.of("437730231580", RegionConstants.RUSSIA, OrganizationType.PHYSIC, false),
                //ИНН юр.лица не ок
                Arguments.of("2877008453", RegionConstants.RUSSIA, OrganizationType.PHYSIC, false),

                //для ИП OrganizationType 10 цифр
                Arguments.of("1234567890", RegionConstants.UKRAINE, OrganizationType.IP, true),
                //для юр.лиц OrganizationType 8 цифр
                Arguments.of("12345678", RegionConstants.UKRAINE, OrganizationType.OOO, true),
                Arguments.of("123456a1", RegionConstants.UKRAINE, OrganizationType.NONE, false),
                Arguments.of("12345678e0", RegionConstants.UKRAINE, OrganizationType.IP, false),

                //9 цифр, корректная контрольная сумма
                Arguments.of("300028308", RegionConstants.BELARUS, OrganizationType.OOO, true),
                Arguments.of("590641673", RegionConstants.BELARUS, OrganizationType.NONE, true),
                //сломалась контрольная сумма
                Arguments.of("101541948", RegionConstants.BELARUS, OrganizationType.OOO, false),
                Arguments.of("12345678Z", RegionConstants.BELARUS, OrganizationType.IP, false),
                Arguments.of("12345678Z", RegionConstants.BELARUS, OrganizationType.NONE, false),

                //для Казахстана всегда просто 12 цифр
                Arguments.of("123456789012", RegionConstants.KAZAKHSTAN, OrganizationType.ZAO, true),
                Arguments.of("123456789012", RegionConstants.KAZAKHSTAN, OrganizationType.NONE, true),

                //для Глобалов c 5 до 30 символов, включая пробел, тире, слэш, цифры и заглавные латинские буквы
                Arguments.of("134ABCXYZ1119", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("134ABC XYZ1119", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("134ABC ./XYZ-9", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("134AB", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("134A", GLOBAL, OrganizationType.OTHER, false),
                Arguments.of("XYZ-12345", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("123456789012345678901234567890", GLOBAL, OrganizationType.OTHER, true),
                Arguments.of("1234567890123456789012345678901", GLOBAL, OrganizationType.OTHER, false),
                Arguments.of("1234567a2345678901", GLOBAL, OrganizationType.OTHER, false),
                Arguments.of("12345~12345", GLOBAL, OrganizationType.OTHER, false),
                Arguments.of("12345_A12345", GLOBAL, OrganizationType.OTHER, false)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    void organizationInfoCheckerTest(
            String registrationNumber,
            long regionId,
            OrganizationType organizationType,
            boolean expectedIsValid
    ) {
        boolean isValid = organizationInfoChecker.validateRegNum(regionId, organizationType, registrationNumber);
        Assertions.assertEquals(expectedIsValid, isValid);
    }
}
