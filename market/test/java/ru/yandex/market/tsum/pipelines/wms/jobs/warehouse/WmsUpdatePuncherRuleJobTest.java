package ru.yandex.market.tsum.pipelines.wms.jobs.warehouse;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.impl.EmptyIterator;
import ru.yandex.market.tsum.clients.puncher.PuncherClient;
import ru.yandex.market.tsum.clients.puncher.models.PuncherProtocol;
import ru.yandex.market.tsum.clients.puncher.models.PuncherResult;
import ru.yandex.market.tsum.context.TestTsumJobContext;
import ru.yandex.market.tsum.context.TsumJobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.exceptions.JobManualFailException;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.JobInstanceBuilder;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsExternalTemplateParams;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsParentIssue;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsPuncherRule;
import ru.yandex.market.tsum.pipelines.wms.resources.warehouse.WmsPuncherSearchRequest;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.model.Issue;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WmsUpdatePuncherRuleJobTest {

    public static final String TEST_SOURCE = "wms-test.test.yandex.net";
    public static final String TEST_DESTINATION = "wms-sql-test.test.yandex.net";
    public static final String TEST_PORT_STR = "1234";
    public static final int TEST_PORT = 1234;
    private final TsumJobContext jobContext = new TestTsumJobContext(null);

    @Mock
    private Issues startrekMock;
    @Mock
    private Issue issueMock;
    @Mock
    private PuncherClient puncherClient;
    @Mock
    private PuncherResult resultMock;

    @Before
    public void before() {
        when(startrekMock.get("MARKETTEST-1234")).thenReturn(issueMock);
        when(issueMock.getComments()).thenReturn(new EmptyIterator<>());
    }

    @Test
    public void shouldFindPuncherRule() throws Exception {
        ArgumentCaptor<String> sourceCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destCapture = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> portCapture = ArgumentCaptor.forClass(List.class);

        WmsPuncherSearchRequest wmsPuncherSearchRequest =
            new WmsPuncherSearchRequest(TEST_SOURCE,
                TEST_DESTINATION, TEST_PORT);


        when(puncherClient.findRules(sourceCapture.capture(),
            destCapture.capture(),
            Mockito.isNull(),
            portCapture.capture()
        )).thenReturn(resultMock);

        WmsUpdatePuncherRuleJob job = JobInstanceBuilder.create(WmsUpdatePuncherRuleJob.class)
            .withResources(
                makePuncherRuleWithoutWait(),
                new WmsParentIssue("MARKETTEST-1234"),
                new WmsExternalTemplateParams(Map.of("whCode", "test")),
                wmsPuncherSearchRequest)
            .withBeans(puncherClient, startrekMock)
            .create();

        try {
            job.execute(jobContext);
        } catch (JobManualFailException e) {
            //don't care we don't test it
        }

        Assert.assertEquals(TEST_SOURCE, sourceCapture.getValue());
        Assert.assertEquals(TEST_DESTINATION, destCapture.getValue());
        Assert.assertEquals(Collections.singletonList(TEST_PORT_STR), portCapture.getValue());
    }

    private WmsPuncherRule makePuncherRuleWithoutWait() {
        return new WmsPuncherRule(List.of("wms-{{ whCode }}.test.yandex.net"),
            List.of("wms-sql-{{ whCode }}.test.yandex.net"),
            PuncherProtocol.TCP,
            List.of("1234"),
            "Test {{ comm }}",
            false
        );
    }

}
