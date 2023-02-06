package ru.yandex.market.mbo.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.common.imageservice.UploadImageException;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.core.images.HyperImageDepotService;
import ru.yandex.market.mbo.core.kdepot.services.validation.ValidationResult;
import ru.yandex.market.mbo.core.kdepot.services.validation.Validator;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageValidationParams;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.MboImageValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.test.InjectResource;
import ru.yandex.market.mbo.test.InjectResources;
import ru.yandex.market.mbo.utils.web.RemoteFile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 03.07.2017
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class ImageProcessingServiceTest {

    @Rule
    public InjectResources resource = new InjectResources(this);

    @InjectResource("/mbo-core/test-image-1.jpeg")
    private byte[] originalImage;

    @InjectResource("/mbo-core/test-image-2.jpeg")
    private byte[] transformedImage;

    @InjectResource("/mbo-core/test-image-3.jpeg")
    private byte[] nonWhiteBackgroundImage;

    @InjectResource("/mbo-core/test-image-bad-exif.jpg")
    private byte[] badExifImage;

    @Mock
    private ImageTransformer transformer;

    @Mock
    private HyperImageDepotService hyperImageDepotService;

    private Validator<ValidationResult, ImageValidationParams> validator = params -> ValidationResult.VALID;

    private ImageProcessingService service;

    private ImageUploadContext imageUploadContext;

    private RemoteFile imageFile;

    @Before
    public void init() throws Exception {
        service = new ImageProcessingService();

        when(transformer.transform(eq(originalImage), anyInt(), any(), eq("jpeg"))).thenReturn(transformedImage);

        when(hyperImageDepotService.addImage(eq(originalImage), any(), any()))
            .thenReturn(new HyperImageDepotService.ImageInfo("//origin", 0.5, 0.42));
        when(hyperImageDepotService.addImage(eq(transformedImage), any(), any()))
            .thenReturn(new HyperImageDepotService.ImageInfo("//transform", 0.1, 0.05));
        service.setHyperImageDepotService(hyperImageDepotService);
        service.setImageTransformers(Collections.singletonList(transformer));
        service.setImageValidator(validator);

        imageUploadContext = new ImageUploadContext();
        imageUploadContext.setSourceUrl("test.jpeg");
        imageFile = new RemoteFile(originalImage, "jpeg", "test.jpeg");
    }

    @Test
    public void validationErrorWhenUploadingFailed() {
        when(hyperImageDepotService.addImage(eq(originalImage), any(), any()))
            .thenThrow(new UploadImageException());

        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, imageFile, errors);

        assertThat(errors, hasSize(1));
        ModelValidationError error = errors.get(0);
        assertThat(error.getType(), is(ModelValidationError.ErrorType.UNKNOWN_IMAGE_ERROR));
        assertThat(error.getParams(),
                hasEntry(ModelStorage.ErrorParamName.DESCRIPTION,
                    "Ошибка загрузки картинки в хранилище."));
        assertThat(error.getParams(), hasEntry(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL, "test.jpeg"));
    }

    @Test
    public void exceptionWhenUploadingFailed() {
        when(hyperImageDepotService.addImage(eq(originalImage), any(), any()))
            .thenThrow(new UploadImageException());

        try {
            service.prepareAndUploadPicture(imageUploadContext, imageFile);

            fail("Expected exception");
        } catch (OperationException e) {
            assertThat(e.getDetailedErrors(), hasSize(1));
        }

    }

    @Test
    public void testPictureUpload() {
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, imageFile);

        Assert.assertEquals("//transform", picture.getUrl());
        Assert.assertEquals("//origin", picture.getUrlOrig());
        Assert.assertEquals(Double.valueOf(0.1), picture.getColorness());
        Assert.assertEquals(Double.valueOf(0.05), picture.getColornessAvg());
    }

    @Test
    public void testIsWhiteBackgroundComputedCorrectlyAsTrue() {
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, imageFile);
        Assertions.assertThat(picture.isWhiteBackground()).isTrue();
    }

    @Test
    public void testIsWhiteBackgroundComputedCorrectlyAsFalse() throws Exception {
        when(transformer.transform(eq(nonWhiteBackgroundImage), anyInt(), any(), eq("jpeg")))
            .thenReturn(nonWhiteBackgroundImage);
        when(hyperImageDepotService.addImage(eq(nonWhiteBackgroundImage), any(), any()))
            .thenReturn(new HyperImageDepotService.ImageInfo("//origin", 0.5, 0.42));
        RemoteFile blackImageFile = new RemoteFile(nonWhiteBackgroundImage, "jpeg", "test.jpeg");
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, blackImageFile);
        Assertions.assertThat(picture.isWhiteBackground()).isFalse();
    }

    @Test
    public void testSingleUploadCallIfNoTransformation() throws Exception {
        when(transformer.transform(eq(originalImage), anyInt(), any(), eq("jpeg")))
            .thenReturn(Arrays.copyOf(originalImage, originalImage.length));

        Picture picture = service.prepareAndUploadPicture(imageUploadContext, imageFile);

        Assert.assertEquals("//origin", picture.getUrl());
        Assert.assertEquals("//origin", picture.getUrlOrig());
        Assert.assertEquals(Double.valueOf(0.5), picture.getColorness());
        Assert.assertEquals(Double.valueOf(0.42), picture.getColornessAvg());

        verify(hyperImageDepotService, only()).addImage(any(), anyString(), anyString());
    }

    @Test
    public void testValidationException() {
        service.setImageValidator(new MboImageValidator());

        try {
            service.prepareAndUploadPicture(imageUploadContext, imageFile);
        } catch (OperationException e) {
            List<ProcessingResult> error = e.getDetailedErrors();
            Assert.assertEquals(1, error.size());
            return;
        }

        fail("Expected validation error");
    }

    @Test
    public void testValidationError() {
        service.setImageValidator(params -> ValidationResult.INVALID);

        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, imageFile, errors);

        Assert.assertEquals(1, errors.size());
        ModelValidationError error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());

        errors = new ArrayList<>();
        byte[] processedImage = service.validateImage(imageFile, imageUploadContext, errors);

        Assert.assertNotNull(processedImage);
        Assert.assertEquals(1, errors.size());
        error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());
    }

    @Test
    public void testPreliminaryValidationError() {
        service.setPreliminaryImageValidator(params -> ValidationResult.INVALID);

        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, imageFile, errors);

        Assert.assertEquals(1, errors.size());
        ModelValidationError error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());


        errors = new ArrayList<>();
        byte[] processedImage = service.validateImage(imageFile, imageUploadContext, errors);

        Assert.assertNull(processedImage);
        Assert.assertEquals(1, errors.size());
        error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());
    }

    @Test
    public void testEmptyImageError() {
        RemoteFile emptyFile = new RemoteFile(new byte[0], "jpeg", "test.jpeg");
        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, emptyFile, errors);

        Assert.assertEquals(1, errors.size());

        ModelValidationError error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_SIZE, error.getType());
        Assert.assertEquals(ValidationResult.EMPTY_FILE.getDescription(),
            error.getParam(ModelStorage.ErrorParamName.DESCRIPTION).get());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());


        errors = new ArrayList<>();
        byte[] processedImage = service.validateImage(emptyFile, imageUploadContext, errors);

        Assert.assertNull(processedImage);
        Assert.assertEquals(1, errors.size());
        error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_SIZE, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());
    }

    @Test
    public void testNullImageError() {
        RemoteFile nullFile = new RemoteFile(null, "jpeg", "test.jpeg");
        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, nullFile, errors);

        Assert.assertEquals(1, errors.size());

        ModelValidationError error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_SIZE, error.getType());
        Assert.assertEquals(ValidationResult.EMPTY_FILE.getDescription(),
            error.getParam(ModelStorage.ErrorParamName.DESCRIPTION).get());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());

        errors = new ArrayList<>();
        byte[] processedImage = service.validateImage(nullFile, imageUploadContext, errors);

        Assert.assertNull(processedImage);
        Assert.assertEquals(1, errors.size());
        error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_SIZE, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());

    }

    @Test
    public void validationErrorWhenTransformFailed() throws Exception {
        when(transformer.transform(eq(originalImage), anyInt(), any(), eq("jpeg")))
            .thenThrow(new MarketImageScaler.ScaleException("Fail"));

        List<ModelValidationError> errors = new ArrayList<>();
        service.prepareAndUploadPicture(imageUploadContext, imageFile, errors);

        Assert.assertEquals(1, errors.size());

        ModelValidationError error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("Fail", error.getParam(ModelStorage.ErrorParamName.DESCRIPTION).get());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());

        errors = new ArrayList<>();
        byte[] processedImage = service.validateImage(imageFile, imageUploadContext, errors);

        Assert.assertNull(processedImage);
        Assert.assertEquals(1, errors.size());
        error = errors.get(0);
        Assert.assertEquals(ModelValidationError.ErrorType.INVALID_IMAGE_FORMAT, error.getType());
        Assert.assertEquals("test.jpeg", error.getParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL).get());
    }

    @Test
    public void notCrashOnBadExifInImage() throws Exception {
        when(transformer.transform(eq(badExifImage), anyInt(), any(), eq("jpeg")))
            .thenReturn(badExifImage);

        when(hyperImageDepotService.addImage(eq(badExifImage), any(), any()))
            .thenReturn(new HyperImageDepotService.ImageInfo("//origin", 0.5, 0.42));
        RemoteFile badExifFile = new RemoteFile(badExifImage, "jpeg", "test.jpeg");
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, badExifFile);
        Assertions.assertThat(picture).isNotNull();
        List<ModelValidationError> errors = new ArrayList<>();
        byte[] res = service.validateImage(badExifFile, imageUploadContext, errors);
        assertNotNull(res);
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void storeSourceUrl() {
        imageUploadContext.setSourceUrl("//source-url");
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, imageFile);

        Assert.assertEquals("//source-url", picture.getUrlSource());
    }

    @Test
    public void testWhenSourceUrlIsNull() {
        imageUploadContext.setSourceUrl(null);
        Picture picture = service.prepareAndUploadPicture(imageUploadContext, imageFile);

        Assert.assertEquals(null, picture.getUrlSource());
    }

    @Test
    public void testImageValidation() {
        List<ModelValidationError> errors = new ArrayList<>();

        byte[] processedImage = service.validateImage(imageFile, imageUploadContext, errors);
        Assert.assertTrue(errors.isEmpty());
        Assert.assertEquals(transformedImage, processedImage);
    }

}
