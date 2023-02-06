package ru.yandex.market.common.mds.s3.client.content.provider;

import java.io.File;
import java.net.URI;

import javax.annotation.Nonnull;

import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link UriContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class UriContentProviderTest {

    @Test
    public void testProvider() {
        final File tempFile = InOutUtils.createTempFile();
        final URI uri = tempFile.toURI();
        final UriContentProvider provider = new UriContentProvider(uri);

        checkProvider(uri, provider);
    }

    @Test(expected = MdsS3Exception.class)
    public void testException() throws Exception {
        final URI uri = new URI("http://i-am-wrong-uri");
        final UriContentProvider provider = new UriContentProvider(uri);

        fail(String.valueOf(provider.getInputStream()));
    }


    public static void checkProvider(@Nonnull final URI uri, @Nonnull final UriContentProvider provider) {
        TestUtils.checkProvider(provider, TEST_DATA);
        assertThat(provider.getUri(), equalTo(uri));
    }

}
