package ru.yandex.chemodan.app.djfs.core.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.chemodan.queller.celery.job.CeleryJob;
import ru.yandex.chemodan.queller.worker.CeleryOnetimeTask;
import ru.yandex.chemodan.queller.worker.CeleryTaskManager;
import ru.yandex.commune.bazinga.impl.FullJobId;

/**
 * @author eoshch
 */
@Configuration
public class MockCeleryTaskManagerContextConfiguration {
    @Bean
    public MockCeleryTaskManager mockCeleryTaskManager() {
        return new MockCeleryTaskManager();
    }

    public static class MockCeleryTaskManager extends CeleryTaskManager {
        public final ListF<CeleryJob> submitted = Cf.arrayList();

        private MockCeleryTaskManager() {
            super(null);
        }

        @Override
        public FullJobId submit(CeleryOnetimeTask task) {
            throw new NotImplementedException();
        }

        @Override
        public void submit(CeleryJob celeryJob) {
            submitted.add(celeryJob);
        }

        @Override
        public void submit(CeleryJob celeryJob, boolean confirmed) {
            submit(celeryJob);
        }
    }
}
