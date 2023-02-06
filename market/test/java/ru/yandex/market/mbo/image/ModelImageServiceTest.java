package ru.yandex.market.mbo.image;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.utils.web.RemoteFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ModelImageServiceTest {

    private String imageAttrName = "XL-Picture";
    private String imageWidthAttrName = "XLPictureSizeX";
    private String imageHeightAttrName = "XLPictureSizeY";
    private String imageColornessAttrName = "XLPictureColorness";
    private String imageColornessAvgAttrName = "XLPictureColornessAvg";

    private Map<String, List<String>> currentChangedParams = new HashMap<>();

    private String imageUrl = "IMAGE_URL";
    private Integer imageIntWidth = 123;
    private String imageStringWidth = String.valueOf(imageIntWidth);
    private Integer imageIntHeight = 456;
    private String imageStringHeight = String.valueOf(imageIntHeight);
    private Double imageDoubleColorness = 0.296;
    private String imageStringColorness = String.valueOf(imageDoubleColorness);
    private Double imageDoubleColornessAvg = 0.759;
    private String imageStringColornessAvg = String.valueOf(imageDoubleColornessAvg);

    private final long categoryId = 300;
    private final long entityId = 400;
    private final long userId = 500;

    Picture picture = new Picture();

    private ModelImageService modelImageService;

    @Before
    public void startUp() {
        currentChangedParams.put(imageAttrName, Collections.singletonList(imageUrl));
        currentChangedParams.put(imageWidthAttrName, Collections.singletonList(imageStringWidth));
        currentChangedParams.put(imageHeightAttrName, Collections.singletonList(imageStringHeight));
        currentChangedParams.put(imageColornessAttrName, Collections.singletonList(imageStringColorness));
        currentChangedParams.put(imageColornessAvgAttrName, Collections.singletonList(imageStringColornessAvg));

        picture.setXslName(imageAttrName);

        modelImageService = new ModelImageServiceImpl();
    }

    @Test
    public void testCorrectCreatePicture() {
        Picture newPicture = createPicture(imageAttrName, currentChangedParams);
        assertEquals(imageAttrName, newPicture.getXslName());
        assertEquals(imageUrl, newPicture.getUrl());
        assertEquals(imageIntWidth, newPicture.getWidth());
        assertEquals(imageIntHeight, newPicture.getHeight());
        assertEquals(imageDoubleColorness, newPicture.getColorness());
        assertEquals(imageDoubleColornessAvg, newPicture.getColornessAvg());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePictureIfUrlNotExists() {
        currentChangedParams.remove(imageAttrName);
        createPicture(imageAttrName, currentChangedParams);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePictureIfWidthNotExists() {
        currentChangedParams.remove(imageWidthAttrName);
        createPicture(imageAttrName, currentChangedParams);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreatePictureIfHeightNotExists() {
        currentChangedParams.remove(imageHeightAttrName);
        createPicture(imageAttrName, currentChangedParams);
    }

    @Test
    public void testJpegImageTypeByExtension() {
        ImageUploadContext context = new ImageUploadContext()
            .setCategoryId(categoryId)
            .setEntityId(entityId)
            .setUserId(userId);
        modelImageService.setImageProcessingService(new ImageProcessingService() {
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errors)
                throws OperationException {
                Assert.assertEquals("image/jpeg", imageFile.getFileType());
                Assert.assertEquals("" + categoryId + "_" + entityId + ".jpg", imageFile.getFileName());
                //skip post-processing
                errors.add(new ModelValidationError(entityId,
                    ModelValidationError.ErrorType.EMPTY_DATE));
                return null;
            }
        });
        modelImageService.setImage(context, imageAttrName, new byte[0], "wrong/type", "any.jpeg",
            new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE, entityId));
    }

    @Test
    public void testJpegImageTypeAndExtensions() {
        ImageUploadContext context = new ImageUploadContext();
        modelImageService.setImageProcessingService(new ImageProcessingService() {
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errors)
                throws OperationException {
                Assert.assertEquals("image/jpeg", imageFile.getFileType());
                Assert.assertTrue(imageFile.getFileName().endsWith(".jpg"));
                return null;
            }
        });
        modelImageService.uploadPicture(context, new byte[0], "image/jpeg", new ArrayList<>());
        modelImageService.uploadPicture(context, new byte[0], "jpg", new ArrayList<>());
        modelImageService.uploadPicture(context, new byte[0], "jpeg", new ArrayList<>());
    }

    @Test
    public void testPngImageTypeAndExtensions() {
        ImageUploadContext context = new ImageUploadContext();
        modelImageService.setImageProcessingService(new ImageProcessingService() {
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errors)
                throws OperationException {
                Assert.assertEquals("image/png", imageFile.getFileType());
                Assert.assertTrue(imageFile.getFileName().endsWith(".png"));
                return null;
            }
        });
        modelImageService.uploadPicture(context, new byte[0], "image/png", new ArrayList<>());
        modelImageService.uploadPicture(context, new byte[0], "png", new ArrayList<>());
    }

    @Test
    public void testImageSourceUrlForValidationErrorsNotNull() {
        ImageUploadContext context = new ImageUploadContext().setSourceUrl(null);
        modelImageService.setImageProcessingService(new ImageProcessingService() {
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errors)
                throws OperationException {
                Assert.assertNotNull(context.getSourceUrlEmptyIfNull());
                return null;
            }
        });
        modelImageService.uploadPicture(context, new byte[0], "jpg", new ArrayList<>());
    }

    @Test
    public void testNotCrushOnNullUrl() {
        ImageUploadContext context = new ImageUploadContext()
            .setCategoryId(categoryId)
            .setEntityId(entityId)
            .setUserId(userId);
        modelImageService.setImageProcessingService(new ImageProcessingService() {
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errors)
                throws OperationException {
                //skip post-processing
                errors.add(new ModelValidationError(entityId,
                    ModelValidationError.ErrorType.EMPTY_DATE));
                return null;
            }
        });
        modelImageService.setImage(context, imageAttrName, new byte[0], "wrong/type", null,
            new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE, entityId));
    }

    private Picture createPicture(
        String imageAttrName,
        Map<String, List<String>> changedParams
    ) {
        ImageType imageTypeService = ImageType.getImageType(imageAttrName);
        String widthAttrName = imageTypeService.getWidthParamName(imageAttrName);
        String heightAttrName = imageTypeService.getHeightParamName(imageAttrName);
        String urlAttrName = imageTypeService.getUrlParamName(imageAttrName);
        String colornessAttrName = imageTypeService.getColornessParamName(imageAttrName);
        String colornessAvgAttrName = imageTypeService.getColornessAvgParamName(imageAttrName);

        List<String> changedParamValue = changedParams.get(imageAttrName);
        List<String> widthParamValue = changedParams.get(widthAttrName);
        List<String> heightParamValue = changedParams.get(heightAttrName);
        List<String> urlParamValue = changedParams.get(urlAttrName);
        List<String> colornessParamValue = changedParams.get(colornessAttrName);
        List<String> colornessAvgParamValue = changedParams.get(colornessAvgAttrName);

        if (changedParamValue == null || changedParamValue.isEmpty()
            || widthParamValue == null || widthParamValue.isEmpty()
            || heightParamValue == null || heightParamValue.isEmpty()
            || colornessParamValue.isEmpty() || colornessAvgParamValue.isEmpty()) {
            throw new IllegalArgumentException("no required attributes for picture");
        }

        String imageUrl0 = changedParamValue.get(0);
        Integer imageWidth = Integer.valueOf(widthParamValue.get(0));
        Integer imageHeight = Integer.valueOf(heightParamValue.get(0));
        String sourceUrl = urlParamValue != null && urlParamValue.size() > 0 ? urlParamValue.get(0) : null;
        Double imageColorness = Double.valueOf(colornessParamValue.get(0));
        Double imageColornessAvg = Double.valueOf(colornessAvgParamValue.get(0));

        return modelImageService.createPicture(imageAttrName, imageUrl0, imageWidth, imageHeight,
            sourceUrl, imageColorness, imageColornessAvg, true);
    }


    private static class ModelImageServiceImpl extends ModelImageService {

        @Override
        public CommonModel getModel(Long modelId, long categoryId, ReadStats readStats) {
            CommonModel model = new CommonModel();
            model.setParentModelId(0);
            return model;
        }

        @Override
        protected OperationStatus update(CommonModel model, ImageUploadContext context) {
            return new OperationStatus(OperationStatusType.OK, OperationType.UPLOAD_IMAGE, model.getId());
        }
    }
}
