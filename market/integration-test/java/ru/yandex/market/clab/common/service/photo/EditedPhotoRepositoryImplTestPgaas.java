package ru.yandex.market.clab.common.service.photo;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.clab.common.test.PhotoTestUtils;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.clab.common.test.PhotoTestUtils.editedPhoto;

/**
 * @author anmalysh
 */
public class EditedPhotoRepositoryImplTestPgaas extends BasePgaasIntegrationTest {

    @Autowired
    private EditedPhotoRepositoryImpl editedPhotoRepository;

    @Test
    public void createEditedPhotos() {
        List<EditedPhoto> photos = Arrays.asList(
            editedPhoto(),
            editedPhoto(),
            editedPhoto()
        );

        List<EditedPhoto> newPhotos = photos.stream()
            .map(PhotoTestUtils::toNotUploaded)
            .collect(Collectors.toList());

        editedPhotoRepository.createProcessedPhotos(photos);

        List<EditedPhoto> savedPhotos = editedPhotoRepository.getNotUploadedToMboPhotos();

        assertThat(savedPhotos).allMatch(editedPhoto -> editedPhoto.getId() != null);
        assertThat(savedPhotos.stream()
            .map(editedPhoto -> new EditedPhoto(editedPhoto).setId(null)))
            .containsExactlyInAnyOrderElementsOf(newPhotos);

        // Subsequent creation shouldn't do anything
        editedPhotoRepository.createProcessedPhotos(photos);

        List<EditedPhoto> savedAgainPhotos = editedPhotoRepository.getNotUploadedToMboPhotos();
        assertThat(savedAgainPhotos).containsExactlyInAnyOrderElementsOf(savedPhotos);
    }

    @Test
    public void updateEditedPhotos() {
        List<EditedPhoto> photos = Arrays.asList(
            editedPhoto(),
            editedPhoto(),
            editedPhoto(),
            editedPhoto()
        );

        editedPhotoRepository.createProcessedPhotos(photos);

        List<EditedPhoto> savedPhotos = editedPhotoRepository.getNotUploadedToMboPhotos();

        assertThat(savedPhotos).hasSameSizeAs(photos);

        Iterator<EditedPhoto> it = savedPhotos.iterator();
        EditedPhoto uploadedBoth = it.next();
        EditedPhoto onlyInMbo = it.next();
        EditedPhoto onlyInS3 = it.next();
        EditedPhoto notUploaded = it.next();

        uploadedInMbo(uploadedBoth);
        uploadedInS3(uploadedBoth);

        uploadedInMbo(onlyInMbo);
        onlyInMbo.setS3LastUploadStatus("FAILURE");

        uploadedInS3(onlyInS3);
        onlyInS3.setLastUploadStatus("FAILURE");

        editedPhotoRepository.saveProcessedPhotos(savedPhotos);

        Set<Long> goodIds = savedPhotos.stream()
            .map(EditedPhoto::getGoodId)
            .collect(Collectors.toSet());

        List<EditedPhoto> updatedPhotos = editedPhotoRepository.getProcessedPhotos(goodIds);

        assertThat(updatedPhotos).containsExactlyInAnyOrderElementsOf(savedPhotos);

        assertThat(editedPhotoRepository.getNotUploaded(goodIds)).containsExactlyInAnyOrder(
            onlyInS3, onlyInMbo, notUploaded
        );

        assertThat(editedPhotoRepository.getNotUploadedToS3Photos()).containsExactlyInAnyOrder(
            onlyInMbo, notUploaded
        );

        assertThat(editedPhotoRepository.getNotUploadedToMboPhotos()).containsExactlyInAnyOrder(
            onlyInS3, notUploaded
        );
    }

    private void uploadedInMbo(EditedPhoto photo) {
        photo.setUploadedTs(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        photo.setLastUploadStatus("OK");
        photo.setUploadedPicture(RandomTestUtils.randomBytes());
    }

    private void uploadedInS3(EditedPhoto photo) {
        photo.setS3UploadedTs(LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));
        photo.setS3LastUploadStatus("OK");
        photo.setS3UploadedPath(RandomTestUtils.randomString());
    }
}
