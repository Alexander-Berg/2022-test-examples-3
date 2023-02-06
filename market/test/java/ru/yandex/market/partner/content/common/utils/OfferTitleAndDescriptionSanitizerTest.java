package ru.yandex.market.partner.content.common.utils;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.utils.OfferTitleAndDescriptionSanitizer.sanitizeString;

public class OfferTitleAndDescriptionSanitizerTest {

    @Test
    public void test() {
        String unprocessedString = "\"\"Some string with\"\" emoji\" :)asd and,punctuation problems'";
        String expectedString = "Some string with\" emoji\" asd and, punctuation problems";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testQuotationAtBeginOnlyShouldRetain() {
        String unprocessedString = "'\"Some string with\"\" quotation\" problems";
        String expectedString = "\"Some string with\" quotation\" problems";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testQuotationAtEndOnlyShouldRetain() {
        String unprocessedString = "Some string with\"\" emoji\" :)asd and,punctuation problems\"»'";
        String expectedString = "Some string with\" emoji\" asd and, punctuation problems'";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testQuotationShouldRemove() {
        String unprocessedString = "\"\"Some string with\"\" quotation\" problems'''";
        String expectedString = "Some string with\" quotation\" problems";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testOnlyThreeQuotationsShouldRemove() {
        String unprocessedString = "\"\"'";
        String expectedString = "";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testOnlyTwoQuotationsShouldRemove() {
        String unprocessedString = "\"\"";
        String expectedString = "";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testReplaceTabsWithSpace() {
        String unprocessedString = "\t testing\ttest \tstring\t testing \t";
        String expectedString = "testing test string testing";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testReplaceSeveralTabsWithSpace() {
        String unprocessedString = "\t\t testing\t\tanother \t\ttest\t\t string\t \ttesting \t\t";
        String expectedString = "testing another test string testing";

        assertThat(sanitizeString(unprocessedString))
                .isEqualTo(expectedString);
    }

    @Test
    public void testReplaceSeveralReturnsWithSpace() {
        String unprocessedString = "\t\n testing\n\nanother \ntest\t\t string\n \ntesting \n\t";
        String expectedString = "testing another test string testing";

        assertThat(sanitizeString(unprocessedString, true))
                .isEqualTo(expectedString);
    }

    @Test
    public void testStringWithDashSign() {
        List<String> stringsWithLongDash = Arrays.asList(
                "bk-test-101121-valid2",
                "bk-test-101121- valid2",
                "bk-test-101121 -valid2",
                "bk-test-101121 - valid2"
        );
        stringsWithLongDash.forEach(input -> assertThat(sanitizeString(input)).isEqualTo(input));
    }

    @Test
    public void testStringWithLongDashSign() {
        List<String> stringsWithLongDash = Arrays.asList(
                "Размер фигурки—28 см.",
                "Размер фигурки —28 см.",
                "Размер фигурки— 28 см.",
                "Размер фигурки — 28 см."
        );
        stringsWithLongDash.forEach(input -> {
            String result = sanitizeString(input);
            assertThat(result)
                    .overridingErrorMessage("Failed for input: '" + input + "' result: '" + result +"'")
                    .isEqualTo("Размер фигурки — 28 см.");
        });
    }

    @Test
    public void testFromTicket() {
        String inputString = "\"Набор махровых полотенец \"\"Вышневолоцкий Текстиль\"\"; 528 Яркий зеленый; Набор из " +
                "3 штук\"";
        String result = sanitizeString(inputString);
        assertThat(result).isEqualTo("Набор махровых полотенец \"Вышневолоцкий Текстиль\"; 528 Яркий зеленый; Набор " +
                "из 3 штук");
    }
}
