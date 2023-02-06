package ru.yandex.market.mbo.core.templates.generator;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.templates.generator.ModelGeneratorType;

/**
 * @author danfertev
 * @since 20.06.2019
 */
public class ModelGeneratorServiceTest {
    private ModelGeneratorService modelGeneratorService;

    @Before
    public void setUp() {
        modelGeneratorService = new ModelGeneratorService();
    }

    @Test(expected = IllegalArgumentException.class)
    public void serviceThrowsExceptionIfNoGeneratorType() {
        modelGeneratorService.generate(ModelGeneratorType.EMPTY_PARTNER_SKU, 0L);
    }
}
