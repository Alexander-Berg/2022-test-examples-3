package ru.yandex.direct.excel.processing.model.internalad.mappers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.internalads.model.ResourceChoice;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceType;
import ru.yandex.direct.excel.processing.model.internalad.InternalBannerRepresentation;
import ru.yandex.direct.excelmapper.mappers.HyperlinkExcelMapper;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class BannerMappersTest {

    private static final String CHOICE_1 = "identity";
    private static final String DISPLAY_CHOICE_2 = "display";
    private static final String VALUE_CHOICE_2 = "value";
    private static final List<ResourceChoice> CHOICES = List.of(
            ResourceChoice.from(CHOICE_1), ResourceChoice.from(DISPLAY_CHOICE_2, VALUE_CHOICE_2));
    private static final String IMAGE_FILE = "someImageFile.name";
    private static final String IMAGE_URL = "imageURL";
    private static final String IMAGE_HASH = "imageHash";
    private static final String IMAGE_HYPERLINK = HyperlinkExcelMapper.makeHyperlink(IMAGE_URL, IMAGE_HASH);


    private InternalBannerRepresentation representation;

    @Before
    public void initTestData() {
        representation = new InternalBannerRepresentation()
                .setBanner(new InternalBanner().withAdGroupId(RandomNumberUtils.nextPositiveLong()));
    }

    public static Object[][] getParametersForVariableValueGetter() {
        return new Object[][]{
//              valueFromExcel     isBananaImage imageFileName imageURL   expectedValue     choices
                {null,             false,        null,         null,      null,             null},
                {null,             true,         IMAGE_FILE,   null,      null,             null},
                {"some Value1",    false,        null,         null,      "some Value1",    null},
                {"some Value2",    false,        IMAGE_FILE,   null,      "some Value2",    null},
                {"some Value3",    true,         null,         null,      "some Value3",    null},
                {IMAGE_HASH,       true,         IMAGE_FILE,   IMAGE_URL, IMAGE_HYPERLINK,  null},
                {CHOICE_1,         false,        null,         null,      CHOICE_1,         CHOICES},
                {VALUE_CHOICE_2,   false,        null,         null,      DISPLAY_CHOICE_2, CHOICES},
                {DISPLAY_CHOICE_2, false,        null,         null,      DISPLAY_CHOICE_2, CHOICES},
                {null,             false,        null,         null,      null,             CHOICES},
        };
    }

    @Test
    @TestCaseName("valueFromExcel = {0}, isBananaImage = {1}, imageFileName = {2}, imageURL = {3}, expectedValue = {4}")
    @Parameters(method = "getParametersForVariableValueGetter")
    public void checkVariableValueGetter(@Nullable String valueFromExcel,
                                         boolean isBananaImage,
                                         @Nullable String imageFileName, @Nullable String imageURL,
                                         @Nullable String expectedValue,
                                         @Nullable List<ResourceChoice> choices) {
        var templateResource = createResourceInfo(isBananaImage, choices);
        Map<String, String> imageURLByHashes = imageFileName != null && valueFromExcel != null && imageURL != null
                ? Map.of(valueFromExcel, imageURL)
                : Collections.emptyMap();

        representation.setTemplateVariableValue(templateResource.getId(), valueFromExcel);
        String actualValue = BannerMappers.variableValueGetter(representation, templateResource, imageURLByHashes);

        assertThat(actualValue).isEqualTo(expectedValue);
    }

    private static ResourceInfo createResourceInfo(boolean isBananaImage, @Nullable List<ResourceChoice> choices) {
        return new ResourceInfo()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withType(isBananaImage ? ResourceType.IMAGE : ResourceType.TEXT)
                .withChoices(choices);
    }

    public static Object[][] getParametersForVariableValueSetter() {
        return new Object[][]{
                {null, false, null, null},
                {null, true, null, null},
                {"some Value", false, "some Value", null},
                {"some: Value", false, "some: Value", null},
                {"http://some.url", false, "http://some.url", null},
                {"https://some.url/secure", false, "https://some.url/secure", null},
                {"invalid image Value", true, "invalid image Value", null},
                {IMAGE_HYPERLINK, true, IMAGE_HASH, null},
                {"   " + IMAGE_HYPERLINK + "    ", true, IMAGE_HASH, null},
                {"onlyFileName.svg", true, "onlyFileName.svg", null},
                {"   onlyFileNameWithSpace.png    ", true, "onlyFileNameWithSpace.png", null},
                {"http://some.url/image.png", true, "http://some.url/image.png", null},
                {"https://some.url/secure/image.jpeg", true, "https://some.url/secure/image.jpeg", null},
                {"   https://some.url_with_space/secure/image.jpeg    ", true,
                        "https://some.url_with_space/secure/image.jpeg", null},
                {CHOICE_1, false, CHOICE_1, CHOICES},
                {DISPLAY_CHOICE_2, false, VALUE_CHOICE_2, CHOICES},
                {VALUE_CHOICE_2, false, VALUE_CHOICE_2, CHOICES},
                {null, false, null, CHOICES},
        };
    }

    @Test
    @TestCaseName("valueForSet = {0}, isBananaImage = {1}, expectedValue = {2}")
    @Parameters(method = "getParametersForVariableValueSetter")
    public void checkVariableValueSetter(@Nullable String valueForSet, boolean isBananaImage,
                                         @Nullable String expectedValue,
                                         @Nullable List<ResourceChoice> choices) {
        var templateResource = createResourceInfo(isBananaImage, choices);

        BannerMappers.variableValueSetter(representation, valueForSet, templateResource);
        String actualValue = representation.getTemplateVariableValue(templateResource.getId());

        assertThat(actualValue).isEqualTo(expectedValue);
    }

}
