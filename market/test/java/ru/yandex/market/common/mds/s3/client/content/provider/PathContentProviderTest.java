package ru.yandex.market.common.mds.s3.client.content.provider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link PathContentProvider}.
 *
 * @author Vladislav Bauer
 */
public class PathContentProviderTest {

    @Test
    public void testProvider() {
        final File tempFile = InOutUtils.createTempFile();
        final Path tempPath = Paths.get(tempFile.getAbsolutePath());

        try {
            final PathContentProvider provider = new PathContentProvider(tempPath);

            checkProvider(tempPath, provider);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test(expected = MdsS3Exception.class)
    public void testException() {
        final Path path = Mockito.mock(Path.class, (invocation) -> {
            throw new IOException("I'm an error");
        });
        final PathContentProvider provider = new PathContentProvider(path);
        fail(String.valueOf(provider.getInputStream()));
    }


    public static void checkProvider(@Nonnull final Path tempPath, @Nonnull final PathContentProvider provider) {
        TestUtils.checkProvider(provider, TEST_DATA);
        assertThat(tempPath, equalTo(provider.getPath()));
    }

}
