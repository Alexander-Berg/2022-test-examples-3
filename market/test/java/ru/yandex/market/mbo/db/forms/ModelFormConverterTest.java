package ru.yandex.market.mbo.db.forms;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.forms.model.ExtendedModelForm;
import ru.yandex.market.mbo.model.forms.ModelForms;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelFormConverterTest {

    private EnhancedRandom random;

    @Before
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(16904)
            .build();
    }

    @Test
    public void testDoubleConvert() {
        for (int i = 0; i < 100; i++) {
            ExtendedModelForm modelForm = random.nextObject(ExtendedModelForm.class);
            ModelForms.ModelForm convert = ModelFormConverter.convert(modelForm);
            ExtendedModelForm modelForm2 = ModelFormConverter.convert(convert);

            Assertions.assertThat(modelForm2)
                .isEqualToComparingFieldByFieldRecursively(modelForm);
        }
    }
}
