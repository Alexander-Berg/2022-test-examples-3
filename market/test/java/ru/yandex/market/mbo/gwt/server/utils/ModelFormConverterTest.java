package ru.yandex.market.mbo.gwt.server.utils;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import ru.yandex.market.mbo.db.forms.ModelFormServiceImpl;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.utils.xml.XmlSchemaValidator;

/**
 * @author s-ermakov
 */
public class ModelFormConverterTest {
    private static final long RANDOM_SEED = 1010101010101010L;
    private static final int SIZE = 100;

    private EnhancedRandom random;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .overrideDefaultInitialization(true)
            .build();
    }

    @Test
    public void testDoubleConversion() {
        for (int i = 0; i < SIZE; i++) {
            ModelForm form = random.nextObject(ModelForm.class);

            String xml = ModelFormConverter.toXml(form);
            ModelForm conversioned = ModelFormConverter.fromXml(xml);

            Assertions.assertThat(conversioned).isEqualTo(form);
        }
    }

    @Test
    public void testXsdValidation() throws SAXException {
        for (int i = 0; i < SIZE; i++) {
            ModelForm form = random.nextObject(ModelForm.class);
            String xml = ModelFormConverter.toXml(form);
            XmlSchemaValidator.validateXml(xml, ModelFormServiceImpl.MODEL_FORM_XSD_NAME);
        }
    }

    @Test
    public void testEmptyBlockXsdValidation() throws SAXException {
        ModelForm form = new ModelFormBuilder()
            .startTab("Params")

            .startBlock("block1")
            .property("param1")
            .endBlock()

            .startBlock("block2")
            .endBlock()

            .endTab()
            .getModelForm();

        String xml = ModelFormConverter.toXml(form);
        XmlSchemaValidator.validateXml(xml, ModelFormServiceImpl.MODEL_FORM_XSD_NAME);
    }
}
