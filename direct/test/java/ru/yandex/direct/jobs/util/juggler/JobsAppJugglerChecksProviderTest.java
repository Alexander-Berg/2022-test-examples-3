package ru.yandex.direct.jobs.util.juggler;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.ansiblejuggler.PlaybookBuilder;
import ru.yandex.direct.jobs.util.juggler.checkinfo.JobWorkingCheckInfo;
import ru.yandex.direct.juggler.JugglerEvent;
import ru.yandex.direct.juggler.JugglerStatus;
import ru.yandex.direct.juggler.check.checkinfo.NumericCheckInfo;
import ru.yandex.direct.scheduler.support.DirectJob;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

class JobsAppJugglerChecksProviderTest {
    private static final String TEST_SERVICE = "service.test.mine";
    private static final String TEST_MESSAGE = "Some message";
    private static final Integer TEST_SHARD = 4;

    private static class TestClassOne extends DirectJob {
        @Override
        public void execute() {

        }
    }

    private static class TestClassTwo extends DirectShardedJob {
        @Override
        public void execute() {

        }

        @Override
        public int getShard() {
            return TEST_SHARD;
        }
    }

    private static class TestClassThree extends DirectJob {
        @Override
        public void execute() {

        }
    }

    private static class TestClassFour extends DirectJob {
        @Override
        public void execute() {

        }
    }

    @Mock
    private JobWorkingCheckInfo descriptionOne;

    @Mock
    private JobWorkingCheckInfo descriptionTwo;

    @Mock
    private JobWorkingCheckInfo descriptionThree;

    @Mock
    private NumericCheckInfo numericCheckInfo;

    private JobsAppJugglerChecksProvider provider;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        provider = new JobsAppJugglerChecksProvider(
                ImmutableMap.<Class, JobWorkingCheckInfo>builder()
                        .put(TestClassOne.class, descriptionOne)
                        .put(TestClassTwo.class, descriptionTwo)
                        .put(TestClassThree.class, descriptionThree)
                        .build(),
                Collections.singletonList(numericCheckInfo));
        for (JobWorkingCheckInfo info : new JobWorkingCheckInfo[]{descriptionOne, descriptionTwo, descriptionThree}) {
            doReturn(info.toString()).when(info).getServiceName();
        }
    }

    @Test
    void testGetEventNotInRegistry() {
        TestClassFour job = new TestClassFour();

        JugglerEvent event = provider.getEvent(job);
        assertThat(event)
                .isNull();
    }

    @Test
    void testGetEventNotSharded() {
        TestClassOne job = new TestClassOne();
        job.setJugglerStatus(JugglerStatus.CRIT, TEST_MESSAGE);

        doReturn(TEST_SERVICE)
                .when(descriptionOne).getServiceName();

        JugglerEvent event = provider.getEvent(job);
        assertThat(event)
                .is(matchedBy(beanDiffer(new JugglerEvent(TEST_SERVICE, JugglerStatus.CRIT, TEST_MESSAGE))));
    }

    @Test
    void testGetEventSharded() {
        TestClassTwo job = new TestClassTwo();
        job.setJugglerStatus(JugglerStatus.WARN, TEST_MESSAGE);

        doReturn(TEST_SERVICE)
                .when(descriptionTwo).shardedServiceName(eq(TEST_SHARD));

        JugglerEvent event = provider.getEvent(job);
        assertThat(event)
                .is(matchedBy(beanDiffer(new JugglerEvent(TEST_SERVICE, JugglerStatus.WARN, TEST_MESSAGE))));
    }

    @Test
    void testAddToPlaybook() {
        PlaybookBuilder builder = mock(PlaybookBuilder.class);
        provider.addChecks(builder);

        verify(descriptionOne).addCheckToPlaybook(eq(builder));
        verify(descriptionTwo).addCheckToPlaybook(eq(builder));
        verify(descriptionThree).addCheckToPlaybook(eq(builder));
        verify(numericCheckInfo).addCheckToPlaybook(eq(builder));
    }
}
