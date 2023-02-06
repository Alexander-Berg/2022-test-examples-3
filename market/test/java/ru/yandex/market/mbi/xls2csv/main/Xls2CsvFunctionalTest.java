package ru.yandex.market.mbi.xls2csv.main;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.common.excel.converter.ConverterReturnCodes;
import ru.yandex.market.mbi.xls2csv.Xls2csv;


/**
 * Тесты на логику работы {@link ru.yandex.market.mbi.xls2csv.Xls2csv#processAndGetExitCode(String[])}.
 *
 * @author fbokovikov
 */
public class Xls2CsvFunctionalTest {

    private static int executeAndGetReturnCode(String... args) {
        return Xls2csv.processAndGetExitCode(args);
    }

    @Test
    public void testWrongArgsNotEnough() {
        Assert.assertEquals(ConverterReturnCodes.WRONG_ARGS, executeAndGetReturnCode());
    }

    @Test
    public void testWrongArgsTooMany() {
        Assert.assertEquals(ConverterReturnCodes.WRONG_ARGS, executeAndGetReturnCode("1", "2", "3", "4"));
    }

    @Test
    public void testIncorrectFileName() {
        Assert.assertEquals(ConverterReturnCodes.ONE_FILE_IO_EXCEPTION, executeAndGetReturnCode("wrong-name.xls"));
    }

    @Test
    public void testNonMarketTemplate() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/non_market_template.xls"));
    }

    @Test
    public void testNonMarketStreamTemplate() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/non_market_template.xlsm"));
    }

    @Test
    public void testRegressMbi26584() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/mbi_26584.xls"));
    }

    @Test
    public void testRegressStreamMbi26584() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/mbi_26584.xlsm"));
    }

    //Тестировать отстуствие ошибки чтения файла при наличии ссылок на внешние excel файлы
    @Test
    public void testRegressMbi28220() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/mbi_28220.xlsx"));
    }

    @Test
    public void testMarketTemplate() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/common_template.xls"));
    }

    @Test
    public void testMarketStreamTemplate() {
        Assert.assertEquals(ConverterReturnCodes.OK, executeAndGetReturnCode("xls/common_template.xlsm"));
    }

    /**
     * {@link ru.yandex.market.common.excel.ContentUtils#getUnescapedContent(CellType, Cell)}
     */
    @Test
    public void testMyTest() {
        int res = executeAndGetReturnCode("xls/null_like_cell.xls");
        Assert.assertEquals(ConverterReturnCodes.OK, res);
    }
}
