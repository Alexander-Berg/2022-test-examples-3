package ru.yandex.canvas.model.elements;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.elements.Element.ColoredTextOptionsWithBackground;
import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetItem;
import ru.yandex.canvas.service.PresetsService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author skirsanov
 */
@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ElementTest {

    @Autowired
    private ValidatorFactory validatorFactory;

    @Autowired
    private PresetsService presetsService;

    private Validator validator;
    private Integer presetId;
    private String presetName;

    @Before
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        validator = validatorFactory.getValidator();
        Preset preset = presetsService.getRawUntranslatedPresets().get(0);
        presetId = preset.getId();
        PresetItem presetItem = preset.getItems().get(0);
        presetName = presetItem.getBundle().getName();

        /* These tests check localized error message, so we need to set up correct locale
         * to be able to run them on any environment
         * TODO add tanker key tag near the error message and check using it
         */
        LocaleContextHolder.setLocale(Locale.ENGLISH);

    }

    @Test
    public void testDeserializeDisclaimerElement() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();

        final String json =
                "{\"type\": \"disclaimer\", \"options\": {\"content\": \"some_content\", \"custom\": \"custom\"}}";

        try (InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            final Element element = mapper.readValue(stream, Element.class);

            assertTrue(element instanceof Disclaimer);

            Disclaimer disclaimer = (Disclaimer) element;

            assertEquals("some_content", disclaimer.getOptions().getContent());
        }
    }

    @Test
    public void testDifferentlyColoredOptions() {
        for (final ColoredTextOptionsWithBackground options : new ColoredTextOptionsWithBackground[]
                {new Special.Options(), new Button.Options()}) {

            final String oneColor = "#BBFFAA";

            options.setColor(oneColor);
            options.setBackgroundColor(oneColor);
            options.setContent("valid_content");

            Set<ConstraintViolation<ColoredTextOptionsWithBackground>> violations = validator.validate(options);

            assertEquals(2, violations.size());

            violations.forEach(violation -> {
                final String path = violation.getPropertyPath().toString();

                assertTrue(path.equals("color") || path.equals("backgroundColor"));
                assertEquals("{colors_are_the_same}", violation.getMessageTemplate());
            });
        }
    }
}
