package ru.yandex.direct.binlogbroker.logbroker_utils.writer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.binlogbroker.logbroker_utils.writer.LogbrokerProducerTestUtils.createProducerSupplierWithCapturedSeqNo;

public class AbstractLogbrokerWriterImplTest {

    private static final Duration LOGBROKER_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(0);

    /**
     * Тест проверяет то, что если кастомный seqNo не указан, то записывается инкреминтирующийся initialSeqNo
     **/
    @Test
    public void testAutoincrementSeqNo() {
        int objectsToWriteCnt = 6;
        long initialSeqNo = 10;
        int repeatCnt = 3;
        ArgumentCaptor<Long> seqNoCapture = ArgumentCaptor.forClass(Long.class);
        LogbrokerWriterAutoincrementSeqNo logbrokerWriter =
                new LogbrokerWriterAutoincrementSeqNo(initialSeqNo, seqNoCapture);
        List<Object> objects =
                IntStream.range(0, objectsToWriteCnt).mapToObj(unsused -> new Object())
                        .collect(Collectors.toList());
        for (int i = 0; i < repeatCnt; i++) {
            logbrokerWriter.writeSync(objects);
        }

        Long[] expected =
                LongStream.rangeClosed(initialSeqNo + 1, initialSeqNo + repeatCnt * objectsToWriteCnt).boxed()
                        .toArray(Long[]::new);
        assertThat(seqNoCapture.getAllValues()).containsExactly(expected);
    }

    /**
     * Тест проверяет то, при неудачной записи и без использования кастомного seqNo,
     * при повторной попытке объекты запишутся с такими же seqNo
     **/
    @Test
    public void testAutoincrementSeqNo_FailureWrite() {
        /* Будет записываться пачка из objectsToWriteCnt объектов */
        int objectsToWriteCnt = 6;
        /* Первый элемент не запишется - переписывается вся пачка */
        int writeFailureCnt = 1;
        /* При инициализации новой сессии для повторной попытки должен вернуться такой же initialSeqNo,
        как и при первой попытке */
        long initialSeqNo = 10;

        /* Значения seqNo объектов при записи. Второй раз пачка должна записываться с такими же seqNo */
        List<Long> seqNoValues =
                LongStream.range(initialSeqNo + 1, initialSeqNo + 1 + objectsToWriteCnt).boxed().collect(
                        Collectors.toList());

        int retryCnt = 10;
        ArgumentCaptor<Long> seqNoCapture = ArgumentCaptor.forClass(Long.class);

        FailureLogbrokerWriterAutoincrementSeqNo logbrokerWriter =
                new FailureLogbrokerWriterAutoincrementSeqNo(initialSeqNo, writeFailureCnt, seqNoCapture, retryCnt);

        List<Object> objects =
                IntStream.range(0, objectsToWriteCnt).mapToObj(unsused -> new Object())
                        .collect(Collectors.toList());

        logbrokerWriter.writeSync(objects);

        List<Long> expected = new ArrayList<>();
        for (int i = 0; i < writeFailureCnt + 1; i++) {
            expected.addAll(seqNoValues);
        }

        assertThat(seqNoCapture.getAllValues()).containsExactly(expected.toArray(new Long[0]));
    }

    /**
     * Тест проверяет то, что если требуется записать кастомный seqNo, то initialSeqNo и его инкремент игнорируется
     **/
    @Test
    public void testCustomSeqNo() {
        int objectsToWriteCnt = 6;
        long initialSeqNo = 10;
        long customStartSeqNo = 20;
        int repeatCnt = 2;
        ArgumentCaptor<Long> seqNoCapture = ArgumentCaptor.forClass(Long.class);
        LogbrokerWriterCustomSeqNo logbrokerWriter =
                new LogbrokerWriterCustomSeqNo(initialSeqNo, customStartSeqNo, seqNoCapture);
        List<Object> objects =
                IntStream.range(0, objectsToWriteCnt).mapToObj(unused -> new Object())
                        .collect(Collectors.toList());
        for (int i = 0; i < repeatCnt; i++) {
            logbrokerWriter.writeSync(objects);
        }

        Long[] expected =
                LongStream.range(customStartSeqNo, customStartSeqNo + repeatCnt * objectsToWriteCnt).boxed()
                        .toArray(Long[]::new);
        assertThat(seqNoCapture.getAllValues()).containsExactly(expected);
    }

    @ParametersAreNonnullByDefault
    public class LogbrokerWriterAutoincrementSeqNo extends AbstractLogbrokerWriterImpl<Object> {

        LogbrokerWriterAutoincrementSeqNo(long initialSeqNo, ArgumentCaptor<Long> seqNoCapture) {
            super(createProducerSupplierWithCapturedSeqNo(initialSeqNo, seqNoCapture), LOGBROKER_TIMEOUT);
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object objects) {
            return new LogbrokerWriteRequest(new byte[0]);
        }
    }

    @ParametersAreNonnullByDefault
    public class LogbrokerWriterCustomSeqNo extends AbstractLogbrokerWriterImpl<Object> {

        long seqNo;

        LogbrokerWriterCustomSeqNo(long initialSeqNo, long startSeqNo, ArgumentCaptor<Long> seqNoCapture) {
            super(createProducerSupplierWithCapturedSeqNo(initialSeqNo, seqNoCapture), LOGBROKER_TIMEOUT);
            seqNo = startSeqNo;
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object objects) {
            return new LogbrokerWriteRequest(new byte[0], seqNo++);
        }
    }

    /**
     * Класс, для проверки того, что если пачка объектов записалась неудачно,
     * при повторной попытке она записется с первоначальными seqNo
     */
    @ParametersAreNonnullByDefault
    class FailureLogbrokerWriterAutoincrementSeqNo extends AbstractLogbrokerWriterImpl<Object> {
        FailureLogbrokerWriterAutoincrementSeqNo(long initialSeqNo, int writeFailureCnt,
                                                 ArgumentCaptor<Long> seqNoCapture, int retryCnt) {
            super(createProducerSupplierWithCapturedSeqNo(initialSeqNo, writeFailureCnt,
                    seqNoCapture), LOGBROKER_TIMEOUT, new LogbrokerWriterRetryConfig(retryCnt, RETRY_DELAY));
        }

        @Override
        public LogbrokerWriteRequest makeRequest(Object objects) {
            return new LogbrokerWriteRequest(new byte[0]);
        }
    }


}
