package ru.yandex.market.common.mds.s3.client.util;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.test.TestUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit-тесты для {@link ContentProviderUtils}.
 *
 * @author Vladislav Bauer
 */
public class ContentProviderUtilsTest {

    private static final String TEST_TEXT = "test";


    @Test
    public void testConstructorContract() {
        TestUtils.checkConstructor(ContentProviderUtils.class);
    }

    @Test
    public void testDoWithFile() {
        final TextContentProvider contentProvider = ContentProviderFactory.text(TEST_TEXT);
        final Consumer<FileContentProvider> checkingConsumer = createCheckingConsumer();

        ContentProviderUtils.doWithFile(contentProvider, checkingConsumer);
    }

    @Test(expected = RuntimeException.class)
    public void testDoWithFileNegative() {
        final TextContentProvider contentProvider = ContentProviderFactory.text(TEST_TEXT);
        final Consumer<FileContentProvider> badConsumer = createBadConsumer();

        ContentProviderUtils.doWithFile(contentProvider, badConsumer);
    }

    @Test
    public void testDoWithFileProvider() throws Exception {
        final File tempFile = TempFileUtils.createTempFile();
        try {
            FileUtils.writeStringToFile(tempFile, TEST_TEXT, StandardCharsets.UTF_8);

            final FileContentProvider contentProvider = ContentProviderFactory.file(tempFile);
            final Consumer<FileContentProvider> checkingConsumer = createCheckingConsumer();

            ContentProviderUtils.doWithFile(contentProvider, checkingConsumer);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testDoWithFileProviderNegative() {
        final File tempFile = TempFileUtils.createTempFile();
        try {
            final FileContentProvider contentProvider = ContentProviderFactory.file(tempFile);
            final Consumer<FileContentProvider> badConsumer = createBadConsumer();

            ContentProviderUtils.doWithFile(contentProvider, badConsumer);
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }


    private Consumer<FileContentProvider> createCheckingConsumer() {
        return (fileContentProvider) -> {
            try (InputStream inputStream = fileContentProvider.getInputStream()) {
                final String actual = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                assertThat(actual, equalTo(TEST_TEXT));
            } catch (final Exception ex) {
                fail(ex.getMessage());
            }
        };
    }

    private Consumer<FileContentProvider> createBadConsumer() {
        return (provider) -> {
            throw new RuntimeException("I'm bad");
        };
    }

}
