package ru.yandex.direct.mysql.ytsync.synchronizator.monitoring;

import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.mysql.ytsync.common.compatibility.YtSupport;
import ru.yandex.direct.mysql.ytsync.synchronizator.util.SyncConfig;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doReturn;

public class SyncStateCheckerTest {

    private static final String STABLE_LINK_PATH = "//home/direct/mysql-sync/current";
    private static final String ROOT_PATH = "//home/direct/mysql-sync/v.4";

    @Mock
    private SyncConfig syncConfig;

    @Mock
    private YtSupport ytSupport;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        doReturn(STABLE_LINK_PATH).when(syncConfig).stableLinkPath();
        doReturn(ROOT_PATH).when(syncConfig).rootPath();
        doReturn(CompletableFuture.completedFuture(true)).when(ytSupport)
                .exists(startsWith(STABLE_LINK_PATH));
        doReturn(CompletableFuture.completedFuture(YTree.stringNode(ROOT_PATH))).when(ytSupport)
                .getNode(startsWith(STABLE_LINK_PATH));
    }


    @Test
    public void checkStableState() {
        SyncStateChecker syncStateChecker = new SyncStateChecker(syncConfig, ytSupport);
        SyncState syncState = syncStateChecker.getSyncState();

        assertThat(syncState).isEqualTo(SyncState.STABLE);
    }

    @Test
    public void checkPreStableState_WhenLinkTargetPath_NotEqualWithRootPath() {
        String linkTargetPath = YtPathUtil.generateTemporaryPath();
        doReturn(CompletableFuture.completedFuture(YTree.stringNode(linkTargetPath)))
                .when(ytSupport).getNode(ArgumentMatchers.startsWith(STABLE_LINK_PATH));

        SyncStateChecker syncStateChecker = new SyncStateChecker(syncConfig, ytSupport);
        SyncState syncState = syncStateChecker.getSyncState();

        assertThat(syncState).isEqualTo(SyncState.PRESTABLE);
    }

    @Test
    public void checkPreStableState_WhenLinkNotExists() {
        doReturn(CompletableFuture.completedFuture(false)).when(ytSupport)
                .exists(ArgumentMatchers.startsWith(STABLE_LINK_PATH));

        SyncStateChecker syncStateChecker = new SyncStateChecker(syncConfig, ytSupport);
        SyncState syncState = syncStateChecker.getSyncState();

        assertThat(syncState).isEqualTo(SyncState.PRESTABLE);
    }
}
