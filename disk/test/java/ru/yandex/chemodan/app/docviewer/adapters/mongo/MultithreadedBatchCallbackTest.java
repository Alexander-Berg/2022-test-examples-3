package ru.yandex.chemodan.app.docviewer.adapters.mongo;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.function.Function1V;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.thread.ThreadUtils;
import ru.yandex.misc.time.Stopwatch;

/**
 * @author ssytnik
 */
public class MultithreadedBatchCallbackTest {

    @Test
    public void waitUntilComplete() {

        class MyTask {
            public boolean done = false;

            void execute() {
                ThreadUtils.sleep(500);
                done = true;
            }
        }

        Function1V<MyTask> callback = MyTask::execute;

        ListF<MyTask> tasks = Cf.list(new MyTask(), new MyTask(), new MyTask(), new MyTask());

        try (MultithreadedBatchCallback<MyTask> batchCallback = new MultithreadedBatchCallback<>(callback, 2)) {
            Stopwatch watch = Stopwatch.createAndStart();
            batchCallback.execute(tasks);
            watch.stop();

            Assert.gt(watch.millisDuration(), 950L);
            Assert.lt(watch.millisDuration(), 1950L);

            for (MyTask task : tasks) {
                Assert.isTrue(task.done);
            }

        }
    }

}
