package ru.yandex.market.freqparser;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class ExcelParserTest {

    @Test
    public void positiveTest() throws IOException {
        String path = getClass().getClassLoader().getResource("").getPath();
        Map<Long, RenewableMsku> result = ExcelParser.parse(path);

        Assert.assertEquals(99, result.size());
        Assert.assertEquals(Long.valueOf(168L), result.get(100407442826L).getRepurchasePeriod());

        Assert.assertEquals(Long.valueOf(60L), result.get(36146556L).getRepurchasePeriod());
    }

    @Test(expected = FileNotFoundException.class)
    public void noDirTest() throws IOException {
        String path = getClass().getClassLoader().getResource("").getPath();
        ExcelParser.parse(path + "wrongDir");
    }

}
