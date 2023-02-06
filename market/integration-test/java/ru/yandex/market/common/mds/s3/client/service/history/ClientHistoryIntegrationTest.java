package ru.yandex.market.common.mds.s3.client.service.history;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.converter.UnGzipFunction;
import ru.yandex.market.common.mds.s3.client.content.provider.TextContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceConfiguration;
import ru.yandex.market.common.mds.s3.client.model.ResourceFileDescriptor;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.AbstractIntegrationTest;
import ru.yandex.market.common.mds.s3.client.test.RandUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory.converter;
import static ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory.text;
import static ru.yandex.market.common.mds.s3.client.service.data.KeyGenerator.DELIMITER_FOLDER;
import static ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory.NAME_HISTORY_ONLY;
import static ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory.NAME_HISTORY_ONLY_GZIP;
import static ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory.NAME_HISTORY_WITH_LAST_25_GZIP;
import static ru.yandex.market.common.mds.s3.client.test.ResourceConfigurationProviderFactory.NAME_LAST_ONLY;

/**
 *
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
public class ClientHistoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void uploadOneLO() {
        final TextContentProvider text1 = RandUtils.createText();
        final TextContentProvider text2 = RandUtils.createText();

        final ResourceLocation location1 = namedHistoryMdsS3Client.upload(NAME_LAST_ONLY, text1);
        final ResourceLocation location2 = namedHistoryMdsS3Client.upload(NAME_LAST_ONLY, text2);
        checkLocation(location1);
        checkLocation(location2);

        URL url1 = namedHistoryMdsS3Client.getUrl(location1);
        URL url2 = namedHistoryMdsS3Client.getUrl(location2);
        checkUrls(url1, url2);
        assertEquals(url1, url2);

        final String data = namedHistoryMdsS3Client.downloadLast(NAME_LAST_ONLY, text());
        assertEquals(data, text2.getText());
    }

    @Test
    void uploadWithLocationHO() {
        final TextContentProvider text1 = RandUtils.createText();
        final TextContentProvider text2 = RandUtils.createText();

        final ResourceLocation location1 = namedHistoryMdsS3Client.upload(NAME_HISTORY_ONLY, text1);
        final ResourceLocation location2 = namedHistoryMdsS3Client.upload(NAME_HISTORY_ONLY, text2);

        checkLocation(location1);
        checkLocation(location2);

        URL url1 = namedHistoryMdsS3Client.getUrl(location1);
        URL url2 = namedHistoryMdsS3Client.getUrl(location2);
        checkUrls(url1, url2);
        assertNotEquals(url1, url2);

        final String data1 = mdsS3Client.download(location1, text());
        assertEquals(data1, text1.getText());

        final String data2 = mdsS3Client.download(location2, text());
        assertEquals(data2, text2.getText());
    }

    @Test
    void uploadWithLocationHOAndGZip() {
        final TextContentProvider text1 = RandUtils.createText();
        final TextContentProvider text2 = RandUtils.createText();

        final ResourceLocation location1 = namedHistoryMdsS3Client.upload(NAME_HISTORY_ONLY_GZIP, text1);
        final ResourceLocation location2 = namedHistoryMdsS3Client.upload(NAME_HISTORY_ONLY_GZIP, text2);

        checkLocation(location1);
        checkLocation(location2);

        URL url1 = namedHistoryMdsS3Client.getUrl(location1);
        URL url2 = namedHistoryMdsS3Client.getUrl(location2);
        checkUrls(url1, url2);
        assertNotEquals(url1, url2);

        final String data1 = mdsS3Client.download(location1, gunzip());
        assertEquals(data1, text1.getText());

        final String data2 = mdsS3Client.download(location2, gunzip());
        assertEquals(data2, text2.getText());
    }

    @Test
    void uploadWithLocationHistoryGZipAndLastDownloadLast() {
        final TextContentProvider text1 = RandUtils.createText();
        final TextContentProvider text2 = RandUtils.createText();

        final ResourceLocation location1 = namedHistoryMdsS3Client.upload(NAME_HISTORY_WITH_LAST_25_GZIP, text1);
        final ResourceLocation location2 = namedHistoryMdsS3Client.upload(NAME_HISTORY_WITH_LAST_25_GZIP, text2);

        checkLocation(location1);
        checkLocation(location2);

        final String data1 = mdsS3Client.download(location1, gunzip());
        assertEquals(data1, text1.getText());

        final String data2 = mdsS3Client.download(location2, gunzip());
        assertEquals(data2, text2.getText());

        final String data = namedHistoryMdsS3Client.downloadLast(NAME_HISTORY_WITH_LAST_25_GZIP, text());
        assertEquals(data, text2.getText());
    }

    @Test
    void downloadLast() {
        final TextContentProvider text1 = RandUtils.createText();
        final TextContentProvider text2 = RandUtils.createText();

        final ResourceLocation location1 = namedHistoryMdsS3Client.upload(NAME_LAST_ONLY, text1);
        final ResourceLocation location2 = namedHistoryMdsS3Client.upload(NAME_LAST_ONLY, text2);

        assertNotEquals(text1.getText(), text2.getText());
        assertEquals(location1.getBucketName(), location2.getBucketName());
        assertEquals(location1.getKey(), location2.getKey());

        final String data = namedHistoryMdsS3Client.downloadLast(NAME_LAST_ONLY, text());
        assertEquals(data, text2.getText());
    }

    @Test
    void downloadHistoryLast() {
        final ResourceConfiguration configuration = resourceConfigurationProvider.getByName(NAME_HISTORY_ONLY);

        final ResourceFileDescriptor fileDescriptor = configuration.getFileDescriptor();
        final String bucket = configuration.getBucketName();
        final String folder = configuration.getFileDescriptor().getFolder().get();
        final String path = folder + DELIMITER_FOLDER + fileDescriptor.getName();
        final String[] keys = new String[]{
                path + "/2017/current0",
                path + "/2017/current1",
                path + "/2017/05_24/current2",
                path + "/2016/current3",
                path + "/2015/current4"
        };

        final List<TextContentProvider> texts = RandUtils.createTexts(keys.length);

        for (int i = 0; i < keys.length; i++) {
            final ResourceLocation destination = ResourceLocation.create(bucket, keys[i]);
            mdsS3Client.upload(destination, texts.get(i));
        }

        final String data = namedHistoryMdsS3Client.downloadLast(NAME_HISTORY_ONLY, text());
        assertEquals(data, texts.get(1).getText());
    }


    private void checkLocation(final ResourceLocation location) {
        assertThat(location.getBucketName(), not(isEmptyOrNullString()));
        assertThat(location.getKey(), not(isEmptyOrNullString()));
    }

    private void checkUrls(URL url1, URL url2) {
        assertNotNull(url1);
        assertNotNull(url2);
    }

    private ContentConsumer<String> gunzip() {
        return converter(text(), is -> new UnGzipFunction().apply(is));
    }
}
