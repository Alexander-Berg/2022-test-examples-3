package ru.yandex.data.compressor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.base64.Base64;
import ru.yandex.base64.Base64Encoder;
import ru.yandex.test.util.TestBase;

public class DataCompressorTest extends TestBase {
    private static final String SOME_TEXT = "Система использует динамические таблицы YT в качестве хранилища "
        + "метаданных, из чего проистекают определенные возможности и ограничения.\n"
        + "Изменения в метаданных транзакционны. Клиент системы может либо явно управлять транзакциями (начать, "
        + "произвести набор изменений, а затем закоммитить), либо, если атомарность не требуется, дать системе "
        + "самостоятельно заворачивать каждый запрос в отдельную транзакцию.\n"
        + "Транзакции работают на оси времени, точки на которой называются timestamps. У каждой транзакции есть момент "
        + "начала (start_timestamp), внутри транзакции состояние метаданных видится ровно таким, каким оно было в "
        + "момент начала. При коммите все изменения атомарно применяются в момент commit_timestamp.\n"
        + "При чтении пользователь может получить консистентный слепок состояния метаданных, указав нужный timestamp. "
        + "Читать глубоко в прошлое нельзя, т.к. система производит периодический compaction метаданных. В случае "
        + "нарушения возможных границ чтения система сообщит об ошибке.";
    private static final String DELIVERY_LOG1_LZO_BASE42URL_PATH = "delivery_log1.lzo.base64url";
    private static final String DELIVERY_LOG1_PATH = "delivery_log1.json";

    public DataCompressorTest() {
    }

    @Test
    public void testBase64CodeDecode() throws Exception {
        Base64Encoder encoder = new Base64Encoder(Base64.INSTANCE);
        encoder.process(SOME_TEXT.getBytes(StandardCharsets.UTF_8));
        final String encoded = encoder.toString();
        Assert.assertNotNull(encoded);
        final String decoded = new String(DataCompressor.LZO.base64DecodeUneven(encoded), StandardCharsets.UTF_8);
        Assert.assertEquals(SOME_TEXT, decoded);
    }

    @Test
    public void testLzoCompressDecompress() throws Exception {
        final byte[] compressed = DataCompressor.LZO.compress(SOME_TEXT.getBytes(StandardCharsets.UTF_8));
        Assert.assertNotNull(compressed);
        logger.info("Initial size: " + SOME_TEXT.length() + ", compressed size: " + compressed.length);
        final String uncompressed = new String(DataCompressor.LZO.uncompress(compressed), StandardCharsets.UTF_8);
        Assert.assertEquals(SOME_TEXT, uncompressed);
    }

    @Test
    public void testLzoCompressDecompressBase64() throws Exception {
        final String compressed = DataCompressor.LZO.compressAndBase64(SOME_TEXT);
        Assert.assertNotNull(compressed);
        logger.info("Initial size: " + SOME_TEXT.length() + ", compressed size: " + compressed.length());
        final String uncompressed = DataCompressor.LZO.unbase64AndUncompress(compressed);
        Assert.assertEquals(SOME_TEXT, uncompressed);
    }

    @Test
    public void testLzoCompressDecompressBase64UrlSafe() throws Exception {
        final String compressed = DataCompressor.LZO.compressAndBase64(SOME_TEXT, Base64.URL);
        Assert.assertNotNull(compressed);
        logger.info("Initial size: " + SOME_TEXT.length() + ", compressed size: " + compressed.length());
        logger.info("Compressed data: " + compressed);
        final String uncompressed = DataCompressor.LZO.unbase64AndUncompress(compressed, Base64.URL);
        Assert.assertEquals(SOME_TEXT, uncompressed);
    }

    @Test
    public void testLzoDecompressBase64UrlSafeDeliveryLog1() throws Exception {
        String compressedData = fileToString(DELIVERY_LOG1_LZO_BASE42URL_PATH);
        String uncompressedData = fileToString(DELIVERY_LOG1_PATH);
        Assert.assertNotNull(compressedData);
        logger.info("Compressed data length = " + compressedData.length());
        final String uncompressed = DataCompressor.LZO.unbase64AndUncompress(compressedData, Base64.URL);
        logger.info("Uncompressed data: " + uncompressed);
        Assert.assertNotNull(uncompressed);
        Assert.assertEquals(uncompressedData, uncompressed.trim());
    }

    protected String fileToString(final String fileName) throws Exception {
        Path path = Paths.get(getClass().getResource(fileName).toURI());
        return Files.readString(path, Charset.defaultCharset()).trim();
    }
}
