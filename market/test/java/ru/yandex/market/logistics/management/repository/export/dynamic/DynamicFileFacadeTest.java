package ru.yandex.market.logistics.management.repository.export.dynamic;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DynamicFileFacadeTest extends AbstractTest {

    private static final String DYNAMIC_FILE_NAME = "lms";

    @Mock
    private MdsS3Client s3Client;

    @Test
    void testUploadCorrectData() throws IOException {
        MdsS3BucketClient client = new MdsS3BucketClient(s3Client, "bucket");
        DynamicGzipMdsRepository gzipRepository = new DynamicGzipMdsRepository(client);

        gzipRepository.set(DYNAMIC_FILE_NAME, createMetaInfo());

        ArgumentCaptor<ContentProvider> captor = ArgumentCaptor.forClass(ContentProvider.class);
        verify(s3Client)
            .upload(eq(ResourceLocation.create("bucket", "report_dynamic/lms.pbuf.sn.gz")), captor.capture());

        softly.assertThat(captor.getValue().getInputStream())
            .hasSameContentAs(new ClassPathResource("data/mds/lms.pbuf.sn.gz").getInputStream());
    }

    private Logistics.MetaInfo createMetaInfo() {
        return Logistics.MetaInfo.newBuilder()
            .setCalendarMetaInfo(Logistics.CalendarMetaInfo.newBuilder()
                .setDepth(1)
                .setStartDate("01.02.2003"))
            .build();
    }

}
