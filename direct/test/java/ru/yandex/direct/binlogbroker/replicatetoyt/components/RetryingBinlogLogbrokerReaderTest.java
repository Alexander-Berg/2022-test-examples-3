package ru.yandex.direct.binlogbroker.replicatetoyt.components;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.binlogbroker.logbroker_utils.models.BinlogEventWithOffset;
import ru.yandex.direct.binlogbroker.logbroker_utils.reader.LogbrokerBatchReader;
import ru.yandex.direct.binlogbroker.logbroker_utils.reader.LogbrokerCommitState;
import ru.yandex.direct.binlogbroker.logbroker_utils.reader.LogbrokerReaderException;
import ru.yandex.direct.binlogbroker.logbroker_utils.reader.RetryingLogbrokerBatchReader;
import ru.yandex.direct.utils.Interrupts;

@ParametersAreNonnullByDefault
public class RetryingBinlogLogbrokerReaderTest {
    private static final int RETRY = 11;
    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void checkReaderFailing() {
        FailingEventReader failingEventReader = new FailingEventReader();

        AtomicInteger instantiaterCounter = new AtomicInteger(0);  // Just a counter.
        RetryingLogbrokerBatchReader<BinlogEventWithOffset> reader = new RetryingLogbrokerBatchReader<>(() -> {
            instantiaterCounter.incrementAndGet(); // Count "instantiations"
            return failingEventReader;
        }, RETRY, 1);

        softly.assertThatCode(
                () -> {
                    reader.fetchEvents((Interrupts.InterruptibleConsumer<List<BinlogEventWithOffset>>) (a) -> {
                        throw new RuntimeException("Shouldn't happen");
                    });
                }
        ).isInstanceOf(LogbrokerReaderException.class);
        softly.assertThat(failingEventReader.counter).isEqualTo(RETRY);
        softly.assertThat(failingEventReader.closeCounter).isEqualTo(RETRY);
        softly.assertThat(instantiaterCounter.get()).isEqualTo(RETRY);
    }

    class FailingEventReader implements LogbrokerBatchReader<BinlogEventWithOffset> {
        int counter = 0;
        int closeCounter = 0;

        @Override
        public void fetchEvents(
                Interrupts.InterruptibleFunction<List<BinlogEventWithOffset>, LogbrokerCommitState> eventsConsumer) {
            ++counter;
            throw new LogbrokerReaderException(new RuntimeException());
        }

        @Override
        public void close() {
            ++closeCounter;
        }
    }
}
