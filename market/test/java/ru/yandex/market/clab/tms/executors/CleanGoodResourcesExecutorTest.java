package ru.yandex.market.clab.tms.executors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.nas.PhotoService;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepositoryStub;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepository;
import ru.yandex.market.clab.common.service.photo.RawPhotoRepositoryStub;
import ru.yandex.market.clab.db.jooq.generated.enums.CleanupStatus;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RawPhoto;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class CleanGoodResourcesExecutorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    private CleanupGoodResourcesExecutor cleanupGoodResourcesExecutor;

    private ComplexMonitoring monitoring = new ComplexMonitoring();

    private RawPhotoRepository rawPhotoRepository = new RawPhotoRepositoryStub();

    private EditedPhotoRepository editedPhotoRepository = new EditedPhotoRepositoryStub();

    @Mock
    private PhotoService photoService;

    @Mock
    private GoodService goodService;

    @Captor
    protected ArgumentCaptor<Long> goodIdCaptor;

    private EnhancedRandom random;

    private static final long SEED = 24623478236423498L;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
        cleanupGoodResourcesExecutor = new CleanupGoodResourcesExecutor(
            monitoring, goodService, rawPhotoRepository, editedPhotoRepository, photoService);
    }

    @Test
    public void testCleanedOk() {
        mockPhotosAndGoods();

        cleanupGoodResourcesExecutor.doRealJob(null);

        verify(goodService, times(2)).updateGood(goodIdCaptor.capture(), any());
        assertThat(goodIdCaptor.getAllValues()).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    public void testCleaningInProcessFailed() {
        mockPhotosAndGoods();

        doThrow(new RuntimeException()).when(photoService).deleteInProcessDirectory(anyString());

        cleanupGoodResourcesExecutor.doRealJob(null);

        verify(goodService, never()).updateGood(goodIdCaptor.capture(), any());
    }

    @Test
    public void testCleaningProcessedFailed() {
        mockPhotosAndGoods();

        doThrow(new RuntimeException()).when(photoService).deleteProcessedDirectory(anyString(), anyLong());

        cleanupGoodResourcesExecutor.doRealJob(null);

        verify(goodService, never()).updateGood(goodIdCaptor.capture(), any());
    }

    private RawPhoto createNotUploadedRawPhoto(Long goodId) {
        RawPhoto randomPhoto = random.nextObject(RawPhoto.class,
            "id", "uploadedTs", "uploadedPath", "lastUploadStatus");
        randomPhoto.setGoodId(goodId);
        randomPhoto.setBarcode("barcode" + goodId);
        return randomPhoto;
    }

    private EditedPhoto createNotUploadedEditedPhoto(Long goodId) {
        EditedPhoto randomPhoto = random.nextObject(EditedPhoto.class,
            "id", "uploadedTs", "uploadedPath", "lastUploadStatus");
        randomPhoto.setGoodId(goodId);
        randomPhoto.setBarcode("barcode" + goodId);
        return randomPhoto;
    }

    private void mockPhotosAndGoods() {
        RawPhoto rawPhoto1 = createNotUploadedRawPhoto(1L);
        RawPhoto rawPhoto2 = createUploadedRawPhoto(2L);
        EditedPhoto editedPhoto1 = createNotUploadedEditedPhoto(3L);
        EditedPhoto editedPhoto2 = createUploadedEditedPhoto(4L);

        rawPhotoRepository.createProcessedPhotos(Arrays.asList(rawPhoto1, rawPhoto2));
        editedPhotoRepository.createProcessedPhotos(Arrays.asList(editedPhoto1, editedPhoto2));

        Good good1 = createGood(1L);
        Good good2 = createGood(2L);
        Good good3 = createGood(3L);
        Good good4 = createGood(4L);

        when(goodService.getGoodsNoData(any(GoodFilter.class))).thenReturn(Arrays.asList(
            good1, good2, good3, good4));
    }

    private Good createGood(Long goodId) {
        return random.nextObject(Good.class)
            .setId(goodId)
            .setCleanupStatus(CleanupStatus.REQUIRED);
    }

    private RawPhoto createUploadedRawPhoto(Long goodId) {
        return random.nextObject(RawPhoto.class, "id")
            .setGoodId(goodId);
    }

    private EditedPhoto createUploadedEditedPhoto(Long goodId) {
        return random.nextObject(EditedPhoto.class, "id")
            .setGoodId(goodId);
    }
}
