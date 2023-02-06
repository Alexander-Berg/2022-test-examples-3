package ru.yandex.market.mbo.randomizers;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.UploadedImageStatus;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class OperationStatusRandomizer implements Randomizer<OperationStatus> {
    private final EnhancedRandom random;
    private final ModelValidationErrorRandomizer validationErrorRandomizer;
    private final UploadedImageStatusRandomizer uploadedImageStatusRandomizer;
    private final SimpleCommonModelRandomizer modelRandomizer;

    public OperationStatusRandomizer(EnhancedRandom random) {
        this.random = random;
        this.validationErrorRandomizer = new ModelValidationErrorRandomizer(random);
        this.uploadedImageStatusRandomizer = new UploadedImageStatusRandomizer(random);
        this.modelRandomizer = new SimpleCommonModelRandomizer(random);
    }

    @Override
    public OperationStatus getRandomValue() {
        OperationStatusType status = random.nextObject(OperationStatusType.class);
        OperationType type = random.nextObject(OperationType.class);
        long modelId = 1 + random.nextInt(100);
        long newModelId = random.nextInt(100);
        long relatedModelId = random.nextInt(100);

        int validateErrors = random.nextInt(10);
        List<ModelValidationError> validationErrors = IntStream.range(0, validateErrors)
            .mapToObj(i -> this.validationErrorRandomizer.getRandomValue())
            .collect(Collectors.toList());

        int uploadedImageStatusesErrors = random.nextInt(10);
        List<UploadedImageStatus> uploadedImageStatuses = IntStream.range(0, uploadedImageStatusesErrors)
            .mapToObj(i -> this.uploadedImageStatusRandomizer.getRandomValue())
            .collect(Collectors.toList());

        CommonModel model = random.nextBoolean() ? modelRandomizer.getRandomNotNewValue() : null;
        String statusMessage = random.nextObject(String.class);
        String localizedMessagePattern = random.nextObject(String.class);

        OperationStatus operationStatus = new OperationStatus(
            status, statusMessage, localizedMessagePattern, type, modelId);
        operationStatus.setNewModelId(random.nextBoolean() ? newModelId : null);
        operationStatus.setRelatedModelId(random.nextBoolean() ? relatedModelId : null);
        operationStatus.setValidationErrors(validationErrors);
        operationStatus.setUploadedImageStatuses(uploadedImageStatuses);
        operationStatus.setModel(model);
        return operationStatus;
    }
}
