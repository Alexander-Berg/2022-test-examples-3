package ru.yandex.market.mbo.db.modelstorage;

import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageUploadingService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.export.client.parameter.CategoryParametersServiceClient;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

/**
 * @author york
 * @since 05.10.2017
 */
public class ModelImageUploadingServiceImpl extends ModelImageUploadingService {

    public ModelImageUploadingServiceImpl(ModelStorageServiceStub storageService,
                                          CategoryParametersServiceClient categoryService) {
        super(new ModelImageServiceImpl(storageService, categoryService),
            categoryService,
            null);
    }

    @Override
    public Picture reuploadPicture(ModelSaveContext context,
                                   CommonModel model,
                                   Picture picture,
                                   OperationStatus operationStatus) {
        Picture uploaded = new Picture(picture);
        uploaded.setUrl(picture.getUrl() + "uploaded");
        return uploaded;
    }
}
