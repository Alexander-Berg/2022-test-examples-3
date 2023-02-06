package ru.yandex.market.delivery.transport_manager.service.s3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.s3.MdsS3Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class S3FileReaderTest extends AbstractContextualTest {
    @Autowired
    private MdsS3Properties mdsS3Properties;

    @Autowired
    private S3FileReader s3FileReader;

    @Autowired
    private MdsS3Client mdsS3Client;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    void readString() {
        when(mdsS3Client.download(any(), any())).thenReturn("ololo");
        String data = s3FileReader.read("f", "g");
        ResourceLocation location = ResourceLocation.create(mdsS3Properties.getGruzinBucketName(), "f/g");
        Mockito.verify(mdsS3Client).download(eq(location), any());
        softly.assertThat(data).isEqualTo("ololo");
    }
}
