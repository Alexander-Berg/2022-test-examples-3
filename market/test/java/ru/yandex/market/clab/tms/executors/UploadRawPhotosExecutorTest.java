package ru.yandex.market.clab.tms.executors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepository;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepositoryStub;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;
import ru.yandex.market.clab.tms.service.RawPhotoS3Service;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class UploadRawPhotosExecutorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private UploadRawPhotosExecutor uploadRawPhotosExecutor;

    private ComplexMonitoring monitoring = new ComplexMonitoring();

    private RawPhotoRepository rawPhotoRepository = new RawPhotoRepositoryStub();

    @Mock
    private RawPhotoS3Service rawPhotoS3Service;

    @Mock
    private GoodService goodService;

    private EnhancedRandom random;

    private static final long SEED = 823478372409234705L;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
        uploadRawPhotosExecutor = new UploadRawPhotosExecutor(
            monitoring, rawPhotoRepository, rawPhotoS3Service, goodService);
    }

    @Test
    public void testUploadedOk() {
        RawPhoto photo1 = createNotUploadedPhoto(1L);
        RawPhoto photo2 = createNotUploadedPhoto(1L);
        RawPhoto photo3 = createUploadedPhoto();

        rawPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        Good good = createGood(1L);
        when(goodService.getGoods(anyCollection())).thenReturn(Collections.singletonList(good));

        String somePath = "pathToUploadedPhoto";
        when(rawPhotoS3Service.uploadRawPhoto(any(Good.class), any(RawPhoto.class)))
            .thenReturn(somePath);

        uploadRawPhotosExecutor.doRealJob(null);

        List<RawPhoto> updatedPhotos = rawPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .isNotEmpty()
            .allMatch(p -> p.getLastUploadStatus() != null)
            .allMatch(p -> p.getUploadedTs() != null)
            .allMatch(p -> somePath.equals(p.getUploadedPath()));
    }

    @Test
    public void testUploadFailedGoodMissing() {
        RawPhoto photo1 = createNotUploadedPhoto(1L);
        RawPhoto photo2 = createNotUploadedPhoto(1L);
        RawPhoto photo3 = createUploadedPhoto();

        rawPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        when(goodService.getGoods(anyCollection())).thenReturn(Collections.emptyList());

        uploadRawPhotosExecutor.doRealJob(null);

        List<RawPhoto> updatedPhotos = rawPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .isNotEmpty()
            .allMatch(p -> p.getLastUploadStatus() != null)
            .allMatch(p -> p.getUploadedTs() == null)
            .allMatch(p -> p.getUploadedPath() == null);
    }

    @Test
    public void testUploadFailedException() {
        RawPhoto photo1 = createNotUploadedPhoto(1L);
        RawPhoto photo2 = createNotUploadedPhoto(1L);
        RawPhoto photo3 = createUploadedPhoto();

        rawPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        Good good = createGood(1L);
        when(goodService.getGoods(anyCollection())).thenReturn(Collections.singletonList(good));

        when(rawPhotoS3Service.uploadRawPhoto(any(Good.class), any(RawPhoto.class)))
            .thenThrow(new RuntimeException());

        uploadRawPhotosExecutor.doRealJob(null);

        List<RawPhoto> updatedPhotos = rawPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .isNotEmpty()
            .allMatch(p -> p.getLastUploadStatus() != null)
            .allMatch(p -> p.getUploadedTs() == null)
            .allMatch(p -> p.getUploadedPath() == null);
    }

    private RawPhoto createNotUploadedPhoto(Long goodId) {
        RawPhoto randomPhoto = random.nextObject(RawPhoto.class,
            "id", "uploadedTs", "uploadedPath", "lastUploadStatus");
        randomPhoto.setGoodId(goodId);
        randomPhoto.setBarcode("barcode" + goodId);
        return randomPhoto;
    }

    private Good createGood(Long goodId) {
        return random.nextObject(Good.class)
            .setId(goodId);
    }

    private RawPhoto createUploadedPhoto() {
        return random.nextObject(RawPhoto.class, "id");
    }
}
