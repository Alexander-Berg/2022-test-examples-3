package ru.yandex.direct.jobs.banner;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import junitparams.converters.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.banner.type.image.BannerImageRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.image.repository.BannerImagesUploadsRepository;
import ru.yandex.direct.core.entity.mdsfile.service.MdsFileService;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusshow;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_UPLOADS;
import static ru.yandex.direct.dbschema.ppc.tables.BannerImages.BANNER_IMAGES;

@JobsTest
@ExtendWith(SpringExtension.class)
class RemoveBannerImagesJobTest {

    private static final int SHARD = 2;

    @Autowired
    private BannerImageRepository bannerImageRepository;
    @Autowired
    private BannerImagesUploadsRepository bannerImagesUploadsRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private BannerImageFormatRepository bannerImageFormatRepository;
    @Mock
    private MdsFileService mdsFileService;

    private BannerImagesUploadsRepository bannerImagesUploadsRepositorySpy;
    private RemoveBannerImagesJob job;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        bannerImagesUploadsRepositorySpy = Mockito.spy(bannerImagesUploadsRepository);
        BannerImageRepository bannerImageRepositorySpy = Mockito.spy(bannerImageRepository);
        job = new RemoveBannerImagesJob(SHARD, bannerImageRepositorySpy, mdsFileService, bannerImageFormatRepository,
                bannerImagesUploadsRepositorySpy, Duration.ofMillis(1000L));
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    /**
     * Если в таблице BANNER_IMAGES_UPLOADS будут строки с разными DATE_ADDED датами (раньше и позже чем
     * now() - {@link RemoveBannerImagesJob#UPLOADS_MAX_AGE_HOURS} часов), то при запуске джобы будут удалены
     * только строки из BANNER_IMAGES_UPLOADS с DATE_ADDED меньшей (позжей) заданной выше
     */
    @Test
    void cleanBannerImagesUploads() {
        long expectedToDeleteId1 = RandomUtils.nextInt();
        long notExpectedToDeleteId2 = RandomUtils.nextInt();

        insertBannerImagesUploads(expectedToDeleteId1, null, 100L, LocalDateTime.now()
                .minus(RemoveBannerImagesJob.UPLOADS_MAX_AGE_HOURS)
                .minusMinutes(1));
        insertBannerImagesUploads(notExpectedToDeleteId2, null, 101L, LocalDateTime.now()
                .minus(RemoveBannerImagesJob.UPLOADS_MAX_AGE_HOURS)
                .plusMinutes(1L));
        executeJob();

        List<Long> ids = bannerImagesUploadsRepositorySpy.getIdsByDateAdded(SHARD, LocalDateTime.now().plusMinutes(1L));
        assertThat(ids).contains(notExpectedToDeleteId2).doesNotContain(expectedToDeleteId1);
    }


    /**
     * Если в таблице BANNER_IMAGES будут строки с STATUS_SHOW = Yes и BANNER_ID больше 0, то при запуске джобы они
     * не будут удалены
     */
    @Test
    void imageSentToBSNotRemovedTest() {
        String notExpectedToDeleteHash = random(22, true, true);
        insertBannerImages(notExpectedToDeleteHash, 55L, BannerImagesStatusshow.Yes, LocalDateTime.now()
                .minus(RemoveBannerImagesJob.IMAGES_MAX_AGE_HOURS)
                .minusDays(1));
        executeJob();

        List<String> bannerImageHashes = getBannerImageHashes(List.of(notExpectedToDeleteHash));
        assertThat(bannerImageHashes).contains(notExpectedToDeleteHash);
    }

    /**
     * Если в таблице BANNER_IMAGES будут строки с разными DATE_ADDED датами (раньше и позже чем
     * now() - {@link RemoveBannerImagesJob#IMAGES_MAX_AGE_HOURS} часов), то при запуске джобы
     * будут удалены только строки из BANNER_IMAGES с DATE_ADDED меньшей (позжей) заданной выше
     */
    @Test
    void cleanBannerImages() {
        String expectedToDeleteHash = random(22, true, true);
        String notExpectedToDeleteHash = random(22, true, true);
        insertBannerImages(expectedToDeleteHash, 0L, BannerImagesStatusshow.No, LocalDateTime.now()
                .minus(RemoveBannerImagesJob.IMAGES_MAX_AGE_HOURS)
                .minusMinutes(1));
        insertBannerImages(notExpectedToDeleteHash, 0L, BannerImagesStatusshow.No, LocalDateTime.now()
                .minus(RemoveBannerImagesJob.IMAGES_MAX_AGE_HOURS)
                .plusMinutes(1));
        executeJob();

        List<String> bannerImageHashes = getBannerImageHashes(List.of(expectedToDeleteHash, notExpectedToDeleteHash));
        assertThat(bannerImageHashes).contains(notExpectedToDeleteHash).doesNotContain(expectedToDeleteHash);
    }

    /**
     * Если при выполнении метода {@link RemoveBannerImagesJob#cleanBannerImagesFormats} метод
     * {@link BannerImageFormatRepository#getHashesForDelete} при первом вызрове вернет заданный
     * список image_hash -> в метод удаления {@link BannerImageFormatRepository#deleteByHashesWithoutRelations} будут
     * переданны все полученные выше image_hash.
     */
    @Test
    void cleanBannerImagesFormats_checkIteration() {
        String expectedToDeleteHash = random(22, true, true);
        doReturn(List.of(expectedToDeleteHash)).when(bannerImageFormatRepository)
                .getHashesForDelete(anyInt(), anyInt());
        job.cleanBannerImagesFormats();
        verify(bannerImageFormatRepository).deleteByHashesWithoutRelations(anyInt(),
                ArgumentMatchers.eq(List.of(expectedToDeleteHash)));
    }

    private List<String> getBannerImageHashes(List<String> hashes) {
        return dslContextProvider.ppc(SHARD)
                .select(BANNER_IMAGES.IMAGE_HASH)
                .from(BANNER_IMAGES)
                .where(BANNER_IMAGES.IMAGE_HASH.in(hashes))
                .fetch(BANNER_IMAGES.IMAGE_HASH);
    }

    private void insertBannerImages(String imageHash, long bannerId,
                                    BannerImagesStatusshow bannerImagesStatusshow, LocalDateTime dateAdded) {
        dslContextProvider.ppc(SHARD)
                .insertInto(BANNER_IMAGES,
                        BANNER_IMAGES.BID,
                        BANNER_IMAGES.BANNER_ID,
                        BANNER_IMAGES.IMAGE_ID,
                        BANNER_IMAGES.DATE_ADDED,
                        BANNER_IMAGES.IMAGE_HASH,
                        BANNER_IMAGES.STATUS_SHOW,
                        BANNER_IMAGES.STATUS_MODERATE,
                        BANNER_IMAGES.PRIORITY_ID)
                .values((long) RandomUtils.nextInt(),
                        bannerId,
                        (long) RandomUtils.nextInt(),
                        dateAdded,
                        imageHash,
                        bannerImagesStatusshow,
                        BannerImagesStatusmoderate.Yes,
                        1L)
                .execute();
    }

    private void insertBannerImagesUploads(long id, @Nullable String hashOrigin, long cid, LocalDateTime logDate) {
        dslContextProvider.ppc(SHARD)
                .insertInto(BANNER_IMAGES_UPLOADS,
                        BANNER_IMAGES_UPLOADS.ID,
                        BANNER_IMAGES_UPLOADS.HASH_ORIG,
                        BANNER_IMAGES_UPLOADS.CID,
                        BANNER_IMAGES_UPLOADS.DATE_ADDED)
                .values(id,
                        hashOrigin != null ? hashOrigin : random(22, true, true),
                        cid,
                        logDate)
                .execute();
    }
}
