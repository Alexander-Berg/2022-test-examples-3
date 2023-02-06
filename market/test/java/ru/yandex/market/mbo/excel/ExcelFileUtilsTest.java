package ru.yandex.market.mbo.excel;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ExcelFileUtilsTest {

    @Test
    @SuppressWarnings("checkstyle:MagicNumber")
    public void testToColumnChar() {
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(0)).isEqualTo("A");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(1)).isEqualTo("B");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(25)).isEqualTo("Z");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(26 + 0)).isEqualTo("AA");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(26 + 1)).isEqualTo("AB");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(26 + 2)).isEqualTo("AC");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(26 + 25)).isEqualTo("AZ");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(2 * 26 + 0)).isEqualTo("BA");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(2 * 26 + 1)).isEqualTo("BB");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(2 * 26 + 2)).isEqualTo("BC");
        Assertions.assertThat(ExcelFileUtils.getColumnLetter(2 * 26 + 25)).isEqualTo("BZ");
    }
}
