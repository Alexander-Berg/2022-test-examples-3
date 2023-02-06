package ru.yandex.market.delivery.rupostintegrationapp.service.component.dbf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbfStorageTest extends BaseTest {

    @Mock
    private AmazonS3 amazonS3Client;

    @InjectMocks
    private DbfStorage storage;

    @Test
    void save() {
        File file = new File("/tmp/test.dbf");
        PutObjectResult result = new PutObjectResult();
        result.setVersionId("versionId");
        when(amazonS3Client.putObject(isNull(), anyString(), eq(file))).thenReturn(result);
        String name = storage.save(file);

        softly.assertThat(name.endsWith("dbf")).isTrue();
    }


    @Test
    void saveFailed() {
        File file = new File("/tmp/test.dbf");
        PutObjectResult result = new PutObjectResult();
        when(amazonS3Client.putObject(isNull(), anyString(), eq(file))).thenReturn(result);

        softly.assertThat(storage.save(file)).isNull();
    }

    @Test
    void get() throws IOException {
        S3Object object = mock(S3Object.class);

        when(object.getObjectContent())
            .thenReturn(new S3ObjectInputStream(
                new FileInputStream(
                    Files.createTempFile("resource-", ".dbf").toFile()
                ),
                new HttpGet("test")
            ));

        when(amazonS3Client.getObject(isNull(), anyString()))
            .thenReturn(object);

        softly.assertThat(storage.get("trololo"))
            .isInstanceOf(FileInputStream.class);
    }

    @Test
    void createPath() throws IOException {
        Path path = DbfStorage.createPath();
        softly.assertThat(path.toString().endsWith("dbf")).isTrue();
    }
}
