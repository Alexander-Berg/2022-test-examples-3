package ru.yandex.market.ir.dao;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.TestUtil;
import ru.yandex.market.ir.http.ReportAliases;

import static org.junit.Assert.assertEquals;

public class ReportAliasesDaoTest {

    private final String filePath = TestUtil.getSrcTestResourcesPath() + "/reportAliasesDaoTest.pb";
    private ReportAliasesDao reportAliasesDao;

    @Before
    public void setUp() {
        reportAliasesDao = new ReportAliasesDao(filePath);
    }

    @Test
    public void getAliasesByValueId() {
        List<ReportAliases.Alias> aliases = reportAliasesDao.getAliases(100, 200);
        assertEquals(1, aliases.size());
        ReportAliases.Alias alias = aliases.get(0);
        assertEquals("Терминатор", alias.getWord());
        assertEquals("A", alias.getLayer());
    }

}
