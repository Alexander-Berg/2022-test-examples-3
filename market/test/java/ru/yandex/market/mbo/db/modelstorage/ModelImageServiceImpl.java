package ru.yandex.market.mbo.db.modelstorage;

import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageSyncService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.image.ImageProcessingService;
import ru.yandex.market.mbo.image.ImageUploadContext;
import ru.yandex.market.mbo.image.ModelImageService;
import ru.yandex.market.mbo.utils.web.RemoteFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author york
 * @since 05.10.2017
 */
public class ModelImageServiceImpl extends ModelImageService {

    private static final int WIDTH_BASE = 200;
    private static final int WIDTH_BOUND = 300;

    private Random random = new Random(1);
    private Map<Long, List<ModelValidationError>> validationErrors = new HashMap<>();
    private ModelStorageServiceStub storageService;

    public ModelImageServiceImpl(ModelStorageServiceStub storageService,
                                 CategoryParametersServiceClient categoryService) {
        this.storageService = storageService;
        imageProcessingService = new ImageProcessingService() {

            @Nullable
            @Override
            public byte[] validateImage(RemoteFile imageFile,
                                        ImageUploadContext context,
                                        List<ModelValidationError> errorList) {
                byte[] imageBytes = imageFile.getFileBytes();
                if (imageBytes != null && imageBytes.length != 0) {
                    return imageBytes;
                } else {
                    errorList.add(new ModelValidationError(null, ModelValidationError.ErrorType.INVALID_IMAGE_SIZE)
                        .addParam(ModelStorage.ErrorParamName.IMAGE_SOURCE_URL, context.getSourceUrl()));
                    return null;
                }
            }

            @Nonnull
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile,
                                                   List<ModelValidationError> errorList) {
                Picture picture = new Picture();
                picture.setUrl(imageFile.getFileName() + random.nextInt() + "Url.jpg");
                picture.setWidth(WIDTH_BASE + random.nextInt(WIDTH_BOUND));
                picture.setHeight(WIDTH_BASE + random.nextInt(WIDTH_BOUND));
                picture.setUrlOrig(imageFile.getFileName() + "RawAvatarUrl.jpg");
                return picture;
            }

            @Nonnull
            @Override
            public Picture prepareAndUploadPicture(ImageUploadContext context, RemoteFile imageFile)
                throws OperationException {
                return prepareAndUploadPicture(context, imageFile, Collections.emptyList());
            }
        };
        ModelImageSyncService imageSyncService = new ModelImageSyncService(categoryService);
        setImageSyncService(imageSyncService);
    }

    public void setValidationResult(long modelId, List<ModelValidationError> errors) {
        validationErrors.put(modelId, errors);
    }

    @Override
    public Picture setImage(ImageUploadContext context, String imageAttrName, byte[] imageBytes,
                            String imageType, String url, OperationStatus status) {
        if (validationErrors.containsKey(context.getEntityId())) {
            status.setStatus(OperationStatusType.VALIDATION_ERROR);
            status.setStatusMessage("Validation errors occurred");
            status.addValidationErrors(validationErrors.get(context.getEntityId()));
            return null;
        }
        return super.setImage(context, imageAttrName, imageBytes, imageType, url, status);
    }

    @Override
    public CommonModel getModel(Long modelId, long categoryId, ReadStats readStats) {
        CommonModel model = storageService.getModel(categoryId, modelId).orElse(null);

        if (model != null && model.getParentModelId() != CommonModel.NO_ID) {
            // Load parent model.
            model.setParentModel(storageService.getModel(
                model.getCategoryId(), model.getParentModelId()).orElse(null));
        }

        return model;
    }

    @Override
    protected OperationStatus update(CommonModel model, ImageUploadContext imageUploadContext) {
        ModelSaveContext context = new ModelSaveContext(imageUploadContext.getUserId())
            .setOperationSource(imageUploadContext.getModificationSource());
        return storageService.saveModel(model, context).getSingleModelStatus();
    }
}
