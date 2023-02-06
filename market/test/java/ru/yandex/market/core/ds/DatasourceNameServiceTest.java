package ru.yandex.market.core.ds;

import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static ru.yandex.market.core.ds.PartnerNameValidator.SUPPRESS_NAME_CHARACTERS;

/**
 * Тест на валидацию datasourceInternalName
 *
 * @author fbokovikov
 */
public class DatasourceNameServiceTest {

    private static Stream<Arguments> validateInternalNameParameterizedTestData() {
        return Stream.of(
                Arguments.of("http://test.org", false),
                Arguments.of("test.org", true),
                Arguments.of("http://www.org.ru", false),
                Arguments.of("www.org.ru", true),
                Arguments.of("https://test.org", false),
                Arguments.of("https://www.test.org", false),
                Arguments.of(StringUtils.repeat("a", 50) + ".ru", false),
                Arguments.of("ООО Ромашка", true),
                Arguments.of("www.yandex.boutique", true),
                Arguments.of("yandex.boutique", true),
                Arguments.of("abc", true),
                Arguments.of("abc.2 абв-1_1 \"test\"", true),
                Arguments.of("yandex.http://boutique", false),
                Arguments.of("yandex.https://boutique", false),
                Arguments.of("yandex.boutique~1", false),
                Arguments.of("yandex.boutique^1", false),
                Arguments.of("yandex.boutique$1", false),
                Arguments.of("yandex.boutique%1", false),
                Arguments.of("яндекс.москва", true),
                Arguments.of("яндекс.москва!", true),
                Arguments.of("яндекс?москва!", true),
                Arguments.of("яндекс,москва", true),
                Arguments.of("Официальный производитель Биомаг-магнитотерапия DBS", false),
                Arguments.of("Beauty's Basket", true)
        );
    }

    @ParameterizedTest
    @MethodSource("validateInternalNameParameterizedTestData")
    public void validateInternalNameParameterizedTest(String internalName, boolean expectedValidationResult) {
        Assertions.assertEquals(expectedValidationResult,
                PartnerNameValidator.isValidMarketInternalNameFormat(internalName));
    }

    @ParameterizedTest
    @MethodSource
    public void suppressTabuSumbolsInInternalName(String internalName, String result) {
        Assertions.assertEquals(result,
                internalName.replaceAll(SUPPRESS_NAME_CHARACTERS, ""));
    }

    private static Stream<Arguments> suppressTabuSumbolsInInternalName() {
        return Stream.of(
                Arguments.of("Starbucks (R)", "Starbucks R"),
                Arguments.of("Ёж на Фонтанке 3/5", "Ёж на Фонтанке 35")
        );
    }


}
