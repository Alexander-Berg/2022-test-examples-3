package ru.yandex.crypta.graph2.workflow;

import java.util.HashSet;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.Dao;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkippableTaskTest {

    public static class TestTask extends SkippableTask<Object, ListF<YPath>, Object> {
        public boolean checkRun = false;

        public TestTask(Dao dao, YPath workdir, String nameSuffix, DestinationsInspector inspector) {
            super(dao, workdir, null, nameSuffix, inspector);
            checkRun = false;
        }

        @Override
        protected void runImpl(Object o) {
            checkRun = true;
        }

        @Override
        public ListF<YPath> getOutput() {
            return Cf.list(workdir.child("output"));
        }

        @Override
        public String getDescription() {
            return null;
        }
    };

    @Test
    public void testEmptySkippableTask() {
        DestinationsInspector inspector = new DestinationsInspector() {
            private HashSet<YPath> paths = new HashSet<>();
            @Override
            public void markDestination(Dao dao, YPath path) {
                paths.add(path);
            }

            @Override
            public boolean isReady(Dao dao, YPath path) {
                return paths.contains(path);
            }
        };
        TestTask task = new TestTask(null, YPath.simple("//workdir"), "test", inspector);

        assertFalse(task.canSkipRunPhase());
        task.run(null);
        assertTrue(task.canSkipRunPhase());
        assertTrue(task.checkRun);

        task.checkRun = false;
        task.run(null);
        assertFalse(task.checkRun);
    }
}
