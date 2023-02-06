package ru.yandex.chemodan.app.docviewer.utils.pdf.text;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.chemodan.app.docviewer.TestResources;
import ru.yandex.chemodan.app.docviewer.utils.pdf.PdfUtils;
import ru.yandex.misc.io.url.UrlInputStreamSource;
import ru.yandex.misc.test.Assert;

/**
 * @author vlsergey
 * @author akirakozov
 */
public class WordsPositionSerializerTest {

    private static final Logger logger = LoggerFactory.getLogger(WordsPositionSerializerTest.class);

    private static void assertBetween(int from, int value, int to) {
        Assert.le(from, value);
        Assert.le(value, to);
    }

    private Document getTestWordsPositions() {
        return PdfUtils.withExistingDocument(new UrlInputStreamSource(TestResources.Adobe_Acrobat_1_5_114p),
                true, PdfPageWordsExtractor.getDocumentWithExtractedWordsF());
    }

    @Test
    public void testDeserializeZip() {
        Document source = getTestWordsPositions();
        Document target = WordPositionSerializer.deserializeJsonCompressed(
                WordPositionSerializer.serializeJsonCompressed(source, 1));

        Assert.equals(source.getPages().size(), target.getPages().size());
        Assert.equals(source, target);
    }

    @Test
    public void testSizeZipCompressed1() {
        Document wordPositions = getTestWordsPositions();
        byte[] bs = WordPositionSerializer.serializeJsonCompressed(wordPositions, 1);
        logger.info("Size with ZIP compression: {} bytes", String.valueOf(bs.length));

        assertBetween(250 * 1024, bs.length, 400 * 1024);
    }

    @Test
    public void testSizeZipCompressed9() {
        Document wordPositions = getTestWordsPositions();
        byte[] bs = WordPositionSerializer.serializeJsonCompressed(wordPositions, 9);
        logger.info("Size with ZIP compression: {} bytes", String.valueOf(bs.length));

        assertBetween(150 * 1024, bs.length, 250 * 1024);
    }

}
