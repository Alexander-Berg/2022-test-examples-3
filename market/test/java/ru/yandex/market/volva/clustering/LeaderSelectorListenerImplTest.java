package ru.yandex.market.volva.clustering;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
public class LeaderSelectorListenerImplTest {


    @Test
    public void takeLeadership() throws Exception {
        TestStateListener nsl = new TestStateListener();
        LeaderSelectorListenerImpl aolsl = new LeaderSelectorListenerImpl(List.of(nsl));
        Thread t = new Thread(() -> {
            try {
                aolsl.takeLeadership(mock(CuratorFramework.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        assertThat(nsl.isRun()).isFalse();
        long timeout = System.currentTimeMillis() + 20_000;
        t.start();
        try {
            while (!nsl.isRun() && System.currentTimeMillis() < timeout) {
                Thread.sleep(50L);
            }
            assertThat(aolsl.isLeader()).isTrue();
            aolsl.dropLeadership();
            t.join();
            assertThat(nsl.isRun()).isFalse();
            assertThat(aolsl.isLeader()).isFalse();
        } catch (Throwable e) {
            throw e;
        } finally {
            aolsl.dropLeadership();
        }
    }

    private class TestStateListener implements NodeStateListener {
        private AtomicBoolean run = new AtomicBoolean(false);

        @Override
        public void onTakeLeadership() throws Exception {
            run.set(true);
        }

        @Override
        public void onReleaseLeadership() throws Exception {
            run.set(false);
        }

        public boolean isRun() {
            return run.get();
        }
    }

}
