package ru.yandex.market.common.mds.s3.client.content.provider;

import java.io.File;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link FileContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class FileContentProviderTest {

    @Test
    public void testProvider() {
        final File tempFile = InOutUtils.createTempFile();
        try {
            final FileContentProvider provider = new FileContentProvider(tempFile);

            checkProvider(tempFile, provider);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test(expected = MdsS3Exception.class)
    public void testException() {
        final File file = Mockito.mock(File.class);
        when(file.getPath()).thenThrow(MdsS3Exception.class);
        final FileContentProvider provider = new FileContentProvider(file);
        fail(String.valueOf(provider.getInputStream()));
    }


    public static void checkProvider(@Nonnull final File tempFile, @Nonnull final FileContentProvider provider) {
        TestUtils.checkProvider(provider, TEST_DATA);
        assertThat(tempFile, equalTo(provider.getFile()));
    }

}
