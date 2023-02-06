package ru.yandex.market.mbo.randomizers;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SimpleCommonModelRandomizer implements Randomizer<CommonModel> {

    private final EnhancedRandom random;

    public SimpleCommonModelRandomizer(EnhancedRandom random) {
        this.random = random;
    }

    @Override
    public CommonModel getRandomValue() {
        long modelId = random.nextInt(100);
        long categoryId = random.nextInt(100);
        long vendorId = random.nextInt(100);
        CommonModel.Source source = random.nextObject(CommonModel.Source.class);
        CommonModel.Source type = random.nextObject(CommonModel.Source.class);

        CommonModel model = new CommonModel();
        model.setId(modelId);
        model.setCategoryId(categoryId);
        model.setLocalVendorId(vendorId);
        model.setSource(source);
        model.setCurrentType(type);
        return model;
    }

    public CommonModel getRandomNotNewValue() {
        CommonModel model = getRandomValue();
        model.setId(1 + random.nextInt(99));
        return model;
    }
}
