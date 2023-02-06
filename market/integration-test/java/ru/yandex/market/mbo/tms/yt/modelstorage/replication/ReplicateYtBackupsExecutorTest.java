package ru.yandex.market.mbo.tms.yt.modelstorage.replication;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.tms.configs.YtTestConfiguration;

/**
 * @author galaev@yandex-team.ru
 * @since 06/12/2018.
 */
@Ignore("For manual use. Uses YT")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = YtTestConfiguration.class)
public class ReplicateYtBackupsExecutorTest {

    @Value("${mbo.yt.rootPath}")
    private String mboRootPath;

    @Autowired
    private Yt ytReplicaApi;

    @Autowired
    private ReplicateYtBackupsExecutor replicateYtBackupsExecutor;

    private JobExecutionContext context = Mockito.mock(JobExecutionContext.class);

    @Test
    public void doRealJob() throws Exception {
        replicateYtBackupsExecutor.doRealJob(context);

        YPath replicatedRecent = YPath.simple(mboRootPath).child("backup").child("recent");
        Assertions.assertThat(ytReplicaApi.cypress().exists(replicatedRecent)).isTrue();
    }
}
