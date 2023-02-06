package ru.yandex.market.mbo.randomizers;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.db.modelstorage.data.UploadedImageStatus;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * @author s-ermakov
 */
public class UploadedImageStatusRandomizer implements Randomizer<UploadedImageStatus> {

    private final EnhancedRandom random;

    public UploadedImageStatusRandomizer(EnhancedRandom random) {
        this.random = random;
    }

    @Override
    public UploadedImageStatus getRandomValue() {
        long id = random.nextInt(100);
        String url = random.nextObject(String.class);
        String internalImageUrl = random.nextObject(String.class);
        return new UploadedImageStatus(id, url, XslNames.XL_PICTURE, internalImageUrl);
    }
}
