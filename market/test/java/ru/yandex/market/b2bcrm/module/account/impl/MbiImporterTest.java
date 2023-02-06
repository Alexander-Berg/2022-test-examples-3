package ru.yandex.market.b2bcrm.module.account.impl;

import java.util.Collection;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.b2bcrm.module.account.impl.step.AbstractMbiImportStep;
import ru.yandex.market.b2bcrm.module.account.step.MbiImportStep;
import ru.yandex.market.crm.util.Exceptions;
import ru.yandex.market.jmf.module.metric.MetricsService;
import ru.yandex.market.jmf.security.AuthRunnerService;
import ru.yandex.market.jmf.security.test.impl.MockAuthRunnerService;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class MbiImporterTest {

    @Spy
    private final AuthRunnerService runnerService = new MockAuthRunnerService();
    @Mock
    private MetricsService metricsService;
    private RetryableImportStep step1;
    private RetryableImportStep step2;
    private MbiImporter mbiImporter;

    @BeforeEach
    public void setUp() throws Exception {
        step1 = Mockito.spy(new RetryableImportStep(runnerService, metricsService, 2));
        step2 = Mockito.spy(new DependentImportStep(runnerService, metricsService, 1, step1.getClass()));
        mbiImporter = new MbiImporter(true, Set.of(step1, step2));
    }

    @Test
    public void stepsShouldRetryOnFail() {
        mbiImporter.doImport();
        Mockito.verify(step1, Mockito.times(2)).doImportInternal();
        Mockito.verify(step2, Mockito.times(1)).doImportInternal();
    }

    @Test
    public void stepsShouldRunInExactOrder() {
        mbiImporter.doImport();
        InOrder order = Mockito.inOrder(step1, step2);
        order.verify(step1, Mockito.times(2)).doImportInternal();
        order.verify(step2, Mockito.times(1)).doImportInternal();

    }

    @Test
    public void stepsShouldBeRunAsSuperUser() {
        mbiImporter.doImport();
        Mockito.verify(runnerService, Mockito.times(2)).runAsSuperUser(any(Exceptions.TrashRunnable.class));
    }

    private static class RetryableImportStep extends AbstractMbiImportStep {

        private final static Logger LOG = LoggerFactory.getLogger(RetryableImportStep.class);

        private int currentAttempt = 0;

        private RetryableImportStep(AuthRunnerService runnerService, MetricsService metricsService, int retryCount) {
            super(LOG, retryCount, null, "", runnerService, null, null, null, metricsService);
        }

        @Override
        public void doImportInternal() {
            if (currentAttempt++ < retryCount) {
                throw new RuntimeException();
            }
        }

    }

    public static class DependentImportStep extends RetryableImportStep {

        private final Class<? extends MbiImportStep> dependencyClass;

        private DependentImportStep(
                AuthRunnerService runnerService,
                MetricsService metricsService,
                int retryCount,
                Class<? extends MbiImportStep> dependencyClass
        ) {
            super(runnerService, metricsService, retryCount);
            this.dependencyClass = dependencyClass;
        }

        @Override
        public Collection<Class<? extends MbiImportStep>> dependsOn() {
            return Set.of(dependencyClass);
        }
    }
}
