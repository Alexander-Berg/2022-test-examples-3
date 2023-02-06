package ru.yandex.market.mboc.tms.executors.dbs;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.yt.TestYt;
import ru.yandex.market.mboc.common.infrastructure.util.UnstableInit;
import ru.yandex.market.mboc.common.services.dsbs.YqlUnfreezeHidingDsbs;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author apluhin
 * @created 2/21/22
 */
public class OldFashionDsbsUnfreezeExecutorTest extends BaseDbTestClass {

    private static final String ENABLED_KEY = "OldFashionDsbsUnfreezeExecutor.enabled";
    private static final String LAST_UPLOAD_DATE = "OldFashionDsbsUnfreezeExecutor.last_upload_date";
    private static final String TEST_PATH = "//test";

    private OldFashionDsbsUnfreezeExecutor oldFashionDsbsUnfreezeExecutor;
    private JdbcTemplate jdbcTemplate;
    private YqlUnfreezeHidingDsbs yqlUnfreezeHidingDsbs;
    private Yt yt;
    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() throws Exception {
        yqlUnfreezeHidingDsbs = Mockito.mock(YqlUnfreezeHidingDsbs.class);
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        yt = new TestYt();
        oldFashionDsbsUnfreezeExecutor = new OldFashionDsbsUnfreezeExecutor(
            yqlUnfreezeHidingDsbs,
            UnstableInit.simple(yt),
            jdbcTemplate,
            storageKeyValueService
        );
        storageKeyValueService.putValue(ENABLED_KEY, true);
        ReflectionTestUtils.setField(oldFashionDsbsUnfreezeExecutor, "unfreezePath", TEST_PATH);
    }

    @Test
    public void testRerunYql() {
        oldFashionDsbsUnfreezeExecutor.execute();
        Mockito.verify(yqlUnfreezeHidingDsbs, Mockito.times(1))
            .handleTable(Mockito.anyString());
        Mockito.verify(jdbcTemplate, Mockito.times(1))
            .update(Mockito.anyString());
    }

    @Test
    public void testIgnoreCompletedDate() {
        storageKeyValueService.putValue(LAST_UPLOAD_DATE, LocalDate.now());
        oldFashionDsbsUnfreezeExecutor.execute();
        Mockito.verify(yqlUnfreezeHidingDsbs, Mockito.times(0))
            .handleTable(Mockito.anyString());
        Mockito.verify(jdbcTemplate, Mockito.times(0))
            .update(Mockito.anyString());
    }

}

