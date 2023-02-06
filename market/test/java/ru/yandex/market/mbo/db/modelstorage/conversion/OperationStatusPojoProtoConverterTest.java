package ru.yandex.market.mbo.db.modelstorage.conversion;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusConverter;
import ru.yandex.market.mbo.db.modelstorage.data.UploadedImageStatus;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.randomizers.ModelValidationErrorRandomizer;
import ru.yandex.market.mbo.randomizers.OperationStatusRandomizer;
import ru.yandex.market.mbo.randomizers.UploadedImageStatusRandomizer;

/**
 * @author s-ermakov
 */
@SuppressWarnings({"checkstyle:lineLength", "checkstyle:magicNumber"})
public class OperationStatusPojoProtoConverterTest {
    private static final long RANDOM_SEED = 2517;
    private static final int TEST_COUNT = 500;

    private EnhancedRandom random;
    private ModelValidationErrorRandomizer validationErrorRandomizer;
    private UploadedImageStatusRandomizer uploadedImageStatusRandomizer;
    private OperationStatusRandomizer operationStatusRandomizer;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .build();

        validationErrorRandomizer = new ModelValidationErrorRandomizer(random);
        uploadedImageStatusRandomizer = new UploadedImageStatusRandomizer(random);
        operationStatusRandomizer = new OperationStatusRandomizer(random);
    }

    @Test
    public void testValidationErrorDoubleConversion() {
        for (int i = 0; i < TEST_COUNT; i++) {
            ModelValidationError validationError = validationErrorRandomizer.getRandomValue();

            ModelStorage.ValidationError proto = OperationStatusConverter.convert(validationError);
            ModelValidationError validationError2 = OperationStatusConverter.convert(proto);

            Assertions.assertThat(validationError2).isEqualTo(validationError);
        }
    }

    @Test
    public void testUploadedImageStatusDoubleConversion() {
        for (int i = 0; i < TEST_COUNT; i++) {
            UploadedImageStatus imageStatus = uploadedImageStatusRandomizer.getRandomValue();

            ModelStorage.UploadedImageStatus proto = OperationStatusConverter.convert(imageStatus);
            UploadedImageStatus imageStatus2 = OperationStatusConverter.convert(proto);

            Assertions.assertThat(imageStatus2).isEqualToComparingFieldByField(imageStatus);
        }
    }

    @Test
    public void testOperationStatusDoubleConversion() {
        for (int i = 0; i < TEST_COUNT; i++) {
            OperationStatus operationStatus = operationStatusRandomizer.getRandomValue();

            ModelStorage.OperationStatus proto = OperationStatusConverter.convert(operationStatus);
            OperationStatus operationStatus2 = OperationStatusConverter.convert(proto);

            Assertions.assertThat(operationStatus2).isEqualToComparingFieldByField(operationStatus);
        }
    }
}
