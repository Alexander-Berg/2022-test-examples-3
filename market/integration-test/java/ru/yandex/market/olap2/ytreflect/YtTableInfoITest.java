package ru.yandex.market.olap2.ytreflect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.load.TestLoadTask;
import ru.yandex.market.olap2.yt.YtWrapper;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class YtTableInfoITest {
    @Autowired
    public YtTableInfo ytTableInfo;

    @Autowired
    public YtWrapper ytWrapper;

    @Test
    public void testGetColumns() {
        TestLoadTask task = new TestLoadTask("tstevid1", YtTestTable.TBL);
        assertThat(ytTableInfo.getYtColumns(task), is(YtTestTable.COLUMNS));
    }

    @Test
    public void testGetPrimaryColumns() {
        TestLoadTask task = new TestLoadTask("tstevid1",YtTestTable.TBL);
        assertThat(ytTableInfo.getYtPrimaryColumnNames(task), is(Arrays.asList("pk_int_col")));
    }
}
