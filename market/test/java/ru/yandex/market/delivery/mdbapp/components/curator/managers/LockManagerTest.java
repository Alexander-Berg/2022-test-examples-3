package ru.yandex.market.delivery.mdbapp.components.curator.managers;

import java.util.Optional;

import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.zookeeper.CreateMode;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.mdbapp.components.curator.Curator;
import ru.yandex.market.delivery.mdbapp.components.curator.exceptions.CuratorException;

import static org.mockito.ArgumentMatchers.eq;

public class LockManagerTest {
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    public static final String TEST_LOCK_SUB_PATH = "test";
    public static final String PATH = LockManager.lockName(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH);
    public static final byte[] LOCAL_ADDRESS = CuratorFrameworkFactory.getLocalAddress();

    private LockManager lockManager;
    private Curator curator;

    @BeforeEach
    void setUp() {
        curator = Mockito.mock(Curator.class);
        lockManager = new LockManager(curator);
    }

    @Test
    public void testLockNew() {
        Mockito.doReturn(false).when(curator).isNodeExists(eq(PATH));
        Mockito.doReturn(true).when(curator).createNode(eq(PATH), eq(LOCAL_ADDRESS), eq(CreateMode.EPHEMERAL));

        softly.assertThat(lockManager.canProceed(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isTrue();

        Mockito.verify(curator).isNodeExists(eq(PATH));
        Mockito.verify(curator, Mockito.never()).getNode(eq(PATH));
        Mockito.verify(curator).createNode(eq(PATH), eq(LOCAL_ADDRESS), eq(CreateMode.EPHEMERAL));
    }

    @Test
    public void testLockReEnter() {
        Mockito.doReturn(true).when(curator).isNodeExists(eq(PATH));
        Mockito.doReturn(LOCAL_ADDRESS).when(curator).getNode(eq(PATH));

        softly.assertThat(lockManager.canProceed(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isTrue();

        Mockito.verify(curator).isNodeExists(eq(PATH));
        Mockito.verify(curator).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).createNode(eq(PATH), eq(LOCAL_ADDRESS), eq(CreateMode.EPHEMERAL));
    }

    @Test
    public void testLockedByAnotherHost() {
        Mockito.doReturn(true).when(curator).isNodeExists(eq(PATH));
        Mockito.doReturn(new byte[]{0, 1, 2, 3}).when(curator).getNode(eq(PATH));

        softly.assertThat(lockManager.canProceed(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isFalse();

        Mockito.verify(curator).isNodeExists(eq(PATH));
        Mockito.verify(curator).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).createNode(eq(PATH), eq(LOCAL_ADDRESS), eq(CreateMode.EPHEMERAL));
    }


    @Test
    public void testLockFailed() {
        Mockito.doThrow(new CuratorException()).when(curator).isNodeExists(eq(PATH));

        softly.assertThat(lockManager.canProceed(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isFalse();

        Mockito.verify(curator).isNodeExists(eq(PATH));
        Mockito.verify(curator, Mockito.never()).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).createNode(eq(PATH), eq(LOCAL_ADDRESS), eq(CreateMode.EPHEMERAL));
    }

    @Test
    void testUnlockSuccess() {
        long currentSession = 1L;
        Mockito.doReturn(Optional.of(currentSession)).when(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.doReturn(currentSession).when(curator).getCurrentSessionId();
        Mockito.doReturn(LOCAL_ADDRESS).when(curator).getNode(eq(PATH));

        softly.assertThat(lockManager.unlock(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isTrue();

        Mockito.verify(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.verify(curator).getCurrentSessionId();
        Mockito.verify(curator).getNode(eq(PATH));
        Mockito.verify(curator).dropNode(eq(PATH));
    }

    @Test
    void testUnlockNotLocked() {
        Mockito.doReturn(Optional.empty()).when(curator).getNodeOwnerSessionId(eq(PATH));

        softly.assertThat(lockManager.unlock(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isTrue();

        Mockito.verify(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.verify(curator, Mockito.never()).getCurrentSessionId();
        Mockito.verify(curator, Mockito.never()).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).dropNode(eq(PATH));
    }

    @Test
    void testUnlockNotherHost() {
        long currentSession = 1L;
        Mockito.doReturn(Optional.of(currentSession)).when(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.doReturn(currentSession).when(curator).getCurrentSessionId();
        Mockito.doReturn(new byte[]{0, 1, 2, 3}).when(curator).getNode(eq(PATH));

        softly.assertThat(lockManager.unlock(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isFalse();

        Mockito.verify(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.verify(curator).getCurrentSessionId();
        Mockito.verify(curator).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).dropNode(eq(PATH));
    }

    @Test
    void testUnlockNotOwned() {
        long currentSession = 1L;
        Mockito.doReturn(Optional.of(2L)).when(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.doReturn(currentSession).when(curator).getCurrentSessionId();

        softly.assertThat(lockManager.unlock(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isFalse();

        Mockito.verify(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.verify(curator).getCurrentSessionId();
        Mockito.verify(curator, Mockito.never()).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).dropNode(eq(PATH));
    }

    @Test
    void testUnlockFailedToCheckLock() {
        Mockito.doThrow(new CuratorException()).when(curator).getNodeOwnerSessionId(eq(PATH));

        softly.assertThat(lockManager.unlock(LockManager.Lock.DEFAULT, TEST_LOCK_SUB_PATH)).isFalse();

        Mockito.verify(curator).getNodeOwnerSessionId(eq(PATH));
        Mockito.verify(curator, Mockito.never()).getCurrentSessionId();
        Mockito.verify(curator, Mockito.never()).getNode(eq(PATH));
        Mockito.verify(curator, Mockito.never()).dropNode(eq(PATH));
    }
}
