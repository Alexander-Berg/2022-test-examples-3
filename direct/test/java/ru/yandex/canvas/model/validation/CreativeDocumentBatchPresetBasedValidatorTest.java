package ru.yandex.canvas.model.validation;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.canvas.config.CanvasTest;
import ru.yandex.canvas.model.Bundle;
import ru.yandex.canvas.model.CreativeData;
import ru.yandex.canvas.model.CreativeDocument;
import ru.yandex.canvas.model.CreativeDocumentBatch;
import ru.yandex.canvas.model.elements.Button;
import ru.yandex.canvas.model.elements.Description;
import ru.yandex.canvas.model.elements.Disclaimer;
import ru.yandex.canvas.model.elements.Domain;
import ru.yandex.canvas.model.elements.Element;
import ru.yandex.canvas.model.elements.Fade;
import ru.yandex.canvas.model.elements.Headline;
import ru.yandex.canvas.model.elements.Legal;
import ru.yandex.canvas.model.elements.Phone;
import ru.yandex.canvas.model.elements.Special;
import ru.yandex.canvas.model.presets.Preset;
import ru.yandex.canvas.model.presets.PresetItem;
import ru.yandex.canvas.service.PresetsService;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.canvas.model.elements.ElementType.BUTTON;
import static ru.yandex.canvas.model.elements.ElementType.DESCRIPTION;
import static ru.yandex.canvas.model.elements.ElementType.DISCLAIMER;
import static ru.yandex.canvas.model.elements.ElementType.DOMAIN;
import static ru.yandex.canvas.model.elements.ElementType.FADE;
import static ru.yandex.canvas.model.elements.ElementType.HEADLINE;
import static ru.yandex.canvas.model.elements.ElementType.LEGAL;
import static ru.yandex.canvas.model.elements.ElementType.PHONE;
import static ru.yandex.canvas.model.elements.ElementType.SPECIAL;
import static ru.yandex.canvas.steps.CreativeDocumentSteps.createEmptyCreativeDocument;

@CanvasTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeDocumentBatchPresetBasedValidatorTest {
    @Autowired
    private ValidatorFactory validatorFactory;

    @Autowired
    private PresetsService presetsService;

    private Validator validator;
    private Integer presetId;
    private String presetName;
    private Headline headlineElement;

    @Before
    public void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        validator = validatorFactory.getValidator();
        Preset preset = presetsService.getRawUntranslatedPresets().get(0);
        presetId = preset.getId();
        PresetItem presetItem = preset.getItems().get(0);
        presetName = presetItem.getBundle().getName();
        headlineElement = StreamEx.of(presetItem.getElements()).select(Headline.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unable to find headline element, use another preset"));
    }

    @Test
    public void testPresetNotFound_returnError() {
        Bundle bundle = new Bundle();
        bundle.setName("no_such_name");
        CreativeData creativeData = new CreativeData();
        creativeData.setBundle(bundle);
        CreativeDocument creative = new CreativeDocument();
        creative.setData(creativeData);
        CreativeDocumentBatch batch = new CreativeDocumentBatch();
        batch.setItems(singletonList(creative));

        Set<ConstraintViolation<CreativeDocumentBatch>> result = validator.validate(batch);

        String expectedPath = "items[0].presetId";
        String expectedMessage = "Preset not found";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testPresetIdIsNull_noError() {
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validator.validate(createEmptyBatch(presetName, null));

        assertThat(result, empty());
    }

    @Test
    public void testPresetIdIsNull_presetIdFilled() {
        CreativeDocumentBatch batch = createEmptyBatch(presetName, null);
        validator.validate(batch);

        assertThat(batch.getItems().get(0).getPresetId(), is(presetId));
    }

    @Test
    public void testGenericPreset_textTooLong_returnError() {
        headlineElement.setMaxLength(60);
        Headline headline = createHeadline(70, "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(headline);
        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Text length must be between 1 and 60 symbols";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testPhoneValidation_valid() {
        Phone phone = createPhone(StringUtils.repeat("1", 14), "#00AABB");
        assertThat(validate(phone, 11), empty());
    }

    @Test
    public void testPhoneValidation_startsWithPlus_valid() {
        Phone phone = createPhone("+" + StringUtils.repeat("1", 14), "#00AABB");
        assertThat(validate(phone, 11), empty());
    }

    @Test
    public void testPhoneValidation_invalidChars() {
        Phone phone = createPhone(StringUtils.repeat("a", 14), "#00AABB");
        var result = validate(phone, 11);

        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Invalid format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testPhoneValidation_tooLong_returnsError() {
        Phone phone = createPhone(StringUtils.repeat("1", 22), "#00AABB");
        var result = validate(phone, 11);

        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Invalid format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testPhoneValidation_invalidColor_returnsError() {
        Phone phone = createPhone(StringUtils.repeat("1", 14), "#00AABZ");
        var result = validate(phone, 11);

        String expectedPath = "items[0].data.elements[0].options.color";
        String expectedMessage = "Invalid color format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testLegalOptionsValidation() {
        Legal legal = createLegal(StringUtils.repeat('*', 700), "#0000BB", "#0000BB");
        assertThat(validate(legal), empty());
    }

    @Test
    public void testLegalOptionsValidation_tooLong_returnsError() {
        Legal legal = createLegal(StringUtils.repeat('*', 701), "#0000BB", "#0000BB");
        var result = validate(legal);

        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Text length must be between 1 and 700 symbols";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testButtonOptionsValidation() {
        Button button = createButton(StringUtils.repeat('*', 17), "#0000BB", "#11AABB");
        assertThat(validate(button), empty());
    }

    @Test
    public void testButton_tooLong_returnError() {
        Button button = createButton(StringUtils.repeat('*', 18), "#0000BB", "#11AABB");
        assertEquals(2, validate(button).size()); // TODO
    }

    @Test
    public void testDescriptionOptionsValidation() {
        Description description = createDescription(StringUtils.repeat('*', 75), "#00AABB");
        assertThat(validate(description), empty());
    }

    @Test
    public void testDescriptionOptionsValidation_invalidColor_returnsError() {
        Description description = createDescription(StringUtils.repeat('*', 33), "#00AABP");
        var result = validate(description);

        String expectedPath = "items[0].data.elements[0].options.color";
        String expectedMessage = "Invalid color format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testDisclaimerOptionsValidation() {
        Disclaimer disclaimer = createDisclaimer(StringUtils.repeat('*', 85), "#0000BB");
        assertThat(validate(disclaimer), empty());
    }

    @Test
    public void testDisclaimerOptionsValidation_tooLong_returnsError() {
        Disclaimer disclaimer = createDisclaimer(StringUtils.repeat('*', 86), "#0000BB");
        var result = validate(disclaimer);

        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Choose a value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testHeadlineOptionsValidation() {
        Headline headline = createHeadline(60, "#00AABB");
        assertThat(validate(headline), empty());
    }

    @Test
    public void testHeadlineOptionsValidation_invalidColor_returnsError() {
        Headline headline = createHeadline(26, "#00AABP");
        var result = validate(headline);

        String expectedPath = "items[0].data.elements[0].options.color";
        String expectedMessage = "Invalid color format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testSpecialColorValidation() {
        Special special = createSpecial("*", "#XBFFAA", "#RBFFAA");
        var result = validate(special, 2);

        checkErrorIsPresent(result, "items[0].data.elements[0].options.color", "Invalid color format value");
        checkErrorIsPresent(result, "items[0].data.elements[0].options.backgroundColor", "Invalid color format value");
    }

    @Test
    public void testButtonColorValidation() {
        Button button = createButton("*", "#XBFFAA", "#RBFFAA");
        var result = validate(button);

        checkErrorIsPresent(result, "items[0].data.elements[0].options.color", "Invalid color format value");
        checkErrorIsPresent(result, "items[0].data.elements[0].options.backgroundColor", "Invalid color format value");
    }

    @Test
    public void testValidSpecialColorValidation() {
        Special special = createSpecial("*", "#abFFde", "#001122");
        assertThat(validate(special, 2), empty());
    }

    @Test
    public void testValidButtonColorValidation() {
        Button button = createButton("*", "#abFFde", "#001122");
        assertThat(validate(button), empty());
    }

    @Test
    public void testFadeValidation() {
        Fade fade = createFade("#AABBCC");

        // Creative with presetId=1 and id=1 has fade
        var result = validate(List.of(fade), 1, 1);

        assertThat(result, empty());
    }

    @Test
    public void testFadeValidation_invalidColor_returnsError() {
        Fade fade = createFade("#AABBCZ");

        // Creative with presetId=1 and id=1 has fade
        var result = validate(List.of(fade), 1, 1);

        String expectedPath = "items[0].data.elements[0].options.color";
        String expectedMessage = "Invalid color format value";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testCpmGeoproductPreset_HeadlineNotAvailable_TextTooLong_NoError() {
        Headline headline = createHeadline(80, "#ffffff");
        headline.setAvailable(false);

        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(headline);
        assertThat(result, empty());
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_Headline() {
        Headline headline = createHeadline(14, "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(headline, 11);

        String expectedPath = "items[0].data.elements[0].options.content";
        String expectedMessage = "Text length must be between 1 and 13 symbols";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_Headline_Valid() {

        Headline headline = createHeadline(13, "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(headline, 11);

        assertThat(result, empty());
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_Description() {

        Description.Options options = new Description.Options();
        options.setContent(RandomStringUtils.randomAlphabetic(0));

        Description description = new Description();
        description.setOptions(options);
        description.setType(DESCRIPTION);
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(description, 11);

        String expectedPath = "items[0].data.elements[0].options.content";
        //String expectedMessage = "size must be between 1 and 13";
        String expectedMessage = "Text length must be between 1 and 13 symbols";
        checkErrorIsPresent(result, expectedPath, expectedMessage);
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_DomainAndPhone() {
        Domain domain = createDomain("http://ya.ru", "#ffffff");
        Phone phone = createPhone("+123456789012345", "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(List.of(domain, phone), 11);
        validate(domain, presetId);

        String expectedPath1 = "items[0].data.elements[0].options.content";
        String expectedPath2 = "items[0].data.elements[1].options.content";
        String expectedMessage = "You cannot specify a domain and phone number in the same creative";
        checkErrorIsPresent(result, expectedPath1, expectedMessage);
        checkErrorIsPresent(result, expectedPath2, expectedMessage);
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_DomainAndPhone_DomainUnavailable() {
        Domain domain = createDomain("http://ya.ru", "#ffffff");
        Phone phone = createPhone("+123456789012345", "#ffffff");
        domain.setAvailable(false);
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(List.of(domain, phone), 11);
        validate(domain, presetId);

        assertThat(result, empty());
    }

    @Test
    public void testCpmGeoPinPreset_TextLengthValidator_DomainAndPhone_PhoneUnavailable() {
        Domain domain = createDomain("http://ya.ru", "#ffffff");
        Phone phone = createPhone("+123456789012345", "#ffffff");
        phone.setAvailable(false);
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(List.of(domain, phone), 11);
        validate(domain, presetId);

        assertThat(result, empty());
    }

    @Test
    public void testCpmGeoPinPreset_UrlValidator_InvalidProtocol() {
        Domain domain = createDomain("ftp://ya.ru", "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(domain, 11);
        validate(domain, presetId);

        checkErrorIsPresent(result, "items[0].data.elements[0].options.content",
                "Enter a link starting with http/https");
    }

    @Test
    public void testCpmGeoPinPreset_UrlValidator_NoProtocol() {
        Domain domain = createDomain("ya.ru", "#ffffff");
        Set<ConstraintViolation<CreativeDocumentBatch>> result = validate(domain, 11);
        validate(domain, presetId);

        checkErrorIsPresent(result, "items[0].data.elements[0].options.content", "Enter a link starting with " +
                "http/https");
    }

    private Set<ConstraintViolation<CreativeDocumentBatch>> validate(Element element) {
        return validate(List.of(element), presetId);
    }

    private Set<ConstraintViolation<CreativeDocumentBatch>> validate(Element element, Integer presetId) {
        return validate(List.of(element), presetId);
    }

    private Set<ConstraintViolation<CreativeDocumentBatch>> validate(List<Element> elements, Integer presetId) {
        return validate(elements, presetId, 0);
    }

    private Set<ConstraintViolation<CreativeDocumentBatch>> validate(List<Element> elements, Integer presetId,
                                                                     Integer itemId) {
        CreativeDocument creative = createEmptyCreativeDocument(presetName, presetId);
        creative.getData().getElements().addAll(elements);
        creative.setId(itemId);

        CreativeDocumentBatch batch = new CreativeDocumentBatch();
        batch.setItems(singletonList(creative));
        batch.setName("Test CreativeDocumentBatch");

        return validator.validate(batch);
    }

    private CreativeDocumentBatch createEmptyBatch(String presetName, Integer presetId) {
        CreativeDocument creative = createEmptyCreativeDocument(presetName, presetId);

        CreativeDocumentBatch batch = new CreativeDocumentBatch();
        batch.setItems(singletonList(creative));
        batch.setName("Test CreativeDocumentBatch");

        return batch;
    }

    @NotNull
    private Headline createHeadline(int length, String color) {
        Headline.Options options = new Headline.Options();
        options.setContent(RandomStringUtils.randomAlphabetic(length));
        options.setColor(color);

        Headline headline = new Headline();
        headline.setOptions(options);
        headline.setType(HEADLINE);
        return headline;
    }

    @NotNull
    private Domain createDomain(String url, String color) {
        Domain.Options options = new Domain.Options();
        options.setContent(url);
        options.setColor(color);

        Domain domain = new Domain();
        domain.setOptions(options);
        domain.setType(DOMAIN);
        return domain;
    }

    @NotNull
    private Disclaimer createDisclaimer(String content, String color) {
        Disclaimer.Options options = new Disclaimer.Options();
        options.setContent(content);
        options.setColor(color);

        Disclaimer disclaimer = new Disclaimer();
        disclaimer.setOptions(options);
        disclaimer.setType(DISCLAIMER);
        return disclaimer;
    }

    @NotNull
    private Description createDescription(String content, String color) {
        Description.Options options = new Description.Options();
        options.setContent(content);
        options.setColor(color);

        Description description = new Description();
        description.setOptions(options);
        description.setType(DESCRIPTION);
        return description;
    }

    @NotNull
    private Phone createPhone(String number, String color) {
        Phone.Options options = new Phone.Options();
        options.setContent(number);
        options.setColor(color);

        Phone phone = new Phone();
        phone.setOptions(options);
        phone.setType(PHONE);
        return phone;
    }

    @NotNull
    private Fade createFade(String color) {
        Fade.Options options = new Fade.Options();
        options.setColor(color);

        Fade fade = new Fade();
        fade.setOptions(options);
        fade.setType(FADE);
        return fade;
    }

    @NotNull
    private Legal createLegal(String content, String color, String iconColor) {
        Legal.Options options = new Legal.Options();
        options.setColor(color);
        options.setContent(content);
        options.setIconColor(iconColor);

        Legal legal = new Legal();
        legal.setOptions(options);
        legal.setType(LEGAL);
        return legal;
    }

    @NotNull
    private Special createSpecial(String content, String color, String backgroundColor) {
        Special.Options options = new Special.Options();
        options.setColor(color);
        options.setContent(content);
        options.setBackgroundColor(backgroundColor);

        Special special = new Special();
        special.setOptions(options);
        special.setType(SPECIAL);
        return special;
    }

    @NotNull
    private Button createButton(String content, String color, String backgroundColor) {
        Button.Options options = new Button.Options();
        options.setColor(color);
        options.setContent(content);
        options.setBackgroundColor(backgroundColor);

        Button button = new Button();
        button.setOptions(options);
        button.setType(BUTTON);
        return button;
    }

    //Text length must be between 1 and 13 symbols
    public void checkErrorIsPresent(Set<ConstraintViolation<CreativeDocumentBatch>> result, String expectedPath,
                                    String expectedMessage) {
        assertTrue(StreamEx.of(result)
                .map(ConstraintViolation::getPropertyPath)
                .map(Path::toString)
                .findFirst(expectedPath::equals)
                .isPresent());

        assertTrue(StreamEx.of(result)
                .map(ConstraintViolation::getMessage)
                .findFirst(expectedMessage::equals)
                .isPresent());
    }
}


