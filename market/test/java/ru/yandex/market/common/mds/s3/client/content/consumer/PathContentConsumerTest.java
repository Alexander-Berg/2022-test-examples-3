package ru.yandex.market.common.mds.s3.client.content.consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link PathContentConsumer}.
 *
 * @author Vladislav Bauer
 */
public class PathContentConsumerTest {

    @Test
    public void testConsumer() throws Exception {
        final Path tempPath = createTempPath();
        final PathContentConsumer consumer = new PathContentConsumer(tempPath);

        checkConsumer(tempPath, consumer);
    }

    @Test(expected = MdsS3Exception.class)
    public void testConsumerNegative() throws Exception {
        final Path tempPath = createTempPath();

        try {
            final PathContentConsumer consumer = new PathContentConsumer(tempPath);
            final Path result = consumer.consume(InOutUtils.badInputStream());

            fail(String.valueOf(result));
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }


    public static void checkConsumer(@Nonnull final Path path, @Nonnull final PathContentConsumer consumer) {
        try {
            final Path result = consumer.consume(InOutUtils.inputStream());
            final String actual = InOutUtils.readPath(result);

            assertThat(consumer.getPath(), equalTo(path));
            assertThat(actual, equalTo(TEST_DATA));
        } finally {
            try {
                Files.deleteIfExists(path);
            } catch (final Exception ignored) {
            }
        }
    }


    private Path createTempPath() throws IOException {
        final String prefix = RandUtils.randomText();
        final String postfix = String.valueOf(System.currentTimeMillis());

        return Files.createTempFile(prefix, postfix);
    }

}
