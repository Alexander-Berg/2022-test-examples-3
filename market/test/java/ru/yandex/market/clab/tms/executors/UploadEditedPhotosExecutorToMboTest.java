package ru.yandex.market.clab.tms.executors;

import org.junit.Test;

import ru.yandex.market.clab.common.test.PhotoTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.editedPhoto;

/**
 * @author anmalysh
 * @since 12/12/2018
 */
public class UploadEditedPhotosExecutorToMboTest extends UploadEditedPhotosExecutorTest {

    @Test
    public void testUploadedOk() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(1L);
        EditedPhoto photo3 = editedPhoto();

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        byte[] photo1Data = createPhotoData();
        byte[] photo2Data = createPhotoData();

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(photo1Data, photo2Data)
        );

        List<ModelStorage.DetachedImageStatus> statuses = editedPhotoRepository.getProcessedPhotos(1L)
            .stream()
            .map(EditedPhoto::getId)
            .map(id -> createStatus(id, ModelStorage.OperationStatusType.OK))
            .collect(Collectors.toList());

        ModelStorage.UploadDetachedImagesResponse response = ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(statuses)
            .build();
        when(modelStorageService.uploadDetachedImages(any())).thenReturn(response);

        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos).allMatch(p -> p.getLastUploadStatus() != null);
        assertThat(updatedPhotos).allMatch(p -> p.getUploadedTs() != null);
        assertThat(updatedPhotos).allMatch(p -> assertPicture(p.getUploadedPicture()));
    }

    @Test
    public void testUploadedOkIfAlreadyInS3() {
        EditedPhoto photo1 = PhotoTestUtils.toUploadedToS3(createNotUploadedPhoto(1L), "/path1");
        EditedPhoto photo2 = PhotoTestUtils.toUploadedToS3(createNotUploadedPhoto(1L), "/path2");
        EditedPhoto photo3 = editedPhoto();

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        byte[] photo1Data = createPhotoData();
        byte[] photo2Data = createPhotoData();

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(photo1Data, photo2Data)
        );

        List<ModelStorage.DetachedImageStatus> statuses = editedPhotoRepository.getProcessedPhotos(1L)
            .stream()
            .map(EditedPhoto::getId)
            .map(id -> createStatus(id, ModelStorage.OperationStatusType.OK))
            .collect(Collectors.toList());

        ModelStorage.UploadDetachedImagesResponse response = ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(statuses)
            .build();
        when(modelStorageService.uploadDetachedImages(any())).thenReturn(response);

        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        assertThat(updatedPhotos).allMatch(p -> p.getLastUploadStatus() != null);
        assertThat(updatedPhotos).allMatch(p -> p.getUploadedTs() != null);
        assertThat(updatedPhotos).allMatch(p -> assertPicture(p.getUploadedPicture()));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testBatchUpload() {
        List<EditedPhoto> photos = Stream.generate(() -> createNotUploadedPhoto(1L))
            .limit(6)
            .collect(Collectors.toList());
        List<byte[]> photosData = Stream.generate(this::createPhotoData)
            .limit(6)
            .collect(Collectors.toList());

        editedPhotoRepository.createProcessedPhotos(photos);

        mockProcessedPhotoService(photos, photosData);

        List<ModelStorage.DetachedImageStatus> statuses = editedPhotoRepository.getProcessedPhotos(1L)
            .stream()
            .map(EditedPhoto::getId)
            .map(id -> createStatus(id, ModelStorage.OperationStatusType.OK))
            .collect(Collectors.toList());

        ModelStorage.UploadDetachedImagesResponse response = ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(statuses)
            .build();
        when(modelStorageService.uploadDetachedImages(any())).thenReturn(response);

        uploadEditedPhotosExecutor.doRealJob(null);

        verify(modelStorageService, times(2)).uploadDetachedImages(any());
    }

    @Test
    public void testUploadFailed() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(1L);
        EditedPhoto photo3 = editedPhoto();

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        byte[] photo1Data = createPhotoData();
        byte[] photo2Data = createPhotoData();

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(photo1Data, photo2Data)
        );

        List<ModelStorage.DetachedImageStatus> statuses = editedPhotoRepository.getProcessedPhotos(1L)
            .stream()
            .map(photo -> createStatus(photo.getId(), photo.getPhoto().equals(photo1.getPhoto()) ?
                ModelStorage.OperationStatusType.OK : ModelStorage.OperationStatusType.VALIDATION_ERROR))
            .collect(Collectors.toList());

        ModelStorage.UploadDetachedImagesResponse response = ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(statuses)
            .build();
        when(modelStorageService.uploadDetachedImages(any())).thenReturn(response);

        uploadEditedPhotosExecutor.doRealJob(null);

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(1L);
        EditedPhoto updatedPhoto1 = updatedPhotos.get(0);
        EditedPhoto updatedPhoto2 = updatedPhotos.get(1);

        assertThat(updatedPhoto1.getLastUploadStatus()).isNotNull();
        assertThat(updatedPhoto1.getUploadedTs()).isNotNull();
        assertPicture(updatedPhoto1.getUploadedPicture());
        assertThat(updatedPhoto2.getLastUploadStatus()).isNotNull();
        assertThat(updatedPhoto2.getUploadedTs()).isNull();
        assertThat(updatedPhoto2.getUploadedPicture()).isNull();
    }

    @Test
    public void testUploadException() {
        EditedPhoto photo1 = createNotUploadedPhoto(1L);
        EditedPhoto photo2 = createNotUploadedPhoto(2L);
        EditedPhoto photo3 = editedPhoto();

        editedPhotoRepository.createProcessedPhotos(Arrays.asList(photo1, photo2, photo3));

        byte[] photo1Data = createPhotoData();
        byte[] photo2Data = createPhotoData();

        mockProcessedPhotoService(
            Arrays.asList(photo1, photo2),
            Arrays.asList(photo1Data, photo2Data)
        );

        List<ModelStorage.DetachedImageStatus> statuses = editedPhotoRepository.getProcessedPhotos(1L)
            .stream()
            .map(EditedPhoto::getId)
            .map(id -> createStatus(id, ModelStorage.OperationStatusType.OK))
            .collect(Collectors.toList());

        ModelStorage.UploadDetachedImagesResponse response = ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(statuses)
            .build();
        when(modelStorageService.uploadDetachedImages(any())).thenAnswer(i -> {
            ModelStorage.UploadDetachedImagesRequest request = i.getArgument(0);

            if (response.getUploadedImage(0).getId() == request.getImageData(0).getId()) {
                return response;
            }
            throw new RuntimeException("Message");
        });

        uploadEditedPhotosExecutor.doRealJob(null);

        EditedPhoto updatedPhoto1 = editedPhotoRepository.getProcessedPhotos(1L).get(0);
        EditedPhoto updatedPhoto2 =  editedPhotoRepository.getProcessedPhotos(2L).get(0);

        assertThat(updatedPhoto1.getLastUploadStatus()).isNotNull();
        assertThat(updatedPhoto1.getUploadedTs()).isNotNull();
        assertPicture(updatedPhoto1.getUploadedPicture());
        assertThat(updatedPhoto2.getLastUploadStatus()).isEqualTo("Error occurred: Message");
        assertThat(updatedPhoto2.getUploadedTs()).isNull();
        assertThat(updatedPhoto2.getUploadedPicture()).isNull();
    }
}
