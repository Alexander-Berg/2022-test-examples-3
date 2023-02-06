package ru.yandex.market.mboc.common.randomizers;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mboc.common.services.modelstorage.models.LocalizedString;

/**
 * @author s-ermakov
 */
public class LocalizedStringRandomizer implements Randomizer<LocalizedString> {
    private final EnhancedRandom random;

    public LocalizedStringRandomizer(long seed) {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(seed)
            .build();
    }

    @Override
    @SuppressWarnings("checkstyle:magicNumber")
    public LocalizedString getRandomValue() {
        // каждая 5 строка будет со случайным языком
        int i = random.nextInt(5);
        Language language = i == 0 ? random.nextObject(Language.class) : Language.RUSSIAN;
        return new LocalizedString(random.nextObject(String.class), language);
    }
}
