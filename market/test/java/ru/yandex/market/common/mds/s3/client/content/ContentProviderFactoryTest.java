package ru.yandex.market.common.mds.s3.client.content;

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProviderTest;
import ru.yandex.market.common.mds.s3.client.content.provider.PathContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.PathContentProviderTest;
import ru.yandex.market.common.mds.s3.client.content.provider.StreamContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProviderTest;
import ru.yandex.market.common.mds.s3.client.content.provider.UriContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.UriContentProviderTest;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.test.InOutUtils;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.TEST_DATA;

/**
 * Unit-тесты для {@link ContentProviderFactory}.
 *
 * @author Vladislav Bauer
 */
public class ContentProviderFactoryTest {

    private static final String TEST_FILE_TXT = "/test-file.txt";


    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(ContentProviderFactory.class);
    }

    @Test
    public void testFile() {
        checkFileProvider(ContentProviderFactory::file);
    }

    @Test
    public void testFileWithName() {
        checkFileProvider((file) -> ContentProviderFactory.file(file.getAbsolutePath()));
    }

    @Test
    public void testFileWithContentProvider() {
        final TextContentProvider textContentProvider = ContentProviderFactory.text(TEST_DATA);
        final FileContentProvider fileContentProvider = ContentProviderFactory.file(textContentProvider);

        TestUtils.checkProvider(fileContentProvider, TEST_DATA);
    }

    @Test(expected = MdsS3Exception.class)
    public void testFileException() {
        final ContentProvider provider = mock(ContentProvider.class);
        when(provider.getInputStream()).thenThrow(MdsS3Exception.class);
        fail(String.valueOf(ContentProviderFactory.file(provider)));
    }

    @Test
    public void testFileWithFileContentProvider() {
        final TextContentProvider textContentProvider = ContentProviderFactory.text(TEST_DATA);
        final FileContentProvider fileContentProvider = ContentProviderFactory.file(textContentProvider);
        final FileContentProvider contentProvider = ContentProviderFactory.file(fileContentProvider);

        assertThat(fileContentProvider, equalTo(contentProvider));
        TestUtils.checkProvider(contentProvider, TEST_DATA);
    }

    @Test
    public void testText() {
        final TextContentProvider provider = ContentProviderFactory.text(TEST_DATA);
        TextContentProviderTest.checkProvider(provider);
    }

    @Test
    public void testStream() {
        final StreamContentProvider provider = ContentProviderFactory.stream(InOutUtils.inputStream());
        TestUtils.checkProvider(provider, TEST_DATA);
    }

    @Test
    public void testResourceAsStream() throws Exception{
        final StreamContentProvider provider = ContentProviderFactory.resourceAsStream(getClass(), TEST_FILE_TXT);
        final String expectedData = IOUtils.toString(getClass().getResource(TEST_FILE_TXT), StandardCharsets.UTF_8);

        TestUtils.checkProvider(provider, expectedData);
    }

    @Test
    public void testUri() {
        final File tempFile = InOutUtils.createTempFile();
        try {
            final URI uri = tempFile.toURI();
            final UriContentProvider provider = ContentProviderFactory.uri(uri);

            UriContentProviderTest.checkProvider(uri, provider);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test
    public void testPath() {
        checkPathProvider(ContentProviderFactory::path);
    }

    @Test
    public void testPathWithName() {
        checkPathProvider((path) -> ContentProviderFactory.path(path.toFile().getAbsolutePath()));
    }


    private void checkFileProvider(final Function<File, FileContentProvider> function) {
        final File tempFile = InOutUtils.createTempFile();
        try {
            final FileContentProvider provider = function.apply(tempFile);
            FileContentProviderTest.checkProvider(tempFile, provider);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    private void checkPathProvider(final Function<Path, PathContentProvider> function) {
        final File tempFile = InOutUtils.createTempFile();
        final Path tempPath = tempFile.toPath();

        try {
            final PathContentProvider provider = function.apply(tempPath);
            PathContentProviderTest.checkProvider(tempPath, provider);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

}
