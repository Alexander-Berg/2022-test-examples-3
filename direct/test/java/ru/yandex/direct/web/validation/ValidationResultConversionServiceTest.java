package ru.yandex.direct.web.validation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.validation.CommonDefectTranslations;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.Predicates;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.defect.params.StringDefectParams;
import ru.yandex.direct.validation.presentation.DefaultDefectPresentationRegistry;
import ru.yandex.direct.validation.presentation.DefectPresentationRegistry;
import ru.yandex.direct.validation.result.DefaultPathNodeConverterProvider;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.MappingPathNodeConverter;
import ru.yandex.direct.validation.result.PathNodeConverterProvider;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.validation.kernel.TranslatableWebDefect;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.ValidationResponse;
import ru.yandex.direct.web.validation.model.WebDefect;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.validation.ValidationUtils.hasValidationIssues;
import static ru.yandex.direct.web.validation.kernel.WebDefectPresentationProviders.webDefect;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ValidationResultConversionServiceTest {
    public static final String NAME = "name";
    public static final String GROUP_NAME_INT = "groupName";
    public static final String ID = "id";
    public static final String GROUPS = "groups";
    public static final String GEO = "geo";

    public static final String CAMPAIGN_NAME = "campaign_name";
    public static final String CAMPAIGN_ID = "campaign_id";
    public static final String CAMPAIGN_GROUPS = "campaign_groups";
    public static final String GROUP_NAME = "group_name";
    public static final String GROUP_ID = "group_id";

    public static final int MAX_LENGTH = 2;

    @Autowired
    private TranslationService translationService;

    private ValidationResultConversionService validationResultConversionService;
    private DefectPresentationRegistry<TranslatableWebDefect> defectPresentationRegistry;

    public static Defect notNullDefinition() {
        return new Defect<>(TestDefectIds.TEST_NOT_NULL);
    }

    public static Defect<StringDefectParams> maxStringLength(int max) {
        return new Defect<>(TestDefectIds.TEST_MAX_STRING_LENGTH,
                new StringDefectParams().withMaxLength(max));
    }

    public static Constraint<String, Defect> maxLength(int max) {
        return Constraint.fromPredicate(Predicates.maxLength(max), maxStringLength(max));
    }

    public static Constraint<String, Defect> notNull() {
        return v -> v != null ? null : notNullDefinition();
    }

    public static WebDefect webMaxStringSizeDefect(String path, String value, int maxLength) {
        return new WebDefect()
                .withPath(path)
                .withCode(TestDefectIds.TEST_MAX_STRING_LENGTH.getCode())
                .withText("Maximum length exceeded")
                .withDescription("Maximum length exceeded")
                .withValue(value)
                .withParams(new StringDefectParams().withMaxLength(maxLength));
    }

    public static WebDefect webNotNull(String path) {
        return new WebDefect()
                .withPath(path)
                .withCode(notNullDefinition().defectId().getCode())
                .withText("Invalid value")
                .withDescription("Invalid value");
    }

    public static WebDefect webInvalidValue(String path) {
        return new WebDefect()
                .withPath(path)
                .withCode(notNullDefinition().defectId().getCode())
                .withText("Invalid value")
                .withDescription("Invalid value");
    }

    public static Group createGroup() {
        Group adGroup = new Group();
        adGroup.id = 1L;
        adGroup.groupName = "ad group name";
        return adGroup;
    }

    public static Campaign createCampaign() {
        Campaign campaign = new Campaign();
        campaign.id = 1L;
        campaign.name = "camp name";
        campaign.groups = singletonList(createGroup());
        return campaign;
    }

    @Before
    public void setup() {
        defectPresentationRegistry = DefaultDefectPresentationRegistry.builder()
                .register(TestDefectIds.TEST_INVALID_VALUE,
                        webDefect(CommonDefectTranslations.INSTANCE.invalidValueShort()))
                .register(TestDefectIds.TEST_MAX_STRING_LENGTH,
                        webDefect(CommonDefectTranslations.INSTANCE.maxStringSizeShort()))
                .register(TestDefectIds.TEST_NOT_NULL,
                        webDefect(WebDefectTranslations.INSTANCE.invalidValue()))
                .build();
    }

    @Before
    public void setUp() {
        validationResultConversionService =
                new ValidationResultConversionService(buildPathNodeConverterProvider(),
                        defectPresentationRegistry, translationService);
    }

    @Test
    public void simpleValidationResult() {
        Campaign campaign = createCampaign();
        ItemValidationBuilder<Campaign, Defect> vb = ItemValidationBuilder.of(campaign);
        vb.item(campaign.name, NAME)
                .check(maxLength(MAX_LENGTH));

        WebValidationResult actualResult =
                validationResultConversionService.buildValidationResponse(vb.getResult()).validationResult();

        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(webMaxStringSizeDefect(CAMPAIGN_NAME, campaign.name, MAX_LENGTH));

        assertThat(actualResult)
                .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void validationResultForSubList() {
        Campaign campaign = createCampaign();
        int max = 2;
        ItemValidationBuilder<Campaign, Defect> vb = ItemValidationBuilder.of(campaign);
        vb.list(campaign.groups, GROUPS)
                .checkEachBy(group -> {
                    ItemValidationBuilder<Group, Defect> vb1 = ItemValidationBuilder.of(group);
                    vb1.item(group.groupName, GROUP_NAME_INT)
                            .check(maxLength(max));
                    return vb1.getResult();
                });

        WebValidationResult actualResult =
                validationResultConversionService.buildValidationResponse(vb.getResult()).validationResult();

        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(
                        webMaxStringSizeDefect(CAMPAIGN_GROUPS + "[0]." + GROUP_NAME, campaign.groups.get(0).groupName,
                                MAX_LENGTH));

        assertThat(actualResult)
                .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void notMappedFieldShownAsIs() {
        Campaign campaign = createCampaign();

        ItemValidationBuilder<Campaign, Defect> vb = ItemValidationBuilder.of(campaign);
        vb.item(campaign.geo, GEO)
                .check(notNull());

        WebValidationResult actualResult =
                validationResultConversionService.buildValidationResponse(vb.getResult()).validationResult();

        // Так как для поля нет правила конвертации, оно сохраняет своё имя
        WebValidationResult expectedResult = new WebValidationResult().addErrors(webNotNull(GEO));

        assertThat(actualResult)
                .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void pathNotChangedIfNoConverterRegistered() {
        Model model = new Model();

        ItemValidationBuilder<Model, Defect> vb = ItemValidationBuilder.of(model);
        vb.item(model.name, NAME)
                .check(notNull());

        WebValidationResult actualResult =
                validationResultConversionService.buildValidationResponse(vb.getResult()).validationResult();

        WebValidationResult expectedResult =
                new WebValidationResult().addErrors(webNotNull(NAME));

        assertThat(actualResult)
                .isEqualToComparingFieldByFieldRecursively(expectedResult);
    }

    @Test
    public void hasValidationIssuesForNullResult() {
        Result result = Result.successful(null);
        assertThat(hasValidationIssues(result)).isFalse();
    }

    @Test
    public void hasValidationIssuesForEmptyResult() {
        ValidationResult<?, Defect> result = ValidationResult.success(null);
        assertThat(hasValidationIssues(result)).isFalse();
    }

    @Test
    public void buildValidationResultForNullValidationObject() {
        ValidationResult<?, Defect> result = null;
        //noinspection ConstantConditions
        assertThat(validationResultConversionService.buildValidationResponse(result))
                .isEqualToComparingFieldByFieldRecursively(new ValidationResponse(new WebValidationResult()));
    }

    private PathNodeConverterProvider buildPathNodeConverterProvider() {
        return DefaultPathNodeConverterProvider.builder()
                .register(Campaign.class,
                        MappingPathNodeConverter
                                .builder(ValidationResultConversionServiceTest.class.getName())
                                .replace(NAME, CAMPAIGN_NAME)
                                .replace(ID, CAMPAIGN_ID)
                                .replace(GROUPS, CAMPAIGN_GROUPS)
                                .build())
                .register(Group.class,
                        MappingPathNodeConverter
                                .builder(ValidationResultConversionServiceTest.class.getName())
                                .replace(GROUP_NAME_INT, GROUP_NAME)
                                .replace(ID, GROUP_ID)
                                .build())
                .build();
    }

    private enum TestDefectIds implements DefectId<StringDefectParams> {
        TEST_NOT_NULL,
        TEST_INVALID_VALUE,
        TEST_MAX_STRING_LENGTH
    }

    public static class Campaign {
        public String name;
        public Long id;
        public String geo;
        public List<Group> groups = new ArrayList<>();
    }

    public static class Group {
        public String groupName;
        public Long id;
    }

    public static class Model {
        public String name;
    }
}
