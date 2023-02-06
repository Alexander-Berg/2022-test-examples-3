package ru.yandex.market.mbo.core.title;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class QuotesTest {

    @Test
    public void testQuotation() {
        assertParsedTo("", "");
        assertParsedTo("   ", "   ");
        assertParsedTo("Joint-stock Company \"Vector\"", "Joint-stock Company \"Vector\"");
        assertParsedTo("ООО Вектор",                "«ООО Вектор»");
        assertParsedTo("  ООО Вектор    ",          "«ООО Вектор»");
        assertParsedTo("  \"ООО Вектор\" ",         "«ООО Вектор»");
        assertParsedTo("«ООО Вектор»",              "«ООО Вектор»");
        assertParsedTo("   «ООО Вектор» ",          "«ООО Вектор»");

        assertParsedTo("\"ООО\" Вектор",            "«ООО» Вектор");
        assertParsedTo("ООО \"Вектор\"",            "ООО «Вектор»");
        assertParsedTo("«ООО» Вектор",              "«ООО» Вектор");
        assertParsedTo("ООО «Вектор»",              "ООО «Вектор»");
        assertParsedTo("\"ООО \"Вектор\"",          "«ООО «Вектор»");
        assertParsedTo("\"ООО\" Вектор\"",          "«ООО» Вектор»");

        assertParsedTo("«ООО» «Вектор»",        "«ООО» «Вектор»");
        assertParsedTo("\"ООО\" \"Вектор\"",    "«ООО» «Вектор»");

        assertParsedTo("\"АБВ \"ГДЕ\" ЁЖЗ\"", "«АБВ «ГДЕ» ЁЖЗ»");
    }

    private void assertParsedTo(String input, String expected) {
        assertEquals(expected, Quotes.getQuotated(input));
    }

}
