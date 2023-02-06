package ru.yandex.market.tsum.pipelines.common.jobs.sandbox;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxTask;
import ru.yandex.market.tsum.clients.sandbox.TaskResource;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.release.dao.ReleaseService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 15.03.18
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ArcadiaHotfixJobTests {

    @Autowired
    private JobTester jobTester;

    @Test
    public void reportsFail() {
        ArcadiaHotfixJob sut = createSut();

        TestTsumJobContext context = getJobContext();

        SandboxTask task = new SandboxTask("", 1, "");
        task.setResults(new SandboxTask.Results().setTraceback("BranchNotReadyException"));

        JobManualFailException exception = null;
        try {
            sut.processFailedResult(context, task);
        } catch (JobManualFailException e) {
            exception = e;
        }

        Assert.assertNotNull(exception);
        Assert.assertEquals(
            "Automatic merge failed! Download and execute hotfix script manually:\nhttp://test",
            exception.getMessage()
        );
    }

    private ArcadiaHotfixJob createSut() {
        SandboxClient sandboxClient = mock(SandboxClient.class);

        TaskResource taskResource = new TaskResource(1);
        taskResource.setType("TSUM_HOTFIX_SCRIPT");
        taskResource.setHttpLink("http://test");

        when(sandboxClient.getResources(1)).thenReturn(Collections.singletonList(taskResource));

        return jobTester.jobInstanceBuilder(ArcadiaHotfixJob.class)
            .withBean(sandboxClient)
            .withResource(new SandboxHotfixJobConfig("", null))
            .withResource(
                SandboxHotfixCustomFields.builder()
                    .withBaseRevision("1")
                    .withRevisions(Arrays.asList("3", "4"))
                    .withBranch("hf")
                    .build()
            )
            .create();
    }

    private TestTsumJobContext getJobContext() {
        return new TestTsumJobContext(mock(ReleaseService.class), "user42");
    }
}
