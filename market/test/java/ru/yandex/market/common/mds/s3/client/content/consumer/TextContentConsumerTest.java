package ru.yandex.market.common.mds.s3.client.content.consumer;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link TextContentConsumer}.
 *
 * @author Vladislav Bauer
 */
public class TextContentConsumerTest {

    @Test
    public void testConsumer() {
        final TextContentConsumer consumer = new TextContentConsumer();
        checkConsumer(consumer);
    }

    @Test(expected = MdsS3Exception.class)
    public void testConsumerNegative() {
        final TextContentConsumer consumer = new TextContentConsumer();
        consumer.consume(InOutUtils.badInputStream());
    }


    public static void checkConsumer(@Nonnull final TextContentConsumer consumer) {
        final String result = consumer.consume(InOutUtils.inputStream());
        assertThat(result, equalTo(TEST_DATA));
    }

}
