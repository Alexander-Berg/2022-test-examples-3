package ru.yandex.market.gutgin.tms.datafile.excel;

import junit.framework.Assert;
import org.junit.Test;

public class ExcelFileUtilsTest {

    @Test
    public void checkSanitizing() {
        Assert.assertEquals(ExcelFileUtils.sanitizeString("\u0000"), "");
    }
}
