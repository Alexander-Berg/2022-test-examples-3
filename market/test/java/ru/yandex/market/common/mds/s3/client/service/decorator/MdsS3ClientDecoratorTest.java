package ru.yandex.market.common.mds.s3.client.service.decorator;

import java.net.URL;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тесты для {@link MdsS3ClientDecorator}.
 *
 * @author Vladislav Bauer
 */
@RunWith(MockitoJUnitRunner.class)
public class MdsS3ClientDecoratorTest {

    private static final String FAKE_URL = "http://i-am-a-fake-url.com";


    @Mock(answer = Answers.RETURNS_MOCKS)
    private MdsS3Client mdsS3Client;

    @Mock
    private ResourceLocation location;

    @Mock
    private ContentConsumer<Object> contentConsumer;

    private MdsS3ClientDecorator decorator;


    @Before
    public void onBefore() throws Exception {
        when(mdsS3Client.getUrl(location)).thenReturn(new URL(FAKE_URL));

        decorator = new MdsS3ClientDecorator(mdsS3Client);
    }

    @Test
    public void testContains() {
        check(
            client -> client.contains(location),
            client -> client.contains(eq(location))
        );
    }

    @Test
    public void testGetUrl() {
        check(
            client -> client.getUrl(location),
            client -> client.getUrl(eq(location))
        );
    }

    @Test
    public void testDownload() {
        check(
            client -> client.download(location, contentConsumer),
            client -> client.download(eq(location), eq(contentConsumer))
        );
    }

    @Test
    public void testUpload() {
        final ContentProvider provider = mock(ContentProvider.class);

        check(
            client -> client.upload(location, provider),
            client -> client.upload(eq(location), eq(provider))
        );
    }

    @Test
    public void testDelete() {
        check(
            client -> client.delete(location),
            client -> client.delete(eq(location))
        );
    }

    @Test
    public void testDeleteUsingPrefix() {
        check(
            client -> client.deleteUsingPrefix(location),
            client -> client.deleteUsingPrefix(eq(location))
        );
    }

    @Test
    public void testList() {
        final boolean recursive = true;
        check(
            client -> client.list(location, recursive),
            client -> client.list(eq(location), eq(recursive))
        );
    }


    private void check(final Consumer<MdsS3Client> operation, final Consumer<MdsS3Client> checker) {
        operation.accept(decorator);

        final MdsS3Client verifyingObject = verify(mdsS3Client, times(1));
        checker.accept(verifyingObject);

        verifyNoMoreInteractions(mdsS3Client);
    }

}
