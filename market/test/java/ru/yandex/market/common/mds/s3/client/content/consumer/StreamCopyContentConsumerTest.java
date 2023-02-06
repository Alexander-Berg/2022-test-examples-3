package ru.yandex.market.common.mds.s3.client.content.consumer;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link StreamCopyContentConsumer}.
 *
 * @author Vladislav Bauer
 */
public class StreamCopyContentConsumerTest {

    @Test
    public void testConsumer() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamCopyContentConsumer<ByteArrayOutputStream> consumer = new StreamCopyContentConsumer<>(outputStream);

        checkConsumer(outputStream, consumer);
    }

    @Test(expected = MdsS3Exception.class)
    public void testConsumerNegative() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamCopyContentConsumer<ByteArrayOutputStream> consumer = new StreamCopyContentConsumer<>(outputStream);

        consumer.consume(InOutUtils.badInputStream());
    }


    public static <T extends OutputStream> void checkConsumer(
        @Nonnull final ByteArrayOutputStream outputStream,
        @Nonnull final StreamCopyContentConsumer<T> consumer
    ) {
        final T result = consumer.consume(InOutUtils.inputStream());

        assertThat(result == outputStream, equalTo(true));
        assertThat(result.toString(), equalTo(TEST_DATA));
    }

}
