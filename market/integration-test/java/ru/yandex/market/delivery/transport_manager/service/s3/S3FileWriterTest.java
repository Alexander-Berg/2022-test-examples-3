package ru.yandex.market.delivery.transport_manager.service.s3;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

class S3FileWriterTest extends AbstractContextualTest {
    @Autowired
    private S3FileWriter s3FileWriter;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Test
    void writeString() throws MalformedURLException {
        Mockito.doNothing().when(mdsS3Client).upload(Mockito.any(), Mockito.any());
        Mockito.when(mdsS3Client.getUrl(resourceLocation("f", "g")))
            .thenReturn(new URL("http://aa.com/bb"));

        URL url = s3FileWriter.write("ololo", "f", "g");

        Mockito.verify(mdsS3Client).upload(
            Mockito.any(),
            Mockito.any()
        );

        softly.assertThat(url).isEqualTo(new URL("http://aa.com/bb"));
    }

    @Test
    void writeFile() throws MalformedURLException {
        Mockito.doNothing().when(mdsS3Client).upload(Mockito.any(), Mockito.any());
        Mockito.when(mdsS3Client.getUrl(resourceLocation("f", "g")))
            .thenReturn(new URL("http://aa.com/ccc"));

        URL url = s3FileWriter.write(new ByteArrayInputStream(new byte[] {0}), "fff", "ggg");

        Mockito.verify(mdsS3Client).upload(
            Mockito.any(),
            Mockito.any()
        );

        softly.assertThat(url).isEqualTo(new URL("http://aa.com/ccc"));
    }

    private ResourceLocation resourceLocation(String... path) {
        return resourceLocationFactory.createLocation(
            Stream.of(path).map(String::trim).collect(Collectors.joining("/"))
        );
    }
}
