package ru.yandex.market.tsum.pipelines.ir.multitestings;

import nanny.pod_sets_api.PodSetsApi;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.nanny.NannyYpApiClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.sandbox.TaskInputDto;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.config.JobTesterConfig;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobTester;
import ru.yandex.market.tsum.pipelines.common.jobs.sandbox.SandboxTaskJobConfig;
import ru.yandex.market.tsum.pipelines.common.resources.ArcadiaRef;
import ru.yandex.market.tsum.pipelines.common.resources.NannyService;
import ru.yandex.yp.client.api.Autogen;
import ru.yandex.yp.client.api.DataModel;

import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobTesterConfig.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SandboxFormalizerB2bTestsYpJobTest {
    @Autowired
    private JobTester jobTester;

    @Before
    public void init() {
    }

    @Test
    public void testSandboxFormalizerJobCustomFields() throws Exception {
        TestTsumJobContext context = new TestTsumJobContext("test");
        TaskInputDto taskInput = new TaskInputDto("");

        SandboxFormalizerB2bTestsYpJob sandboxFormalizerB2bTestsYpJob = createSandboxFormalizerB2bTestsYpJob();
        sandboxFormalizerB2bTestsYpJob.prepareTask(context, taskInput);

        Assert.assertEquals(ArcadiaRef.REF_DEFAULT_VALUE, getFieldAsString(taskInput, "checkout_arcadia_from_url"));
        Assert.assertEquals("market/ir/b2b/run.sh", getFieldAsString(taskInput, "program"));
        Assert.assertEquals(" --batch-size=200" +
                " --stable-host=http://[stable]:80/" +
                " --testing-host=http://[testing]:80/" +
                " --input-path=//home/market/users/a-shar/offers" +
                " --main-class=ru.yandex.autotests.market.services.formalizer.Main" +
                " --report-full-file-name=full-report.json" +
                " --report-overview-file-name=overview-report.html" +
                " --report-full-archive-file-name=full-report.json.gz" +
                " --ext_opts=\"-Dformalizer.request.return.hypotheses=true" +
                "-Dformalizer.request.return.all.positions=true" +
                "-Dformalizer.request.apply.change.category.rules=true\"",
            getFieldAsString(taskInput, "program_args"));
    }

    private String getFieldAsString(TaskInputDto taskInput, String program) {
        return (String) taskInput.getCustomFields().stream()
            .filter(field -> field.getName().equals(program)).findFirst().orElseThrow().getValue();
    }


    private SandboxFormalizerB2bTestsYpJob createSandboxFormalizerB2bTestsYpJob() {
        NannyYpApiClient ypApiClient = Mockito.mock(NannyYpApiClient.class);
        Mockito.when(ypApiClient.listPods(any(), any())).then(invocation -> {
            String serviceId = invocation.getArgument(0);
            return PodSetsApi.ListPodsResponse.newBuilder()
                .addPods(
                    Autogen.TPod.newBuilder()
                        .setStatus(DataModel.TPodStatus.newBuilder()
                            .addIp6AddressAllocations(
                                DataModel.TPodStatus.TIP6AddressAllocation.newBuilder()
                                    .setVlanId("backbone")
                                    .setAddress(serviceId)
                            )))
                .build();
        });

        JobInstanceBuilder<SandboxFormalizerB2bTestsYpJob> jobBuilder =
            jobTester.jobInstanceBuilder(SandboxFormalizerB2bTestsYpJob.class)
                .withBean(Mockito.mock(SandboxClient.class))
                .withBean(ypApiClient)
                .withResources(Mockito.mock(SandboxTaskJobConfig.class))
                .withResource(new IrB2bFormalizerParams(true, true, true))
                .withResource(new IrOffersStorage())
                .withResources(new NannyService("stable"), new NannyService("testing"));

        return jobBuilder.create();
    }
}
