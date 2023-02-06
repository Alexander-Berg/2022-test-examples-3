package ru.yandex.market.mboc.app.offers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:MagicNumber")
public class ExcelS3ServiceImplTest {

    private static final String TEST_URL = "http://anything.bo";
    private static final String BUCKET_NAME = "bucket";
    private ExcelS3ServiceImpl service;
    private AmazonS3Mock s3Client;

    @Before
    public void setup() {
        NamedParameterJdbcTemplate jdbcMock = Mockito.mock(NamedParameterJdbcTemplate.class);
        s3Client = new AmazonS3Mock();
        when(jdbcMock.update(anyString(), anyMap())).thenReturn(1);
        service = new ExcelS3ServiceImpl(s3Client, new ObjectMapper(), jdbcMock, BUCKET_NAME);
    }

    @Test
    public void testUploadRecordHappens() {
        byte[] fileBytes = {
            (byte) 0x1, (byte) 0x2, (byte) 0x4, (byte) 0x3
        };
        service.uploadAsImportFile(100, "coo", "file.xlsx", fileBytes, null,
            ExcelFileType.ASSORTMENT);
        assertEquals(TEST_URL, s3Client.getUrl(null, null).toString());
        assertArrayEquals(fileBytes, s3Client.getData());
        assertEquals(service.generateImportKey(100, "coo", "file.xlsx"), s3Client.getKey());
    }

    private static class AmazonS3Mock extends AmazonS3Client {

        private String bucketName;
        private String key;
        private byte[] data = new byte[4];

        @Override
        public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata) {
            this.bucketName = bucketName;
            this.key = key;
            try {
                ByteArrayInputStream s = (ByteArrayInputStream) input;
                s.read(this.data);
            } catch (IOException ignored) {
            }
            return null;
        }

        @Override
        public URL getUrl(String any1, String any2) {
            if (bucketName != null && key != null && data != null) {
                try {
                    return new URL(TEST_URL);
                } catch (MalformedURLException ignored) {

                }
            }
            return null;
        }

        public byte[] getData() {
            return data;
        }

        public String getKey() {
            return key;
        }
    }
}
