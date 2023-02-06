package ru.yandex.direct.core.entity.creative.service.validation;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoAspect;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoSound;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoVideo;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.creative.service.add.validation.Constants;
import ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.creative.service.add.validation.CreativeValidationService.MULTI_PLAYLIST_FORMAT_TYPE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedObject;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CreativeValidationServiceVideoAdditionTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);
    private static final Long CREATIVE_ID = 1L;

    private CreativeValidationService creativeValidationService;

    @Before
    public void setUp() {
        creativeValidationService = new CreativeValidationService(null);
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(), emptyMap());
        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void validate_DuplicateClientIdAndStockCreativeId() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(Arrays.asList(
                                defaultVideoAddition(), defaultVideoAddition().withId(CREATIVE_ID + 1)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        duplicatedObject())));
    }

    @Test
    public void validate_WidthMayBeNotull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withWidth(1L)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void validate_HeightMayBeNotNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withHeight(1L)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getErrors(), hasSize(0));
    }

    @Test
    public void validate_LayoutIdOutOfRange() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(defaultVideoAddition().withLayoutId(6L)), CLIENT_ID,
                                emptyMap(),
                                emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(invalidValue().defectId())));
    }

    @Test
    public void validate_StockCreativeIdNull() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withStockCreativeId(null)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("stockCreativeId")),
                        notNull())));
    }

    @Test
    public void validate_StockCreativeIdNegative() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withStockCreativeId(-1L)),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(greaterThan(-1L).defectId())));
    }

    //moderation info

    //videos
    @Test
    public void validate_ModerationInfoVideosEmptyList() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition()
                                        .withModerationInfo(new ModerationInfo().withVideos(emptyList()))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("videos")),
                        notEmptyCollection())));
    }

    @Test
    public void validate_ModerationInfoVideosListNullValue() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition()
                                        .withModerationInfo(new ModerationInfo().withVideos(singletonList(null)))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("videos"), index(0)),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoVideoNullUrlField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withVideos(singletonList(new ModerationInfoVideo())))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("videos"), index(0), field("url")),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoVideoEmptyUrlField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(new ModerationInfo()
                                        .withVideos(singletonList(new ModerationInfoVideo().withUrl(""))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("videos"), index(0), field("url")),
                        notEmptyString())));
    }

    //sounds

    @Test
    public void validate_ModerationInfoSoundsListNullValue() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition()
                                        .withModerationInfo(new ModerationInfo().withSounds(singletonList(null)))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("sounds"), index(0)),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoSoundNullUrlField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withSounds(singletonList(new ModerationInfoSound())))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("sounds"), index(0), field("url")),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoSoundEmptyUrlField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(new ModerationInfo()
                                        .withSounds(singletonList(new ModerationInfoSound().withUrl(""))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("sounds"), index(0), field("url")),
                        notEmptyString())));
    }

    //aspects

    @Test
    public void validate_ModerationInfoAspectsEmptyList() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition()
                                        .withModerationInfo(new ModerationInfo().withAspects(emptyList()))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("aspects")),
                        notEmptyCollection())));
    }

    @Test
    public void validate_ModerationInfoAspectsListNullValue() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition()
                                        .withModerationInfo(new ModerationInfo().withAspects(singletonList(null)))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("aspects"), index(0)),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoAspectNullWidthField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withAspects(
                                                singletonList(defaultModerationInfoAspect().withWidth(null))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("aspects"), index(0), field("width")),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoAspectNegativeWidthField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withAspects(singletonList(
                                                defaultModerationInfoAspect().withWidth(-1L))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(greaterThan(-1L).defectId())));
    }

    @Test
    public void validate_ModerationInfoAspectGreaterMaxWidthField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withAspects(singletonList(
                                                defaultModerationInfoAspect()
                                                        .withWidth(Constants.MAX_ASPECT_VALUE + 1))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual,
                hasDefectDefinitionWith(validationError(lessThanOrEqualTo(Constants.MAX_ASPECT_VALUE).defectId())));
    }

    @Test
    public void validate_ModerationInfoAspectNullHeightField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo()
                                                .withAspects(singletonList(
                                                        defaultModerationInfoAspect().withHeight(null))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("moderationInfo"), field("aspects"), index(0), field("height")),
                        notNull())));
    }

    @Test
    public void validate_ModerationInfoAspectNegativeHeightField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withAspects(singletonList(
                                                defaultModerationInfoAspect().withHeight(-1L))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual, hasDefectDefinitionWith(validationError(greaterThan(-1L).defectId())));
    }

    @Test
    public void validate_ModerationInfoAspectGreaterMaxHeightField() {
        ValidationResult<List<Creative>, Defect> actual =
                creativeValidationService
                        .generateValidation(singletonList(
                                defaultVideoAddition().withModerationInfo(
                                        new ModerationInfo().withAspects(singletonList(
                                                defaultModerationInfoAspect()
                                                        .withHeight(Constants.MAX_ASPECT_VALUE + 1))))),
                                CLIENT_ID, emptyMap(), emptyMap());

        assertThat(actual,
                hasDefectDefinitionWith(validationError(lessThanOrEqualTo(Constants.MAX_ASPECT_VALUE).defectId())));
    }

    //additional data
    @Test
    public void validate_NotCpmOutdoorWithAdditionalDataNull_Successful() {
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(defaultVideoAddition()), CLIENT_ID, emptyMap(), emptyMap());

        assertThat("не outdoor креативы могут не иметь additionalData", vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataNull() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative().withAdditionalData(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr,
                hasDefectDefinitionWith(validationError(path(index(0), field(Creative.ADDITIONAL_DATA)), notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataNullDuration() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().withDuration(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.DURATION)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataNullFormats() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().withFormats(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataEmptyFormats() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().withFormats(emptyList());
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS)),
                        notEmptyCollection())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatWidthNull() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(0).withWidth(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(0),
                        field(VideoFormat.WIDTH)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatHeightNull() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(1).withHeight(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(1),
                        field(VideoFormat.HEIGHT)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatHeightWidthNullAndMultiPlaylistType_NoErrors() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(1)
                .withHeight(null)
                .withWidth(null)
                .withType(MULTI_PLAYLIST_FORMAT_TYPE);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatTypeNull() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(0).withType(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(0),
                        field(VideoFormat.TYPE)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatTypeBlank() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(0).withType("  ");
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(0),
                        field(VideoFormat.TYPE)),
                        notEmptyString())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatUrlNull() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(0).withUrl(null);
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(0),
                        field(VideoFormat.URL)),
                        notNull())));
    }

    @Test
    public void validate_CpmOutdoorAdditionalDataFormatUrlBlank() {
        Creative cpmOutdoorCreative = defaultCpmOutdoorCreative();
        cpmOutdoorCreative.getAdditionalData().getFormats().get(0).withUrl(" ");
        ValidationResult<List<Creative>, Defect> vr = creativeValidationService
                .generateValidation(singletonList(cpmOutdoorCreative), CLIENT_ID, emptyMap(), emptyMap());

        assertThat(vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(Creative.ADDITIONAL_DATA), field(AdditionalData.FORMATS), index(0),
                        field(VideoFormat.URL)),
                        notEmptyString())));
    }

    private Creative defaultVideoAddition() {
        return TestCreatives.defaultVideoAddition(CLIENT_ID, CREATIVE_ID);
    }

    private ModerationInfoAspect defaultModerationInfoAspect() {
        return new ModerationInfoAspect().withWidth(4L).withHeight(3L);
    }

    private Creative defaultCpmOutdoorCreative() {
        return TestCreatives.defaultCpmOutdoorVideoAddition(CLIENT_ID, CREATIVE_ID);
    }
}
