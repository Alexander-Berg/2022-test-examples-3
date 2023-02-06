package ru.yandex.direct.jobs.adfox.messaging;

import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.juggler.JugglerStatus;

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.common.db.PpcPropertyNames.ADFOX_INPUT_PROCESSING_ENABLED;
import static ru.yandex.direct.jobs.adfox.messaging.ProcessIncomingAdfoxMessagesJob.PpcPropertyKey;
import static ru.yandex.direct.jobs.adfox.messaging.ProcessIncomingAdfoxMessagesJobTest.JugglerStatusTestCase.given;

@Disabled("Не работающая фича 'частные сделки'")
@ParametersAreNonnullByDefault
class ProcessIncomingAdfoxMessagesJobTest {

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DirectConfig config;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @InjectMocks
    private ProcessIncomingAdfoxMessagesJob job;

    @BeforeEach
    void init() {
        initMocks(this);
        job = spy(job);
        setProp(ADFOX_INPUT_PROCESSING_ENABLED, true);
        @SuppressWarnings("unchecked")
        PpcProperty<Boolean> property = mock(PpcProperty.class);
        when(property.getOrDefault(false)).thenReturn(true);
        when(ppcPropertiesSupport.get(eq(ADFOX_INPUT_PROCESSING_ENABLED))).thenReturn(property);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("juggler")
    void checkJugglerStatus(JugglerStatusTestCase testCase) throws Exception {
        letProcessingResultBe(testCase.processingResult);
        setProp(PpcPropertyKey.ADFOX_INPUT_ERR_LAST, testCase.lastError);
        setProp(PpcPropertyKey.ADFOX_INPUT_ERR_REVIEWED, testCase.lastReviewed);

        job.execute();

        if (testCase.expectedStatus == JugglerStatus.OK) {
            verify(job, never()).setJugglerStatus(any(), any());
        } else {
            verify(job, times(1)).setJugglerStatus(eq(testCase.expectedStatus), anyString());
        }
    }

    static Iterable<JugglerStatusTestCase> juggler() {
        LocalDateTime before = LocalDateTime.parse("2018-05-05T10:15:30", ISO_LOCAL_DATE_TIME);
        LocalDateTime after = LocalDateTime.parse("2018-05-05T10:16:00", ISO_LOCAL_DATE_TIME);
        return asList(
                given(QueueProcessor.Result.SUCCESS)
                        .expect(JugglerStatus.OK),

                given(QueueProcessor.Result.ERRORS)
                        .expect(JugglerStatus.WARN),

                given(QueueProcessor.Result.SUCCESS).lastError(before)
                        .expect(JugglerStatus.WARN),

                given(QueueProcessor.Result.SUCCESS).lastError(before).lastReviewed(after)
                        .expect(JugglerStatus.OK),

                given(QueueProcessor.Result.SUCCESS).lastError(after).lastReviewed(before)
                        .expect(JugglerStatus.WARN)
        );
    }

    @ParameterizedTest(name = "given {0}, expect updated: {1}")
    @MethodSource("propUpdate")
    void checkPropUpdated(QueueProcessor.Result given, boolean shouldUpdate) throws Exception {
        letProcessingResultBe(given);
        @SuppressWarnings("unchecked")
        PpcProperty<LocalDateTime> property = mock(PpcProperty.class);
        when(ppcPropertiesSupport.get(eq(PpcPropertyKey.ADFOX_INPUT_ERR_LAST))).thenReturn(property);

        job.execute();

        verify(property, times(shouldUpdate ? 1 : 0))
                .set(any());
    }

    static Iterable<Object[]> propUpdate() {
        return asList(new Object[][]{
                {QueueProcessor.Result.SUCCESS, false},
                {QueueProcessor.Result.ERRORS, true},
        });
    }

    private <T> void setProp(PpcPropertyName<T> key, @Nullable T val) {
        @SuppressWarnings("unchecked")
        PpcProperty<T> property = mock(PpcProperty.class);
        when(property.get()).thenReturn(val);
        when(ppcPropertiesSupport.get(eq(key))).thenReturn(property);
    }

    private void letProcessingResultBe(QueueProcessor.Result result) {
        doReturn(result).when(job).runRpcCommand(any());
    }

    static class JugglerStatusTestCase {
        QueueProcessor.Result processingResult;
        LocalDateTime lastError, lastReviewed;
        JugglerStatus expectedStatus = JugglerStatus.OK;

        static JugglerStatusTestCase given(QueueProcessor.Result processingResult) {
            return new JugglerStatusTestCase(processingResult);
        }

        JugglerStatusTestCase(QueueProcessor.Result processingResult) {
            this.processingResult = processingResult;
        }

        JugglerStatusTestCase lastError(LocalDateTime dateTime) {
            this.lastError = dateTime;
            return this;
        }

        JugglerStatusTestCase lastReviewed(LocalDateTime dateTime) {
            this.lastReviewed = dateTime;
            return this;
        }

        JugglerStatusTestCase expect(JugglerStatus status) {
            this.expectedStatus = status;
            return this;
        }

        @Override
        public String toString() {
            return format("Given %s, expect %s; dates: [%s; %s]",
                    processingResult, expectedStatus, lastError, lastReviewed);
        }
    }
}
