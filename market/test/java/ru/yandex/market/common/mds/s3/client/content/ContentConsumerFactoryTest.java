package ru.yandex.market.common.mds.s3.client.content;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.content.consumer.FileContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.FileContentConsumerTest;
import ru.yandex.market.common.mds.s3.client.content.consumer.PathContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.PathContentConsumerTest;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumerTest;
import ru.yandex.market.common.mds.s3.client.content.consumer.TextContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.TextContentConsumerTest;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;

/**
 * Unit-тесты для {@link ContentConsumerFactory}.
 *
 * @author Vladislav Bauer
 */
public class ContentConsumerFactoryTest {

    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(ContentConsumerFactory.class);
    }

    @Test
    public void testFile() {
        final File tempFile = TempFileUtils.createTempFile();
        final FileContentConsumer consumer = ContentConsumerFactory.file(tempFile);

        FileContentConsumerTest.checkConsumer(tempFile, consumer);
    }

    @Test
    public void testFileWithName() {
        final File tempFile = TempFileUtils.createTempFile();
        final String filePath = tempFile.getAbsolutePath();
        final FileContentConsumer consumer = ContentConsumerFactory.file(filePath);

        FileContentConsumerTest.checkConsumer(tempFile, consumer);
    }

    @Test
    public void testTempFile() {
        final FileContentConsumer consumer = ContentConsumerFactory.tempFile();

        checkFileConsumer(consumer);
    }

    @Test
    public void testTempFileWithPrefixAndSuffix() {
        final String prefix = RandUtils.randomText();
        final String suffix = RandUtils.randomText();
        final FileContentConsumer consumer = ContentConsumerFactory.tempFile(prefix, suffix);

        checkFileConsumer(consumer);
    }

    @Test
    public void testPath() {
        final File tempFile = TempFileUtils.createTempFile();
        final Path tempPath = tempFile.toPath();
        final PathContentConsumer consumer = ContentConsumerFactory.path(tempPath);

        PathContentConsumerTest.checkConsumer(tempPath, consumer);
    }

    @Test
    public void testPathWithName() {
        final File tempFile = TempFileUtils.createTempFile();
        final Path tempPath = tempFile.toPath();
        final String filePath = tempPath.toFile().getAbsolutePath();
        final PathContentConsumer consumer = ContentConsumerFactory.path(filePath);

        PathContentConsumerTest.checkConsumer(tempPath, consumer);
    }

    @Test
    public void testText() {
        final TextContentConsumer consumer = ContentConsumerFactory.text();

        TextContentConsumerTest.checkConsumer(consumer);
    }

    @Test
    public void testStreamCopy() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamCopyContentConsumer<ByteArrayOutputStream> consumer =
            ContentConsumerFactory.streamCopy(outputStream);

        StreamCopyContentConsumerTest.checkConsumer(outputStream, consumer);
    }


    private void checkFileConsumer(final FileContentConsumer consumer) {
        final File tempFile = consumer.getFile();
        FileContentConsumerTest.checkConsumer(tempFile, consumer);
    }

}
