package ru.yandex.market.common.mds.s3.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.mds.s3.client.content.factory.ContentConsumerFactory;
import ru.yandex.market.common.mds.s3.client.content.factory.ContentProviderFactory;
import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.DirectMdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.api.impl.DirectMdsS3ClientImpl;
import ru.yandex.market.common.mds.s3.client.service.factory.HttpClientFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.common.mds.s3.client.test.TestProperties.PROPS;
import static ru.yandex.market.common.mds.s3.client.test.TestUtils.todaysRandomFolder;

/**
 * @author Mikhail Khorkov (atroxaper@yandex-team.ru)
 */
@Disabled
public class DirectMdsS3ClientTest extends AbstractIntegrationTest {

    private static DirectMdsS3Client directMdsS3Client;

    @BeforeEach
    public void onBefore() {
        super.onBefore();
        directMdsS3Client = new DirectMdsS3ClientImpl(HttpClientFactory.create(), PROPS.endpoint());
    }

    @Test
    void downloadPositive() {
        final String data = "upload";

        final ResourceLocation location = location(todaysRandomFolder(), "direct-positive");
        mdsS3Client.upload(location, ContentProviderFactory.text(data));

        final String downloaded = directMdsS3Client.download(location, ContentConsumerFactory.text());
        assertEquals(downloaded, data);
    }

    @Test
    void downloadNotFound() {
        final ResourceLocation location = location(todaysRandomFolder(), "not-found");
        assertThrows(MdsS3Exception.class, () -> directMdsS3Client.download(location, ContentConsumerFactory.text()));
    }
}
