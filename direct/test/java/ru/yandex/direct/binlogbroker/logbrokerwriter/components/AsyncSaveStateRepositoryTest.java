package ru.yandex.direct.binlogbroker.logbrokerwriter.components;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlogbroker.logbroker_utils.models.SourceType;
import ru.yandex.direct.binlogbroker.logbrokerwriter.models.ImmutableSourceState;
import ru.yandex.direct.test.utils.TestUtils;

import static java.time.Duration.ofMinutes;
import static ru.yandex.direct.env.EnvironmentType.DEVTEST;

@ParametersAreNonnullByDefault
public class AsyncSaveStateRepositoryTest {
    private static final SourceType SOURCE_TYPE = SourceType.fromType(DEVTEST, "ppc:1");
    private static final String MESSAGE = TestUtils.randomName("Oops ", 16);
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();
    private ConcurrentLinkedQueue<ImmutableSourceState> writtenStates = new ConcurrentLinkedQueue<>();
    private int throwAtAttempt;
    private int counter;
    private SourceStateRepository sourceStateRepository = new SourceStateRepository() {

        @Override
        public ImmutableSourceState loadState(SourceType source) {
            throw new IllegalStateException();
        }

        @Override
        public void saveState(SourceType source, ImmutableSourceState immutableSourceState) {
            if (++counter == throwAtAttempt) {
                throw new IllegalStateException(MESSAGE + " " + counter);
            }
            writtenStates.add(immutableSourceState);
        }

        @Override
        public void close() {
            // nothing
        }
    };

    @Before
    public void setUp() {
        throwAtAttempt = counter = 0;
    }

    /**
     * Всё, что было записано через {@link AsyncSaveStateRepository}, должно быть записано в реальности.
     */
    @Test
    public void successfulWrite() {
        ImmutableSourceState state1 =
                new ImmutableSourceState(1L, 123L, "1", 0, new byte[0]);
        ImmutableSourceState state2 =
                new ImmutableSourceState(2L, 124L, "2", 0, new byte[0]);
        ImmutableSourceState state3 =
                new ImmutableSourceState(3L, 125L, "3", 0, new byte[0]);

        try (AsyncSaveStateRepository asyncSaveStateRepository =
                     new AsyncSaveStateRepository(sourceStateRepository, ofMinutes(2))) {
            asyncSaveStateRepository.saveState(SOURCE_TYPE, state1);
            asyncSaveStateRepository.saveState(SOURCE_TYPE, state2);

            softly.assertThat(ImmutableList.copyOf(writtenStates))
                    .describedAs("state1 assuredly written before start of writing state2")
                    .contains(state1);

            asyncSaveStateRepository.saveState(SOURCE_TYPE, state3);

            softly.assertThat(ImmutableList.copyOf(writtenStates))
                    .describedAs("state2 assuredly written before start of writing state3")
                    .contains(state1);
        }

        softly.assertThat(ImmutableList.copyOf(writtenStates))
                .describedAs("All states written in right order")
                .containsExactly(state1, state2, state3);
    }

    /**
     * Ошибки не должны проглатываться и должны перебрасываться при первой возможности.
     */
    @Test
    public void rethrowErrors() {
        ImmutableSourceState state1 =
                new ImmutableSourceState(1L, 123L, "1", 0, new byte[0]);
        ImmutableSourceState state2 =
                new ImmutableSourceState(2L, 124L, "2", 0, new byte[0]);
        ImmutableSourceState state3 =
                new ImmutableSourceState(3L, 125L, "3", 0, new byte[0]);

        throwAtAttempt = 2;

        try (AsyncSaveStateRepository asyncSaveStateRepository = new AsyncSaveStateRepository(sourceStateRepository,
                ofMinutes(2))) {
            softly.assertThatCode(() -> asyncSaveStateRepository.saveState(SOURCE_TYPE, state1))
                    .describedAs("Successful save state1")
                    .doesNotThrowAnyException();
            softly.assertThatCode(() -> asyncSaveStateRepository.saveState(SOURCE_TYPE, state2))
                    .describedAs("Successful save state2 (error will be throwed in another thread)")
                    .doesNotThrowAnyException();
            softly.assertThatCode(() -> asyncSaveStateRepository.saveState(SOURCE_TYPE, state3))
                    .describedAs("Rethrow error from previous asynchronous save")
                    .hasMessageContaining(MESSAGE + " " + 2);
        }

        softly.assertThat(ImmutableList.copyOf(writtenStates))
                .describedAs("State that should be saved after erroneous state was not saved")
                .containsExactly(state1);
    }
}
