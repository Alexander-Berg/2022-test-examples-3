package ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket;

import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AvatarImageUploaderTest {

    private static final List<PictureContents> PICS = Collections.singletonList(
        new PictureContents(new byte[]{}, "image", "imageUrl1")
    );
    private final ModelStorageService modelStorage = mock(ModelStorageService.class);
    private final AvatarImageUploader avatarImageUploader = new AvatarImageUploader(modelStorage);

    @Test
    public void validationWithUnknownImageErrorShouldCreateProblem() {
        ModelStorage.UploadDetachedImagesResponse response = createResponseWithValidationType(
            ModelStorage.ValidationErrorType.UNKNOWN_IMAGE_ERROR
        );
        doReturn(response).when(modelStorage).uploadDetachedImages(any());

        ArrayList<String> problems = new ArrayList<>();
        Map<PictureContents, AvatarImageUploader.Result> results = avatarImageUploader.uploadImages(PICS, problems);

        assertThat(results.values()).containsOnlyNulls();
        assertThat(problems).hasSize(1);
    }

    @Test
    public void regularValidationErrorShouldNotCreateProblem() {
        ModelStorage.UploadDetachedImagesResponse response = createResponseWithValidationType(
            ModelStorage.ValidationErrorType.INVALID_IMAGE_FORMAT
        );
        doReturn(response).when(modelStorage).uploadDetachedImages(any());

        ArrayList<String> problems = new ArrayList<>();
        Map<PictureContents, AvatarImageUploader.Result> results = avatarImageUploader.uploadImages(PICS, problems);

        assertThat(results).hasSize(1);
        assertThat(problems).isEmpty();
    }

    private ModelStorage.UploadDetachedImagesResponse createResponseWithValidationType(
        ModelStorage.ValidationErrorType validationErrorType
    ) {
        return ModelStorage.UploadDetachedImagesResponse.newBuilder()
            .addUploadedImage(
                ModelStorage.DetachedImageStatus.newBuilder()
                    .setStatus(ModelStorage.OperationStatus.newBuilder()
                        .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                        .addValidationError(ModelStorage.ValidationError.newBuilder()
                            .setType(validationErrorType)
                            .build())
                        .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR))
                    .build())
            .build();
    }

}