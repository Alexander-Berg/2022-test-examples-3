package ru.yandex.market.mbo.tms.yt.modelstorage.backup;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.tms.configs.YtTestConfiguration;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Ignore("For manual use. Uses YT")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = YtTestConfiguration.class)
public class BackupYtModelsExecutorTest {

    @Value("${mbo.yt.rootPath}")
    private String mboRootPath;

    @Inject
    private Yt ytApi;

    @Inject
    private BackupYtModelsExecutor backupYtModelsExecutor;

    private JobExecutionContext mock = mock(JobExecutionContext.class);
    private YPath backupDirPath;

    @Test
    public void doRealJob() throws Exception {
        backupYtModelsExecutor.doRealJob(mock);

        backupDirPath = YPath.simple(mboRootPath).child("backup");

        assertThat(ytApi.cypress().exists(backupDirPath), is(true));
        assertThat(ytApi.cypress().list(backupDirPath).size(), is(greaterThan(0)));
    }
}
