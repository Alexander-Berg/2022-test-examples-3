package ru.yandex.market.logistics.management.repository.export;

import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.common.mds.s3.client.content.ContentProvider;
import ru.yandex.market.common.mds.s3.client.model.FileMeta;
import ru.yandex.market.common.mds.s3.client.model.ResourceListing;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.Logistics;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.repository.export.dynamic.DynamicSnappyMdsRepository;
import ru.yandex.market.logistics.management.repository.export.dynamic.s3.MdsS3BucketClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VersionedMdsRepository")
@ExtendWith(MockitoExtension.class)
class VersionedMdsRepositoryTest extends AbstractTest {
    private static final String BUCKET = "bucket";
    @Mock
    private MdsS3Client s3Client;
    private MdsS3BucketClient bucketClient;

    private Clock clock;

    private Instant now = LocalDateTime.parse("2020-02-20T20:20:20").toInstant(ZoneOffset.UTC);

    private VersionedMdsRepository<Logistics.MetaInfo> repository;

    private MdsRepository<Logistics.MetaInfo> subRepository;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneId.systemDefault());

        bucketClient = new MdsS3BucketClient(s3Client, BUCKET);

        subRepository = new DynamicSnappyMdsRepository(bucketClient);
        repository = new VersionedMdsRepository<>(subRepository, bucketClient, clock);
    }

    @DisplayName("сохранение копии с текущим таймстемпом")
    @Test
    @SneakyThrows
    void saveVersionUpload() {
        Logistics.MetaInfo metaInfo = createMetaInfo();

        var fileName = String.format(
            "%s/report.%s-%s",
            repository.getFullOlddir(),
            subRepository.getMdsFileType().getFileExtension(),
            VersionedMdsRepository.FORMATTER.format(clock.instant())
        );

        ArgumentCaptor<ContentProvider> contentProviderArgumentCaptor = ArgumentCaptor.forClass(ContentProvider.class);
        repository.set("report", metaInfo);
        verify(s3Client).upload(
            eq(ResourceLocation.create(BUCKET, fileName)), contentProviderArgumentCaptor.capture()
        );

        softly.assertThat(contentProviderArgumentCaptor.getValue().getInputStream().readAllBytes())
            .isEqualTo(subRepository.getContentAsBytes(metaInfo));
    }

    @DisplayName("очистка архивных версий")
    @Test
    @SneakyThrows
    void versionCleanup() {
        List<FileMeta> fileList = List.of(
            new FileMeta("report_dynamic/archive/f.2020-07-09.pbuf", Date.from(now)),
            new FileMeta("report_dynamic/archive/f.2020-07-08.pbuf", Date.from(now.minus(Duration.ofDays(1)))),
            new FileMeta("report_dynamic/archive/f.2020-07-06.pbuf", Date.from(now.minus(Duration.ofDays(3)))),
            new FileMeta("report_dynamic/archive/a.2020-07-06.pbuf", Date.from(now.minus(Duration.ofDays(3))))
        );

        when(s3Client.list(any(), eq(false)))
            .thenReturn(ResourceListing.createListingWithMeta(
                BUCKET,
                fileList,
                List.of()
            ));
        repository.doCleanup(Collections.singletonList("f"), now);

        verify(s3Client).delete(eq(ResourceLocation.create(BUCKET, "report_dynamic/archive/f.2020-07-06.pbuf")));
    }

    private Logistics.MetaInfo createMetaInfo() {
        return Logistics.MetaInfo.newBuilder()
                .setCalendarMetaInfo(Logistics.CalendarMetaInfo.newBuilder()
                        .setDepth(1)
                        .setStartDate("01.02.2003"))
                .build();
    }
}
