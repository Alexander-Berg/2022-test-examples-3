package ru.yandex.market.logshatter.reader.logbroker;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.Session.Partition;
import ru.yandex.market.logshatter.reader.logbroker.topic.CompressionRatioCalculator;

import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.configThatMatchesEverything;
import static ru.yandex.market.logshatter.reader.logbroker.LbReadingTester.messageData;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 23.01.2019
 */
public class LogBrokerReaderServiceCompressionRatioTest {
    @Rule
    public final Timeout timeout = new Timeout(10, TimeUnit.SECONDS);

    private final LbReadingTester tester = new LbReadingTester();

    // Куча нулей
    private static final byte[] UNCOMPRESSED_DATA =
        new byte[CompressionRatioCalculator.MIN_COMPRESSED_SIZE_FOR_PRECISE_COMPRESSION_RATIO_CALCULATION_BYTES + 1];

    // Куча нулей, сжатых gzip'ом
    private static final byte[] COMPRESSED_GZIP_DATA = CompressionCodec.GZIP.compressData(UNCOMPRESSED_DATA);

    // Куча нулей, сжатых zstd
    private static final byte[] COMPRESSED_ZSTD_DATA = CompressionCodec.ZSTD.compressData(UNCOMPRESSED_DATA);

    @Test
    public void compressionGZIPRatio() {
        compressionRatio(COMPRESSED_GZIP_DATA, CompressionCodec.GZIP);
    }

    @Test
    public void compressionZSTDRatio() {
        compressionRatio(COMPRESSED_ZSTD_DATA, CompressionCodec.ZSTD);
    }

    private void compressionRatio(byte[] compressedData, CompressionCodec codec) {
        Partition partition = tester.givenStartedSessionWithLockedPartition(configThatMatchesEverything());

        // Присылаем большую несжатую пачку данных чтобы было достаточно данных для подсчёта коэффициента сжатия
        partition.lbSendsData(1, messageData(1, CompressionCodec.RAW, UNCOMPRESSED_DATA));

        // Проверяем что пока данных мало, используем коэффициент сжатия по умолчанию
        tester.verifyQueueSizeIncreasedBy(
            (long) CompressionRatioCalculator.DEFAULT_COMPRESSION_RATIO * UNCOMPRESSED_DATA.length
        );

        // Запускаем парсеры чтобы данные распаковались и обновился коэффициент сжатия
        tester.runParsers();


        // Присылаем маленькую хорошо сжатую пачку данных
        partition.lbSendsData(2, messageData(2, codec, compressedData));

        // Проверяем что коэффициент сжатия 1, потому что до этого мы добавили кучу несжатых данных
        tester.verifyQueueSizeIncreasedBy(compressedData.length);

        // Запускаем парсеры чтобы данные распаковались и обновился коэффициент сжатия
        tester.runParsers();


        // Присылаем маленькую хорошо сжатую пачку данных ещё раз
        partition.lbSendsData(3, messageData(3, codec, compressedData));

        // Проверяем что коэффициент сжатия рассчитан на основе первых двух пачек данных
        tester.verifyQueueSizeIncreasedBy(
            (2.0 * UNCOMPRESSED_DATA.length) /
                (UNCOMPRESSED_DATA.length + compressedData.length) *
                compressedData.length,
            10
        );
    }
}
