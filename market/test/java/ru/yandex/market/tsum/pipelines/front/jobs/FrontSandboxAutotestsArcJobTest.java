package ru.yandex.market.tsum.pipelines.front.jobs;

import org.assertj.core.util.Strings;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.datasource.DataSourceProperty;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.ArcDefaultParams;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FrontSandboxAutotestsArcJobTest {
    @Autowired
    private JobTester jobTester;

    TestTsumJobContext context = new TestTsumJobContext("unknown");

    private static final DataSourceProperty WITH_COCON_COMMIT = new DataSourceProperty(
        null, null, "cocon_commit_id", "12345"
    );
    private static final String SELF_ENVIRON = "{\"foo\": 5}";

    @Test
    public void testNoDatasourceNoSelfEnviron() throws Exception {
        FrontSandboxAutotestsArcJob job = createJob(null, null);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        var result = findEnvironField(taskInput);

        Assert.assertNull("no custom environ field", result);
    }

    @Test
    public void testHasDatasourceNoSelfEnviron() throws Exception {
        FrontSandboxAutotestsArcJob job = createJob(null, WITH_COCON_COMMIT);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        var result = findEnvironField(taskInput);
        var json = new JSONObject(result.getValue().toString());

        assertHasCoconCommit(json);
    }

    @Test
    public void testNoDatasourceHasSelfEnviron() throws Exception {
        FrontSandboxAutotestsArcJob job = createJob(SELF_ENVIRON, null);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        var result = findEnvironField(taskInput);
        var json = new JSONObject(result.getValue().toString());

        assertHasSelfEnviron(json);
    }

    @Test
    public void testHasDatasourceHasSelfEnviron() throws Exception {
        FrontSandboxAutotestsArcJob job = createJob(SELF_ENVIRON, WITH_COCON_COMMIT);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        var result = findEnvironField(taskInput);
        var json = new JSONObject(result.getValue().toString());

        assertHasSelfEnviron(json);
        assertHasCoconCommit(json);
    }

    @Test
    public void testHasDatasourceWithEmptyValueHasSelfEnviron() throws Exception {


        var datasourceProperty = new DataSourceProperty(null, null, "cocon_commit_id", "");

        FrontSandboxAutotestsArcJob job = createJob(SELF_ENVIRON, datasourceProperty);
        TaskInputDto taskInput = new TaskInputDto("");

        job.prepareTask(context, taskInput);

        var result = findEnvironField(taskInput);
        var json = new JSONObject(result.getValue().toString());

        assertHasSelfEnviron(json);
        Assert.assertFalse("no need for empty cocon commit id", json.has("COCON_COMMIT_ID"));
    }

    private TaskInputDto.TaskFieldValidateItem findEnvironField(TaskInputDto t) {
        return t.getCustomFields().stream()
            .filter(f -> "environ".equals(f.getName()))
            .findFirst()
            .orElse(null);
    }

    private FrontSandboxAutotestsArcJob createJob(
        String environFromOuterTask,
        DataSourceProperty datasourceProperty
    ) {
        var sandboxTaskConfigBuilder = SandboxTaskJobConfig.newBuilder("FOO_BAR");
        if (!Strings.isNullOrEmpty(environFromOuterTask)) {
            sandboxTaskConfigBuilder.setCustomParam("environ", environFromOuterTask);
        }

        JobInstanceBuilder<FrontSandboxAutotestsArcJob> jobBuilder =
            jobTester.jobInstanceBuilder(FrontSandboxAutotestsArcJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withResource(new FrontSandboxAutotestsJobConfig())
                .withResource(new ArcDefaultParams("trunk", "market/front/apps/partner"))
                .withResource(sandboxTaskConfigBuilder.build());

        if (datasourceProperty != null) {
            jobBuilder.withResource(datasourceProperty);
        }

        return jobBuilder.create();
    }

    private void assertHasSelfEnviron(JSONObject json) {
        Assert.assertTrue("keep value from sb task", json.has("foo") && json.getInt("foo") == 5);
    }

    private void assertHasCoconCommit(JSONObject json) {
        Assert.assertTrue(
            "got value from pipeline",
            json.has("COCON_COMMIT_ID") && "12345".equals(json.getString("COCON_COMMIT_ID"))
        );
    }
}
