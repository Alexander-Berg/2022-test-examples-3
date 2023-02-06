package ru.yandex.market.clab.common.test;

import ru.yandex.market.clab.common.mbo.ProtoUtils;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.EditedPhoto;
import ru.yandex.market.mbo.http.ModelStorage;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;

import java.time.LocalDateTime;

/**
 * @author anmalysh
 * @since 1/18/2019
 */
public class PhotoTestUtils {

    private static final long SEED = 2321321321321541L;

    private static final EnhancedRandom RANDOM = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(SEED).build();

    private PhotoTestUtils() {
    }

    public static EditedPhoto editedPhoto() {
        return editedPhoto(true);
    }

    public static EditedPhoto editedPhoto(boolean uploaded) {
        EditedPhoto photo = RANDOM.nextObject(EditedPhoto.class, "id");
        if (!uploaded) {
            return toNotUploaded(photo);
        }
        return photo;
    }

    public static EditedPhoto toNotUploaded(EditedPhoto editedPhoto) {
        return new EditedPhoto(editedPhoto)
            .setId(null)
            .setUploadedPicture(null)
            .setLastUploadStatus(null)
            .setUploadedTs(null)
            .setS3UploadedPath(null)
            .setS3LastUploadStatus(null)
            .setS3UploadedTs(null);
    }

    public static EditedPhoto toUploadedToS3(EditedPhoto newPhoto, String path) {
        return new EditedPhoto(newPhoto)
            .setS3UploadedPath(RANDOM.nextObject(String.class))
            .setS3LastUploadStatus(RANDOM.nextObject(String.class))
            .setS3UploadedTs(RANDOM.nextObject(LocalDateTime.class));
    }

    public static EditedPhoto toUploadedToMbo(EditedPhoto newPhoto, ModelStorage.Picture picture) {
        return new EditedPhoto(newPhoto)
            .setUploadedPicture(ProtoUtils.toByteArray(picture))
            .setLastUploadStatus(RANDOM.nextObject(String.class))
            .setUploadedTs(RANDOM.nextObject(LocalDateTime.class));
    }
}
