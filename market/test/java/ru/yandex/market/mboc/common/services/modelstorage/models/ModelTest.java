package ru.yandex.market.mboc.common.services.modelstorage.models;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.randomizers.ModelRandomizer;

/**
 * @author yuramalinov
 * @created 20.09.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelTest {
    private ModelRandomizer modelRandomizer = new ModelRandomizer(42);

    @Test
    public void testCopy() {
        for (int i = 0; i < 50; i++) {
            Model model = modelRandomizer.getRandomValue();
            Model copy = model.copy();
            Assertions.assertThat(copy).isEqualToIgnoringGivenFields(model, "parameterValues");
        }
    }
}
