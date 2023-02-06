package ru.yandex.market.common.mds.s3.client.service.decorator;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.HistoryMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.data.ResourceConfigurationProvider;
import ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit-тест для {@link HistoryMdsS3ClientDecorator}
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@RunWith(Parameterized.class)
public class HistoryMdsS3ClientDecoratorTest {

    private static final String BUCKET = "bucket";
    private static final String KEY = "key";
    private static final String FAKE_URL = "http://fake.url";


    private HistoryMdsS3ClientDecorator<ResourceConfiguration> decorator;

    private boolean withFolder;

    public HistoryMdsS3ClientDecoratorTest(final boolean withFolder) {
        this.withFolder = withFolder;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { true }, { false }
        });
    }

    @Mock(answer = Answers.RETURNS_MOCKS)
    private HistoryMdsS3Client<ResourceConfiguration> historyMdsS3Client;

    @Before
    public void onBefore() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(historyMdsS3Client.getUrl(ResourceLocation.create(BUCKET, KEY))).thenReturn(new URL(FAKE_URL));
        decorator = new HistoryMdsS3ClientDecorator<>(historyMdsS3Client);
    }

    @Test
    public void upload() {
        final ContentProvider contentProvider = ContentProviderFactory.text("arg2");
        final ResourceConfiguration configuration = mock(ResourceConfiguration.class);
        decorator.upload(configuration, contentProvider);

        verify(historyMdsS3Client, times(1)).upload(configuration, contentProvider);
        verifyNoMoreInteractions(historyMdsS3Client);
    }

    @Test
    public void downloadLast() {
        final ContentConsumer<String> arg2 = ContentConsumerFactory.text();
        final ResourceConfiguration configuration = mock(ResourceConfiguration.class);
        decorator.downloadLast(configuration, arg2);

        verify(historyMdsS3Client, times(1)).downloadLast(configuration, arg2);
        verifyNoMoreInteractions(historyMdsS3Client);
    }

    @Test
    public void deleteOld() {
        final ResourceConfigurationProvider provider = ResourceConfigurationProviderFactory.create(BUCKET, withFolder);
        final Collection<ResourceConfiguration> configurations = provider.getConfigurations();

        for (final ResourceConfiguration configuration : configurations) {
            decorator.deleteOld(configuration);
            verify(historyMdsS3Client, times(1)).deleteOld(configuration);
        }

        verifyNoMoreInteractions(historyMdsS3Client);
    }

    @Test
    public void getUrl() {
        final URL url = decorator.getUrl(ResourceLocation.create(BUCKET, KEY));
        assertThat(url, notNullValue());

        verify(historyMdsS3Client, atLeastOnce()).getUrl(eq(ResourceLocation.create(BUCKET, KEY)));
        verifyNoMoreInteractions(historyMdsS3Client);
    }
}
