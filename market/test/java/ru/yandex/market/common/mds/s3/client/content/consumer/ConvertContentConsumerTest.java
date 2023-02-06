package ru.yandex.market.common.mds.s3.client.content.consumer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.codec.binary.Base64InputStream;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Unit-тесты для {@link ConvertContentConsumer}.
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ConvertContentConsumerTest {

    private static final String BASE64_OK = "cG9zaXRpdmU=";
    private static final String NATURAL_OK = "positive";

    @Test
    public void testConsumer() {
        final ContentConsumer<String> wrapper = ContentConsumerFactory.converter(
            ContentConsumerFactory.text(),
            is -> new Base64InputStream(is, false)
        );

        final byte[] bytes = BASE64_OK.getBytes(StandardCharsets.UTF_8);
        final String data = wrapper.consume(new ByteArrayInputStream(bytes));

        assertThat(data, equalTo(NATURAL_OK));
    }

}
