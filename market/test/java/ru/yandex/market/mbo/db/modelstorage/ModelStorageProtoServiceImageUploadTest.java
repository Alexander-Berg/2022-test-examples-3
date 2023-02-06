package ru.yandex.market.mbo.db.modelstorage;

import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.image.ImageData;
import ru.yandex.market.mbo.db.modelstorage.image.ImageDownloader;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageUploadingService;
import ru.yandex.market.mbo.db.modelstorage.image.ParallelImageProcessingService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatusType;
import ru.yandex.market.mbo.image.ModelImageService;
import ru.yandex.market.mbo.test.InjectResource;
import ru.yandex.market.mbo.test.InjectResources;
import ru.yandex.market.mbo.user.AutoUser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 09.02.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelStorageProtoServiceImageUploadTest {

    private static final String IMAGE_JPEG = "image/jpeg";
    private static final String SOURCE_URL = "http://some-source-url/";
    private static final int WIDTH = 404;
    private static final int HEIGHT = 707;
    private static final long PICTURE_ID = 889;
    private static final int AUTO_USER = 998877;
    private static final String UPLOADED_URL = "http://avatars/img_009/orig";
    private static final String UPLOADED_URL_ORIG = "http://avatars/img_120/orig";
    private static final int THREE = 3;
    private static final int TREE = 3;

    @Rule
    public InjectResources resource = new InjectResources(this);

    @InjectResource("/mbo-core/test-image-1.jpeg")
    private byte[] picture1bytes;

    @InjectResource("/mbo-core/test-image-2.jpeg")
    private byte[] picture2bytes;

    @Mock
    private ModelImageService modelImageService;

    @Mock
    private ModelImageUploadingService modelImageUploadingService;

    @Mock
    private ModelStorageHealthService modelStorageHealthService;

    private ModelStorageProtoService service;
    private Picture picture;

    @Before
    public void before() throws Exception {
        service = new ModelStorageProtoService();
        service.setModelImageService(modelImageService);
        service.setModelImageUploadingService(modelImageUploadingService);
        service.setAutoUser(new AutoUser(AUTO_USER));
        service.setModelStorageHealthService(modelStorageHealthService);

        ParallelImageProcessingService parallelImageProcessingService = new ParallelImageProcessingService();
        parallelImageProcessingService.setThreadCount(1);
        parallelImageProcessingService.afterPropertiesSet();
        service.setParallelImageProcessingService(parallelImageProcessingService);

        picture = new Picture();
        picture.setWidth(WIDTH);
        picture.setHeight(HEIGHT);
        picture.setUrl(UPLOADED_URL);
        picture.setUrlOrig(UPLOADED_URL_ORIG);
    }

    @Test
    public void successSingleUpload() {
        ModelStorage.UploadDetachedImagesRequest request = ModelStorage.UploadDetachedImagesRequest.newBuilder()
            .addImageData(imageData().setId(PICTURE_ID))
            .build();

        when(modelImageService.uploadPicture(any(), aryEq(picture1bytes), eq(IMAGE_JPEG), anyList()))
            .thenReturn(picture);


        ModelStorage.UploadDetachedImagesResponse response = service.uploadDetachedImages(request);


        assertThat(response, is(notNullValue()));
        assertThat(response.getUploadedImageCount(), is(1));
        ModelStorage.DetachedImageStatus status = response.getUploadedImageList().get(0);

        assertThat(status.getId(), is(PICTURE_ID));
        assertThat(status.hasStatus(), is(true));
        assertThat(status.getStatus().getStatus(), is(OperationStatusType.OK));
    }

    @Test
    public void testDownloadFromUrl() throws IOException {
        ModelStorage.UploadDetachedImagesRequest request = ModelStorage.UploadDetachedImagesRequest.newBuilder()
            .addImageData(
                ModelStorage.ImageData.newBuilder()
                    .setUrl("some url")
                    .setContentType("hzhz")
            )
            .build();

        ImageDownloader imageDownloader = Mockito.mock(ImageDownloader.class);
        when(imageDownloader.downloadImage("some url"))
            .thenReturn(new ImageData(picture1bytes, IMAGE_JPEG));

        when(modelImageUploadingService.getImageDownloader()).thenReturn(imageDownloader);

        when(modelImageService.uploadPicture(any(), aryEq(picture1bytes), eq(IMAGE_JPEG), anyList()))
            .thenReturn(picture);

        ModelStorage.UploadDetachedImagesResponse response = service.uploadDetachedImages(request);

        assertThat(response, is(notNullValue()));
        assertThat(response.getUploadedImageCount(), is(1));
        ModelStorage.DetachedImageStatus status = response.getUploadedImageList().get(0);

        assertThat(status.hasStatus(), is(true));
        assertThat(status.getStatus().getStatus(), is(OperationStatusType.OK));
    }

    @Test
    public void successMultipleUpload() {
        ModelStorage.UploadDetachedImagesRequest request = ModelStorage.UploadDetachedImagesRequest.newBuilder()
            .addImageData(imageData().setId(PICTURE_ID))
            .addImageData(imageData().setId(PICTURE_ID + 1))
            .addImageData(imageData().setId(PICTURE_ID + 2))
            .build();

        when(modelImageService.uploadPicture(any(), any(), eq(IMAGE_JPEG), anyList()))
            .thenAnswer((Answer<Picture>) invocation -> new Picture(picture));


        ModelStorage.UploadDetachedImagesResponse response = service.uploadDetachedImages(request);

        assertThat(response.getUploadedImageCount(), is(TREE));

        int n = 0;
        for (ModelStorage.DetachedImageStatus status : response.getUploadedImageList()) {
            assertThat("picture " + n + " id equals", status.getId(), is(PICTURE_ID + n));
            assertThat("picture " + n + " status ok", status.getStatus().getStatus(),
                is(OperationStatusType.OK));
            assertThat("picture " + n + " type is upload", status.getStatus().getType(),
                is(ModelStorage.OperationType.UPLOAD_IMAGE));

            assertThat("picture " + n + " has picture", status.hasPicture(), is(true));
            ModelStorage.Picture receivedPicture = status.getPicture();
            assertThat("picture " + n + " url", receivedPicture.getUrl(), is(UPLOADED_URL));
            assertThat("picture " + n + " width", receivedPicture.getWidth(), is(WIDTH));
            assertThat("picture " + n + " height", receivedPicture.getHeight(), is(HEIGHT));
            n++;
        }
    }

    @Test
    public void failedMultipleUpload() {
        ModelStorage.UploadDetachedImagesRequest request = ModelStorage.UploadDetachedImagesRequest.newBuilder()
            .addImageData(imageData().setId(PICTURE_ID))
            .addImageData(imageData().setId(PICTURE_ID + 1).setContentBytes(ByteString.copyFrom(picture2bytes)))
            .addImageData(imageData().setId(PICTURE_ID + 2))
            .build();

        // throw error on second picture
        when(modelImageService.uploadPicture(any(), any(), eq(IMAGE_JPEG), anyList()))
            .thenAnswer((Answer<Picture>) (InvocationOnMock invocation) -> {
                byte[] bytes = invocation.getArgument(1);
                if (Arrays.equals(bytes, picture2bytes)) {
                    throw new RuntimeException("something went wrong");
                } else {
                    return new Picture(picture);
                }
            });

        ModelStorage.UploadDetachedImagesResponse response = service.uploadDetachedImages(request);

        assertThat(response.getUploadedImageCount(), is(THREE));
        List<ModelStorage.DetachedImageStatus> images = response.getUploadedImageList();
        assertThat(images.get(0).getStatus().getStatus(), is(OperationStatusType.OK));
        assertThat(images.get(1).getStatus().getStatus(), is(OperationStatusType.INTERNAL_ERROR));
        assertThat(images.get(1).getStatus().getStatusMessage(), is("something went wrong"));
        assertThat(images.get(2).getStatus().getStatus(), is(OperationStatusType.OK));
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void validationError() {
        ModelStorage.UploadDetachedImagesRequest request = ModelStorage.UploadDetachedImagesRequest.newBuilder()
            .addImageData(imageData().setId(PICTURE_ID))
            .addImageData(imageData().setId(PICTURE_ID + 1).setContentBytes(ByteString.copyFrom(picture2bytes)))
            .addImageData(imageData().setId(PICTURE_ID + 2))
            .build();

        // fail validation for second picture
        when(modelImageService.uploadPicture(any(), any(), eq(IMAGE_JPEG), anyList()))
            .thenAnswer((Answer<Picture>) (InvocationOnMock invocation) -> {
                byte[] bytes = invocation.getArgument(1);
                if (Arrays.equals(bytes, picture2bytes)) {
                    List<ModelValidationError> errors = invocation.getArgument(3);
                    errors.add(new ModelValidationError(0L, ModelValidationError.ErrorType.INVALID_IMAGE_SIZE));
                    return null;
                }
                return new Picture(picture);
            });


        ModelStorage.UploadDetachedImagesResponse response = service.uploadDetachedImages(request);


        assertThat(response.getUploadedImageCount(), is(THREE));
        List<ModelStorage.DetachedImageStatus> images = response.getUploadedImageList();
        assertThat("first is ok", images.get(0).getStatus().getStatus(), is(OperationStatusType.OK));

        ModelStorage.OperationStatus failed = images.get(1).getStatus();
        assertThat("middle has validation error status", failed.getStatus(), is(OperationStatusType.VALIDATION_ERROR));
        assertThat("middle got validation error details", failed.getValidationErrorCount(), is(1));
        assertThat("middle got right details",
            failed.getValidationError(0).getType(), is(ModelStorage.ValidationErrorType.INVALID_IMAGE_SIZE));

        assertThat("last is ok", images.get(2).getStatus().getStatus(), is(OperationStatusType.OK));
    }

    private ModelStorage.ImageData.Builder imageData() {
        return ModelStorage.ImageData.newBuilder()
            .setContentBytes(ByteString.copyFrom(picture1bytes))
            .setContentType(IMAGE_JPEG)
            .setUrl(SOURCE_URL + "/image1");
    }

}

