package ru.yandex.market.clab.tms.executors;

import org.junit.Test;

import ru.yandex.market.clab.common.test.PhotoTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Picture;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.editedPhoto;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 18.06.2019
 */
public class UploadEditedPhotosExecutorToS3Test extends UploadEditedPhotosExecutorTest {

    private static final String OK = "OK";


    @Override
    public void setUp() {
        super.setUp();
        when(modelStorageService.uploadDetachedImages(any()))
            .thenReturn(ModelStorage.UploadDetachedImagesResponse.getDefaultInstance());
    }

    @Test
    public void testUploadedOk() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(1L);
        EditedPhoto photo3 = editedPhoto();
        createGoodsForPhotos(photo1, photo2, photo3);

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(createPhotoData(), createPhotoData())
        );

        when(rawPhotoS3Service.uploadEditedPhoto(any(), any(EditedPhoto.class)))
            .thenReturn("s3-path");


        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .hasSize(2)
            .allSatisfy(p ->
                assertThat(p.getS3LastUploadStatus()).isEqualTo(OK))
            .allMatch(p -> p.getS3UploadedTs() != null)
            .allMatch(p -> "s3-path".equals(p.getS3UploadedPath()));
    }

    @Test
    public void testUploadedOkIfAlreadyInMbo() {
        EditedPhoto photo1 = PhotoTestUtils.toUploadedToMbo(createNotUploadedPhoto(1L), Picture.getDefaultInstance());
        EditedPhoto photo2 = PhotoTestUtils.toUploadedToMbo(createNotUploadedPhoto(1L), Picture.getDefaultInstance());
        EditedPhoto photo3 = editedPhoto();
        createGoodsForPhotos(photo1, photo2, photo3);

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(createPhotoData(), createPhotoData())
        );

        when(rawPhotoS3Service.uploadEditedPhoto(any(), any(EditedPhoto.class)))
            .thenReturn("s3-path");


        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .hasSize(2)
            .allSatisfy(p ->
                assertThat(p.getS3LastUploadStatus()).isEqualTo(OK))
            .allMatch(p -> p.getS3UploadedTs() != null)
            .allMatch(p -> "s3-path".equals(p.getS3UploadedPath()));
    }

    @Test
    public void testUploadFailedGoodMissing() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(1L);
        EditedPhoto photo3 = editedPhoto();
        createGoodsForPhotos(photo3);

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(createPhotoData(), createPhotoData())
        );

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .hasSize(2)
            .allSatisfy(p ->
                assertThat(p.getS3LastUploadStatus()).isEqualTo("ERROR: Good 1 is missing"))
            .allMatch(p -> p.getS3UploadedTs() == null)
            .allMatch(p -> p.getS3UploadedPath() == null);
    }

    @Test
    public void testUploadFailedException() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(1L);
        EditedPhoto photo3 = editedPhoto();
        createGoodsForPhotos(photo1, photo2, photo3);

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(createPhotoData(), createPhotoData())
        );

        when(rawPhotoS3Service.uploadEditedPhoto(any(Good.class), any(EditedPhoto.class)))
            .thenThrow(new RuntimeException());

        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos)
            .hasSize(2)
            .allMatch(p -> p.getS3LastUploadStatus() != null)
            .allMatch(p -> p.getS3UploadedTs() == null)
            .allMatch(p -> p.getS3UploadedPath() == null);
    }

    private void createGoodsForPhotos(EditedPhoto... photos) {
        for (EditedPhoto photo : photos) {
            if (photo.getGoodId() == null) {
                continue;
            }

            Good good = new Good();
            good.setId(photo.getGoodId());
            goodRepository.save(good);
        }
    }

    private String createPath(EditedPhoto p) {
        return "/asd/";
    }
}
