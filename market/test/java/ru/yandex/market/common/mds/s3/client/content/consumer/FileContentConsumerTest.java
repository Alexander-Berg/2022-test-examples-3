package ru.yandex.market.common.mds.s3.client.content.consumer;

import java.io.File;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;
import ru.yandex.market.common.mds.s3.client.util.TempFileUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link FileContentConsumer}.
 *
 * @author Vladislav Bauer
 */
public class FileContentConsumerTest {

    @Test
    public void testConsumer() {
        final File tempFile = TempFileUtils.createTempFile();
        final FileContentConsumer consumer = new FileContentConsumer(tempFile);

        checkConsumer(tempFile, consumer);
    }

    @Test(expected = MdsS3Exception.class)
    public void testConsumerNegative() {
        final File tempFile = TempFileUtils.createTempFile();
        try {
            final FileContentConsumer consumer = new FileContentConsumer(tempFile);
            final File result = consumer.consume(InOutUtils.badInputStream());

            fail(String.valueOf(result));
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }


    public static void checkConsumer(@Nonnull final File file, @Nonnull final FileContentConsumer consumer) {
        try {
            final File result = consumer.consume(InOutUtils.inputStream());
            final String actual = InOutUtils.readFile(result);

            assertThat(consumer.getFile(), equalTo(file));
            assertThat(actual, equalTo(TEST_DATA));
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

}
