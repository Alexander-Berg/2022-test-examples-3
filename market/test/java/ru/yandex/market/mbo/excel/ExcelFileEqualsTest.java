package ru.yandex.market.mbo.excel;

import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ExcelFileEqualsTest {
    private static final long SEED = "MBO-15486".hashCode();
    private final ExcelFileRandomizer excelFileRandomizer = new ExcelFileRandomizer(SEED);

    @Test
    public void testCopy() {
        for (int i = 0; i < 100; i++) {
            ExcelFile excelFile = excelFileRandomizer.getRandomValue();
            ExcelFile copy = new ExcelFile(excelFile);

            Assertions.assertThat(copy).isEqualTo(excelFile);
        }
    }
}
