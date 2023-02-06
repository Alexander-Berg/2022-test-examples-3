package ru.yandex.direct.jobs.mobileappssync;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.mobilecontent.MobileContentYtTablesConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.transfermanagerutils.TransferManagerConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.jobs.mobileappssync.MobileAppsSyncJob.CREATION_TIME_ATTRIBUTE;
import static ru.yandex.direct.jobs.mobileappssync.MobileAppsSyncJob.LINK_TARGET_PATH;
import static ru.yandex.direct.jobs.mobileappssync.MobileAppsSyncJob.MODIFICATION_TIME_ATTRIBUTE;
import static ru.yandex.direct.jobs.mobileappssync.YtNodeMountState.MOUNTED;
import static ru.yandex.direct.jobs.mobileappssync.YtNodeMountState.UNMOUNTED;

class MobileAppsSyncJobTest {
    @Mock
    private Yt yt;
    @Mock
    private YtProvider ytProvider;
    @Mock
    private Cypress cypress;
    @Mock
    private YtTables tables;
    @Mock
    private MobileContentYtTablesConfig outputConfig;
    @Mock
    private TransferManagerConfig transferManagerConfig;
    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    private MobileAppsSyncJobConfig.Task task;
    private MobileAppsSyncJob job;
    private YPath outputTable;
    private List<YtCluster> outputClusters;

    @BeforeEach
    void setUp() {
        initMocks(this);
        when(ytProvider.get(any())).thenReturn(yt);
        when(yt.cypress()).thenReturn(cypress);
        when(yt.tables()).thenReturn(tables);

        outputClusters = Arrays.asList(YtCluster.ZENO, YtCluster.HAHN);
        outputTable = YPath.simple("//test");
        job = spy(new MobileAppsSyncJob(ytProvider, DirectConfigFactory.getConfig(), transferManagerConfig,
                outputConfig, ppcPropertiesSupport));
        task = job.getConfig().getTasks().get(0);
    }

    @Test
    void waitMounted_success() throws Exception {
        YTreeStringNode mounted = makeStringNode(MOUNTED.name);
        when(cypress.get(outputTable.child("@tablet_state"))).thenReturn(mounted);
        job.waitForNodeMountState(yt, outputTable, Duration.ofSeconds(2), MOUNTED);
        verify(job, times(1)).mountStateEquals(yt, outputTable, MOUNTED);
    }

    @Test
    void waitMounted_wait_success() throws Exception {
        YTreeStringNode unmounted = makeStringNode(UNMOUNTED.name);
        YTreeStringNode mounted = makeStringNode(MOUNTED.name);
        when(cypress.get(outputTable.child("@tablet_state"))).thenReturn(unmounted).thenReturn(mounted);
        job.waitForNodeMountState(yt, outputTable, Duration.ofSeconds(2), MOUNTED);
        verify(job, times(2)).mountStateEquals(yt, outputTable, MOUNTED);
    }

    @Test
    void waitMounted_timeout() {
        YTreeStringNode unmounted = makeStringNode(UNMOUNTED.name);
        when(cypress.get(outputTable.child("@tablet_state"))).thenReturn(unmounted);
        assertThatThrownBy(() -> job.waitForNodeMountState(yt, outputTable, Duration.ofSeconds(2), MOUNTED))
                .isInstanceOf(TimeoutException.class);
        verify(job, atLeastOnce()).mountStateEquals(yt, outputTable, MOUNTED);
    }

    @Test
    void isRecalcNeeded_no_output() throws Exception {
        YTreeStringNode input = makeStringNode(Instant.now().toString());
        YTreeStringNode output = makeStringNode(Instant.now().minusSeconds(3600).toString());
        isRecalcNeeded_test(input, output, true, false);
    }

    @Test
    void isRecalcNeeded_true() throws Exception {
        YTreeStringNode input = makeStringNode(Instant.now().toString());
        YTreeStringNode output = makeStringNode(Instant.now().minusSeconds(3600).toString());
        isRecalcNeeded_test(input, output, true, true);
    }

    @Test
    void isRecalcNeeded_false() throws Exception {
        YTreeStringNode input = makeStringNode(Instant.now().minusSeconds(3600).toString());
        YTreeStringNode output = makeStringNode(Instant.now().toString());
        isRecalcNeeded_test(input, output, false, true);
    }

    private void isRecalcNeeded_test(YTreeStringNode input, YTreeStringNode output, boolean result,
                                     boolean outputExists) throws MobileAppsSyncException {
        if (outputExists) {
            when(cypress.exists(outputTable)).thenReturn(true);
        }
        task.inputTables.forEach(table -> when(cypress.get(table.attribute(MODIFICATION_TIME_ATTRIBUTE)))
                .thenReturn(input));
        when(cypress.get(outputTable.attribute(CREATION_TIME_ATTRIBUTE))).thenReturn(output);
        assertEquals(result, job.isRecalcNeeded(task, outputClusters, outputTable));
    }

    @Test
    void clearDataTest() {
        String link = "link";
        String linkTarget = "11111";
        String anotherLink = "anotherLink";
        String anotherLinkTarget = "22222";
        String nodeToDelete = "33333";

        ListF<YTreeStringNode> dirContents = Cf.list(link, linkTarget, anotherLink, anotherLinkTarget,
                nodeToDelete).map(this::makeStringNode);
        YPath path = YPath.cypressRoot().child("directory").child("anotherDirectory");
        when(cypress.list(eq(path))).thenReturn(dirContents);
        when(cypress.exists(eq(path.child(link + LINK_TARGET_PATH)))).thenReturn(true);
        when(cypress.exists(eq(path.child(anotherLink + LINK_TARGET_PATH)))).thenReturn(true);
        when(cypress.get(eq(path.child(link + LINK_TARGET_PATH))))
                .thenReturn(makeStringNode(path.child(linkTarget).toString()));
        when(cypress.get(eq(path.child(anotherLink + LINK_TARGET_PATH))))
                .thenReturn(makeStringNode(path.child(anotherLinkTarget).toString()));
        dirContents.forEach(node -> when(cypress.get(path.child(node.getValue()).child("@tablet_state")))
                .thenReturn(makeStringNode(MOUNTED.name)).thenReturn(makeStringNode(UNMOUNTED.name)));
        doNothing().when(tables).unmount(any());
        doNothing().when(cypress).remove((YPath) any());

        job.clearOldData(path.child(linkTarget), YtCluster.ZENO);

        verify(tables).unmount(eq(path.child(nodeToDelete)));
        verify(cypress).remove(eq(path.child(nodeToDelete)));
        verify(cypress, never()).remove(eq(path.child(link)));
        verify(cypress, never()).remove(eq(path.child(linkTarget)));
        verify(cypress, never()).remove(eq(path.child(anotherLink)));
        verify(cypress, never()).remove(eq(path.child(anotherLinkTarget)));
    }

    private YTreeStringNodeImpl makeStringNode(String anotherLinkTarget) {
        return new YTreeStringNodeImpl(anotherLinkTarget, Cf.map());
    }
}
