package ru.yandex.market.clab.tms.executors;

import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.clab.common.service.barcode.SsBarcodeRepository;
import ru.yandex.market.clab.common.service.good.GoodRepositoryStub;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.common.service.good.GoodServiceImpl;
import ru.yandex.market.clab.common.service.nas.PhotoService;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepository;
import ru.yandex.market.clab.common.service.photo.EditedPhotoRepositoryStub;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.tms.service.RawPhotoS3Service;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.editedPhoto;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class UploadEditedPhotosExecutorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    protected UploadEditedPhotosExecutor uploadEditedPhotosExecutor;

    private ComplexMonitoring monitoring = new ComplexMonitoring();

    protected EditedPhotoRepository editedPhotoRepository = new EditedPhotoRepositoryStub();

    @Mock
    protected PhotoService photoService;

    @Mock
    protected ModelStorageService modelStorageService;

    protected GoodService goodService;
    protected GoodRepositoryStub goodRepository;

    @Mock
    protected RawPhotoS3Service rawPhotoS3Service;

    private EnhancedRandom random;

    private static final long SEED = 873876236823659623L;

    @Before
    public void setUp() {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();
        goodRepository = new GoodRepositoryStub();
        goodService = new GoodServiceImpl(goodRepository, mock(SsBarcodeRepository.class));
        uploadEditedPhotosExecutor = new UploadEditedPhotosExecutor(
            monitoring, editedPhotoRepository, photoService, modelStorageService, goodService, rawPhotoS3Service);
    }

    protected boolean assertPicture(byte[] data) {
        assertThat(data).isNotNull();
        try {
            ModelStorage.Picture picture = ModelStorage.Picture.parseFrom(data);
            assertThat(picture.getValueSource()).isEqualTo(ModelStorage.ModificationSource.CONTENT_LAB);
        } catch (InvalidProtocolBufferException e) {
            fail("Failed to parse picture", e);
        }
        return true;
    }

    protected void mockProcessedPhotoService(List<EditedPhoto> photos, List<byte[]> datas) {
        when(photoService.readProcessedPhotoEdited(anyString(), anyLong(), anyString())).thenAnswer(i -> {
            String barcode = i.getArgument(0);
            String photo = i.getArgument(2);
            for (int index = 0; index < photos.size(); index++) {
                EditedPhoto p = photos.get(index);
                if (barcode.equals(p.getBarcode()) && photo.equals(p.getPhoto())) {
                    return datas.get(index);
                }
            }
            fail("readProcessedPhoto called unexpectedly");
            return null;
        });
    }

    protected EditedPhoto createNotUploadedPhoto(Long goodId) {
        return editedPhoto(false)
            .setGoodId(goodId)
            .setBarcode("barcode" + goodId);
    }

    protected ModelStorage.DetachedImageStatus createStatus(Long id, ModelStorage.OperationStatusType type) {
        return ModelStorage.DetachedImageStatus.newBuilder()
            .setId(id)
            .setPicture(ModelStorage.Picture.newBuilder()
                .build())
            .setStatus(ModelStorage.OperationStatus.newBuilder()
                .setStatus(type)
                .setType(ModelStorage.OperationType.CREATE)
                .build())
            .build();
    }

    protected byte[] createPhotoData() {
        byte[] data = new byte[100];
        random.nextBytes(data);
        return data;
    }
}
