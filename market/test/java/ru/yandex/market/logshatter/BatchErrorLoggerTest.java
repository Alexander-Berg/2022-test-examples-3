package ru.yandex.market.logshatter;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.logshatter.logging.BatchErrorLogger;
import ru.yandex.market.logshatter.logging.ErrorLogger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 11.10.16
 */
public class BatchErrorLoggerTest {
    public static final int UNREACHABLE_THRESHOLD = 100500;
    private ErrorLogger logger;

    @Before
    public void setUp() throws Exception {
        logger = mock(ErrorLogger.class);
    }

    @Test
    public void logErrors_groupSame_onFirstThreshold() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, 0, UNREACHABLE_THRESHOLD);

        IllegalStateException firstException = new IllegalStateException();
        sut.addError(firstException, "");
        sut.addError(new IllegalStateException(), "");
        sut.addError(new IllegalStateException(), "");

        sut.batchParsed();

        verify(logger, times(1)).logErrorGroup(any(), eq(3));
    }

    @Test
    public void logErrors_groupSeveralTypes() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, 0, UNREACHABLE_THRESHOLD);

        Exception firstException = new Exception();
        IllegalStateException firstIllegalStateException = new IllegalStateException();
        IllegalArgumentException firstIllegalArgumentException = new IllegalArgumentException();

        sut.addError(firstException, "");
        sut.addError(new Exception(), "");
        sut.addError(new Exception(), "");
        sut.addError(firstIllegalStateException, "");
        sut.addError(new IllegalStateException(), "");
        sut.addError(firstIllegalArgumentException, "");
        sut.addError(new Exception(), "");

        sut.batchParsed();

        verify(logger, times(3)).logErrorGroup(any(), anyInt());
    }

    @Test
    public void logErrors_logEveryError_beforeAnyThreshold() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, UNREACHABLE_THRESHOLD, UNREACHABLE_THRESHOLD);

        sut.addError(new IllegalStateException(), "");
        sut.addError(new IllegalStateException(), "");
        sut.addError(new IllegalStateException(), "");

        sut.batchParsed();

        verify(logger, times(3)).logSingleError(any());
    }

    @Test
    public void logErrors_logEveryError_logOnce() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, UNREACHABLE_THRESHOLD, UNREACHABLE_THRESHOLD);

        sut.addError(new IllegalStateException(), "");

        sut.batchParsed();
        verify(logger, times(1)).logSingleError(any());
        reset(logger);

        sut.batchParsed();
        verifyZeroInteractions(logger);
    }

    @Test
    public void logErrors_groupSame_logOnce() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, 0, UNREACHABLE_THRESHOLD);

        sut.addError(new IllegalStateException(), "");
        sut.addError(new IllegalStateException(), "");
        sut.batchParsed();
        verify(logger, times(1)).logErrorGroup(any(), anyInt());
        reset(logger);

        sut.batchParsed();
        verifyZeroInteractions(logger);
    }

    @Test
    public void logErrors_logarithmicLogging_onSecondThreshold() throws Exception {
        BatchErrorLogger sut = new BatchErrorLogger(logger, 1, 2);

        sut.addError(new IllegalStateException(), "");
        sut.batchParsed();

        // первый предел
        sut.addError(new IllegalStateException(), "");
        sut.batchParsed();

        reset(logger);

        // второй предел
        sut.addError(new IllegalStateException(), "");
        sut.batchParsed(); // 1
        verify(logger, times(1)).logErrorGroupWithoutStacktrace(any(), anyInt());
        reset(logger);

        sut.addError(new IllegalStateException(), "");
        sut.batchParsed(); // 2
        verify(logger, times(1)).logErrorGroupWithoutStacktrace(any(), anyInt());
        reset(logger);

        sut.addError(new IllegalStateException(), "");
        sut.batchParsed(); // 3
        verify(logger, never()).logErrorGroupWithoutStacktrace(any(), anyInt());
        reset(logger);

        sut.addError(new IllegalStateException(), "");
        sut.batchParsed(); // 4
        verify(logger, times(1)).logErrorGroupWithoutStacktrace(any(), anyInt());
        reset(logger);

        for (int i = 5; i < 8; ++i) {
            sut.addError(new IllegalStateException(), "");
            sut.batchParsed();
            verify(logger, never()).logErrorGroupWithoutStacktrace(any(), anyInt());
            reset(logger);
        }

        sut.addError(new IllegalStateException(), "");
        sut.batchParsed(); // 8
        verify(logger, times(1)).logErrorGroupWithoutStacktrace(any(), anyInt());
        reset(logger);
    }
}
