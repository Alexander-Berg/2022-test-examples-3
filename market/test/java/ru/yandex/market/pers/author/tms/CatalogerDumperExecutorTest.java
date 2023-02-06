package ru.yandex.market.pers.author.tms;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author grigor-vlad
 * 16.08.2021
 */
public class CatalogerDumperExecutorTest extends PersAuthorTest {
    private static final String CATALOGER_VERSION = "cataloger.version";

    @Autowired
    private CatalogerDumperExecutor catalogerDumperExecutor;
    @Autowired
    private CatalogerClient catalogerClient;
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void testDumpSameVersion() {
        configurationService.mergeValue(CATALOGER_VERSION, "current");
        catalogerDumperExecutor.dump();

        Mockito.verify(catalogerClient, Mockito.times(0)).getNavigationTreeFromDepartment();
    }

    @Test
    public void testDumpNewVersion() {
        configurationService.mergeValue(CATALOGER_VERSION, "old");
        catalogerDumperExecutor.dump();

        Mockito.verify(catalogerClient, Mockito.times(1)).getNavigationTreeFromDepartment();
        assertEquals(147, jdbcTemplate.queryForList("select hid, root_nid from pers.hid_to_root_nid").size());
        assertEquals("current", catalogerDumperExecutor.getCurrentVersion());
    }

}
