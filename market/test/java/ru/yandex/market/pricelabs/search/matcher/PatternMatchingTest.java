package ru.yandex.market.pricelabs.search.matcher;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.search.matcher.PatternMatching.matcher;
import static ru.yandex.market.pricelabs.search.matcher.PatternMatching.normalize;

@Slf4j
class PatternMatchingTest {

    @Test
    void testPattern() {
        log.info("{}", matcher("телефон|смартфон -apple -samsung"));
        log.info("{}", matcher("телефон|смартфон -\"apple\" -samsung"));
        log.info("{}", matcher("телефон|Смартфон \"-apple\" -Samsung"));
        log.info("{}", matcher("телефон|смартфон звонилка -apple -samsung"));
        log.info("{}", matcher("телефон|\"смартфон\" -apple -samsung"));
        log.info("{}", matcher("телефон|\"смартфон и  смартфон\" -apple -samsung"));
        log.info("{}", matcher("  телефон-5    | \"смартфон\" -apple -samsung"));
        log.info("{}", matcher("телефон"));
        log.info("{}", matcher("сэмпл"));
        log.info("{}", matcher("?|'"));
        log.info("{}", matcher("Acuvue OASYS -\"1-Day\" -\"Astigmatism \""));
    }

    @ParameterizedTest
    @MethodSource("normalizeSamples")
    void testNormalize(String text, String expectNormalized) {
        var norm = normalize(text);
        assertEquals(expectNormalized, norm);
    }

    @ParameterizedTest
    @MethodSource("matchSamples")
    void testMatch(String pattern, String sample, boolean expectMatch) {
        check(pattern, sample, expectMatch);
    }

    private static void check(String pattern, String text, boolean expectMatch) {
        var matcher = matcher(pattern);
        log.info("Pattern [{}] -> {}", pattern, matcher);

        var norm = normalize(text);
        boolean matched = matcher.isMatched(norm);
        log.info("Normalized [{}] -> [{}] = {}", text, norm, matched);
        assertEquals(expectMatch, matched);
    }

    static Object[][] normalizeSamples() {
        return new Object[][]{
                {"offer-q0-test", "+offer+q0+test+"},
                {"offer     -q0  ( test", "+offer+q0+test+"},
                {"offer  \" -q0\"  '(((test))))", "+offer+q0+test+"},
                {"Тест  и тест и ТЕВСТ(99) dfdf99+1", "+тест+и+тест+и+тевст+99+dfdf99+1+"},
                {"бумага Lomond LM120 матовая A4/100л(0102003)", "+бумага+lomond+lm120+матовая+a4+100л+0102003+"}
        };
    }

    static Object[][] matchSamples() {
        return new Object[][]{
                {"\"A4,100л\"", "бумага Lomond LM120 матовая A4/100л(0102003)", true},
                {"\"A4,100л\"", "бумага Lomond LM160 матовая A4/100л (0102005)", true},
                {"\"A4,100л\"", "фотобумага Lomond LM90 матовая A4/100л (0102001)", true},
                {"\"A4,100л\"", "бумага Lomond LM230 глянцевая_бумага A4/50л (0102022)", false},
                {"\"A4-100л\"", "фотобумага Lomond LM90 матовая A4/100л (0102001)", true},
                {"\"A4-100л\"", "бумага Lomond LM230 глянцевая_бумага A4/50л (0102022)", false},
                {"category-100", "category-100", true},
                {"category-100", "category-101", false},
                {"offer-Q\"0\"", "offer-q0-test", false},
                {"offer-Q\"0\"", "offer-q\"0\"-test", true},
                {"\"\"", "any", true},
                {"test,123,345", "1234", true},
                {"test,123,345", "123", true},
                {"345", "123", false},
                {"345", "345", true},
                {"345", "3453", true},
                {"344,345,346", "3453", true},
                {"Acuvue OASYS -\"1-Day\" -\"Astigmatism \"",
                        "Контактные линзы Acuvue Acuvue Oasys with Hydraclear Plus 6 шт", true},
                {"Acuvue OASYS -\" 1-Day \" -\"Astigmatism \"",
                        "Контактные линзы Acuvue Acuvue Oasys with Hydraclear Plus 6 шт", true},
                {"Acuvue OASYS -\"1-Day\" -\"Astigmatism \"",
                        "Контактные линзы Acuvue OASYS 1-Day with HydraLuxe Technology, 30 шт (8.5, -0.50)", false},
                {"Acuvue OASYS -\"1-Day\" -\"Astigmatism \"",
                        "Контактные линзы Acuvue OASYS test 1 with HydraLuxe Technology, 30 шт (8.5, -0.50)", true},
                {"Acuvue OASYS -\"Oasys Hydraclear\"",
                        "Контактные линзы Acuvue Acuvue Oasys with Hydraclear Plus 6 шт", true},
                {"Acuvue OASYS -\"Oasys Hydraclear\"",
                        "Контактные линзы Acuvue Acuvue Oasys Hydraclear Plus 6 шт", false},
                {"Acuvue OASYS \"Oasys Hydraclear\"",
                        "Контактные линзы Acuvue Acuvue Oasys with Hydraclear Plus 6 шт", false},
                {"Acuvue OASYS \"Oasys Hydraclear\"",
                        "Контактные линзы Acuvue Acuvue Oasys Hydraclear Plus 6 шт", true}
        };
    }

}
