package ru.yandex.market.mbo.synchronizer.export.io;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockito.Mockito;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistry;
import ru.yandex.market.mbo.synchronizer.export.ExportRegistryMock;
import ru.yandex.market.mbo.synchronizer.export.storage.S3DumpStorageCoreService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("checkstyle:magicnumber")
public class S3OutputStreamTest {

    private S3DumpStorageCoreService s3DumpStorageCoreService = mock(S3DumpStorageCoreService.class);
    private S3OutputStream writer;
    private String uploadId = "uploadId";
    private String eTag = "eTag";


    private ExportRegistry registry;
    private Map<Integer, byte[]> parts;

    @Before
    public void setUp() throws Exception {
        parts = new HashMap<>();
        s3DumpStorageCoreService = Mockito.mock(S3DumpStorageCoreService.class);
        when(s3DumpStorageCoreService.initiateMultipartUploadToS3(Mockito.anyString())).thenReturn(uploadId);
        when(s3DumpStorageCoreService.uploadPartToS3(Mockito.any(UploadPartRequest.class)))
            .thenAnswer(invocation -> {
                UploadPartRequest request = invocation.getArgument(0);
                byte[] arr = new byte[(int) request.getPartSize()];
                ((ByteArrayInputStream) request.getInputStream()).read(arr, 0, (int) request.getPartSize());
                parts.put(request.getPartNumber(), arr);
                return new PartETag(-1, eTag);
            });

        registry = new ExportRegistryMock();
        registry.setDumpName("test");
        registry.processStart();
    }

    @Test
    public void testWriteFunction() throws IOException {
        writer = new S3OutputStream(s3DumpStorageCoreService, registry, "hmm_file", 5);
        writer.write(new byte[]{1, 2, 3, 4, 5, 6});

        Mockito.verify(s3DumpStorageCoreService, Mockito.times(1))
            .initiateMultipartUploadToS3(Mockito.anyString());
        Mockito.verify(s3DumpStorageCoreService, Mockito.times(1))
            .uploadPartToS3(Mockito.any(UploadPartRequest.class));
        Mockito.verify(s3DumpStorageCoreService, Mockito.times(0))
            .completeMultipartUploadToS3(Mockito.anyString(), Mockito.anyString(), Mockito.anyList());
        Assertions.assertThat(parts).hasSize(1);
        Assertions.assertThat(parts.get(1)).contains(1, 2, 3, 4, 5);

        writer.close();
        Mockito.verify(s3DumpStorageCoreService, Mockito.times(1))
            .initiateMultipartUploadToS3(Mockito.anyString());
        Mockito.verify(s3DumpStorageCoreService, Mockito.times(2))
            .uploadPartToS3(Mockito.any(UploadPartRequest.class));
        Mockito.verify(s3DumpStorageCoreService, Mockito.times(1))
            .completeMultipartUploadToS3(Mockito.anyString(), Mockito.anyString(), Mockito.anyList());
        Assertions.assertThat(parts).hasSize(2);
        Assertions.assertThat(parts.get(2)).contains(6);
    }
}
