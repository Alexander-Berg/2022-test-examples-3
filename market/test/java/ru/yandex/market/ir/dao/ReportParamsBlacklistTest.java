package ru.yandex.market.ir.dao;

import org.junit.Test;

import ru.yandex.market.ir.TestUtil;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReportParamsBlacklistTest {

    private static final String DIR_PATH = TestUtil.getSrcTestResourcesPath();
    //в случае запуска отдельного теста надо прописывать абсолютный путь(ну или придумать другое решение)
    //например "/Users/gavrilov-mi/arcadia/market/ir/formalizer/src/test/resources";

    @Test
    public void contains() {
        ReportParamsBlacklist reportParamsBlacklist = new ReportParamsBlacklist(DIR_PATH + "/blacklist.txt");
        assertFalse(reportParamsBlacklist.isEmpty());
        assertTrue(reportParamsBlacklist.contains(28598861));
        assertFalse(reportParamsBlacklist.contains(123456));
    }

    @Test
    public void fileNotFound() {
        ReportParamsBlacklist reportParamsBlacklist = new ReportParamsBlacklist(DIR_PATH + "/FileNotExists");
        assertTrue(reportParamsBlacklist.isEmpty());
    }

    @Test
    public void wrongFormatFile() {
        ReportParamsBlacklist reportParamsBlacklist = new ReportParamsBlacklist(DIR_PATH + "/incorrect_blacklist.txt");
        assertTrue(reportParamsBlacklist.isEmpty());
    }
}
