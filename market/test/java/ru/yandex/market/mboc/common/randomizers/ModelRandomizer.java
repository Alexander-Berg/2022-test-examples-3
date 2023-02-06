package ru.yandex.market.mboc.common.randomizers;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.market.mboc.common.services.modelstorage.models.LocalizedString;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;

/**
 * @author s-ermakov
 */
public class ModelRandomizer implements Randomizer<Model> {
    private final EnhancedRandom random;

    public ModelRandomizer(long seed) {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(seed)
            .overrideDefaultInitialization(true)
            .randomize(LocalizedString.class, new LocalizedStringRandomizer(seed))
            .build();
    }

    @Override
    @SuppressWarnings("checkstyle:magicNumber")
    public Model getRandomValue() {
        Model model = random.nextObject(Model.class, "parameterValues");
        if (model.getId() <= 0) {
            model.setId(1 + random.nextInt(10000));
        }
        return model;
    }
}
