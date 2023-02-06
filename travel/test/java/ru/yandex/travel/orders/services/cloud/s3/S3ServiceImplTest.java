package ru.yandex.travel.orders.services.cloud.s3;

import com.amazonaws.AbortedException;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

public class S3ServiceImplTest {
    private AmazonS3 mockS3;
    private S3ServiceImpl impl;

    @Before
    public void init() {
        mockS3 = Mockito.mock(AmazonS3.class, RETURNS_DEEP_STUBS);
        impl = new S3ServiceImpl(S3ServiceProperties.builder()
                .bucket("test-bucket")
                .maxInMemoryFileSize(20 * 1024 * 1024)
                .build(), mockS3);
    }

    @Test(expected = RetryableException.class)
    public void retryableExceptions_checkExists() {
        when(mockS3.doesObjectExist(any(), any()))
                .thenThrow(new SdkClientException("This exception's parent class is retryable by default"));
        impl.checkObjectExists("some-id");
    }

    @Test(expected = AbortedException.class)
    public void nonRetryableExceptions_checkExists() {
        when(mockS3.doesObjectExist(any(), any())).thenThrow(new AbortedException("non retryable"));
        impl.checkObjectExists("some-id");
    }

    @Test(expected = RetryableException.class)
    public void retryableExceptions_upload() {
        when(mockS3.putObject(any()))
                .thenThrow(new SdkClientException("This exception's parent class is retryable by default"));
        impl.uploadObject(new InMemoryS3Object("any", "mime/type", "file.name", new byte[0]));
    }

    @Test(expected = RetryableException.class)
    public void retryableExceptions_read() {
        when(mockS3.getObject((String) any(), any()))
                .thenThrow(new SdkClientException("This exception's parent class is retryable by default"));
        impl.readObject("some-id");
    }

    @Test
    public void maxFileSize() {
        S3Object deepMockObject = mockS3.getObject("test-bucket", "some-id");
        when(deepMockObject.getObjectMetadata().getContentLength())
                .thenReturn(50 * 1024 * 1024L);
        assertThatThrownBy(() -> impl.readObject("some-id"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds the maximum allowed in-memory file size");
    }
}
