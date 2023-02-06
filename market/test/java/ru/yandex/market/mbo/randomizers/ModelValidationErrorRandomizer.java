package ru.yandex.market.mbo.randomizers;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;

/**
 * @author s-ermakov
 */
public class ModelValidationErrorRandomizer implements Randomizer<ModelValidationError> {
    private final EnhancedRandom random;

    public ModelValidationErrorRandomizer(EnhancedRandom random) {
        this.random = random;
    }

    @Override
    public ModelValidationError getRandomValue() {
        long modelId = random.nextInt(100);
        ModelValidationError.ErrorType errorType = random.nextObject(ModelValidationError.ErrorType.class);
        ModelValidationError.ErrorSubtype errorSubtype = random.nextObject(ModelValidationError.ErrorSubtype.class);
        boolean critical = random.nextBoolean();
        boolean allowForce = random.nextBoolean();

        return new ModelValidationError(modelId, errorType, errorSubtype, critical, allowForce);
    }
}
