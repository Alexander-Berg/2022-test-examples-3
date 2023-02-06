package ru.yandex.direct.core.entity.banner.type.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersOperationContainer;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.href.BannerUrlCheckService;
import ru.yandex.direct.core.entity.campaign.model.InternalAutobudgetCampaign;
import ru.yandex.direct.core.entity.campaign.model.InternalCampaign;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.internalads.Constants;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;
import ru.yandex.direct.core.entity.internalads.model.InternalAdsProductOption;
import ru.yandex.direct.core.entity.internalads.model.InternalTemplateInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceChoice;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.model.ResourceRestriction;
import ru.yandex.direct.core.entity.internalads.model.ResourceType;
import ru.yandex.direct.core.entity.internalads.restriction.InternalAdRestrictionDefects;
import ru.yandex.direct.core.entity.internalads.restriction.Restrictions;
import ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefects;
import ru.yandex.direct.core.service.urlchecker.UrlCheckResult;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.invalidSpecSymbols;
import static ru.yandex.direct.core.entity.banner.type.internal.TemplateVariablesValidator.templateVariablesValidator;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
public class TemplateVariablesValidatorTest {
    private static final long TEMPLATE_ID = 44L;
    private static final long TEMPLATE_RESOURCE_ID1 = 1001L;
    private static final long TEMPLATE_RESOURCE_ID2 = 1002L;
    private static final long AGE_TEMPLATE_RESOURCE_ID = 1003L;
    private static final long CLOSE_COUNTER_TEMPLATE_RESOURCE_ID = 1004L;
    private static final long URL_RESOURCE_ID = 1005L;
    private static final long TEXT_RESOURCE_ID = 1006L;
    private static final long UNKNOWN_TEMPLATE_RESOURCE_ID = 11L;
    private static final String IMAGE_HASH1 = "1234567890123456789011";
    private static final String IMAGE_HASH2 = "1234567890123456789012";

    private static final String REACHABLE_URL = "https://yandex.ru/";
    private static final String VALUE_CHOICE1 = "text1";

    private static final InternalTemplateInfo INTERNAL_TEMPLATE_INFO = new InternalTemplateInfo()
            .withTemplateId(TEMPLATE_ID)
            .withResources(List.of(
                    new ResourceInfo()
                            .withId(TEMPLATE_RESOURCE_ID1)
                            .withType(ResourceType.TEXT)
                            .withLabel("this is text with choice")
                            .withValueRestrictions(List.of())
                            .withChoices(List.of(ResourceChoice.from(VALUE_CHOICE1), ResourceChoice.from("text2"))),
                    new ResourceInfo()
                            .withId(TEMPLATE_RESOURCE_ID2)
                            .withType(ResourceType.IMAGE)
                            .withLabel("this is image")
                            .withValueRestrictions(List.of(
                                    Restrictions.imageDimensionsEq(true, 100, 100)
                            )),
                    new ResourceInfo()
                            .withId(AGE_TEMPLATE_RESOURCE_ID)
                            .withType(ResourceType.AGE)
                            .withLabel("this is age")
                            .withValueRestrictions(Collections.emptyList()),
                    new ResourceInfo()
                            .withId(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID)
                            .withType(ResourceType.CLOSE_COUNTER)
                            .withLabel("this is close counter")
                            .withValueRestrictions(Collections.emptyList()),
                    new ResourceInfo()
                            .withId(URL_RESOURCE_ID)
                            .withType(ResourceType.URL)
                            .withLabel("This is url")
                            .withValueRestrictions(List.of(
                                    Restrictions.urlIsCorrect(true, true)
                            )),
                    new ResourceInfo()
                            .withId(TEXT_RESOURCE_ID)
                            .withType(ResourceType.TEXT)
                            .withLabel("this is text")
                            .withValueRestrictions(List.of())
            ))
            .withResourceRestrictions(List.of(
                    new ResourceRestriction().withRequired(Set.of(TEMPLATE_RESOURCE_ID2)).withAbsent(Set.of(TEMPLATE_RESOURCE_ID1)),
                    new ResourceRestriction().withRequired(Set.of(TEMPLATE_RESOURCE_ID1)).withAbsent(Set.of(TEMPLATE_RESOURCE_ID2))
            ))
            .withResourceRestrictionsErrorMessage("some error text");

    private static final TemplateVariable CORRECT_TEMPLATE_VARIABLE_1 = variable(TEMPLATE_RESOURCE_ID1, null);
    private static final TemplateVariable CORRECT_TEMPLATE_VARIABLE_2 = variable(TEMPLATE_RESOURCE_ID2, IMAGE_HASH1);
    private static final TemplateVariable CORRECT_TEMPLATE_AGE_VARIABLE = variable(AGE_TEMPLATE_RESOURCE_ID, "12+");
    private static final TemplateVariable CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE =
            variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, null);
    private static final TemplateVariable CORRECT_URL_VARIABLE = variable(URL_RESOURCE_ID, REACHABLE_URL);
    private static final TemplateVariable CORRECT_TEXT_VARIABLE =
            variable(TEXT_RESOURCE_ID,
                    "<br> text &nbsp; strong/strongstrong <strong>text</strong> text <br> text &nbsp; ><");

    private static final Map<String, BannerImageFormat> BANNER_IMAGE_FORMAT_MAP = Map.of(
            IMAGE_HASH1, new BannerImageFormat()
                    .withImageHash(IMAGE_HASH1)
                    .withSize(new ImageSize().withWidth(100).withHeight(100)),
            IMAGE_HASH2, new BannerImageFormat()
                    .withImageHash(IMAGE_HASH2)
                    .withSize(new ImageSize().withWidth(200).withHeight(200))
    );

    private TemplateVariablesValidator validator;
    private InternalAdsProduct internalAdsProduct;
    private GeoTree geoTree;
    private BannerUrlCheckService bannerUrlCheckService;
    private InternalCampaign campaign;
    private InternalAdGroup adGroup;
    private InternalBanner banner;
    private BannersOperationContainer container;

    @Before
    public void setUp() {
        geoTree = mock(GeoTree.class);
        doReturn(true)
                .when(geoTree).isAnyRegionIncludedIn(any(), any());
        bannerUrlCheckService = mock(BannerUrlCheckService.class);
        doReturn(new UrlCheckResult(true, null))
                .when(bannerUrlCheckService).isUrlReachable(REACHABLE_URL);
        internalAdsProduct = new InternalAdsProduct()
                .withOptions(Collections.emptySet());
        campaign = new InternalAutobudgetCampaign()
                .withIsMobile(false);
        adGroup = new InternalAdGroup()
                .withGeo(List.of(Region.RUSSIA_REGION_ID));
        banner = new InternalBanner()
                .withStatusShow(true);
        container = mock(BannersOperationContainer.class);
        validator = createValidator();
    }

    @Test
    public void correct() {
        var validationResult = validator.apply(List.of(CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2, CORRECT_TEMPLATE_AGE_VARIABLE, CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE, CORRECT_TEXT_VARIABLE));
        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void correctInAnotherOrder() {
        var validationResult = validator.apply(List.of(CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE, CORRECT_TEXT_VARIABLE, CORRECT_URL_VARIABLE,
                CORRECT_TEMPLATE_AGE_VARIABLE, CORRECT_TEMPLATE_VARIABLE_1));
        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void correctWithAnotherResourceRestrictionVariant() {
        var validationResult = validator.apply(List.of(
                variable(TEMPLATE_RESOURCE_ID1, VALUE_CHOICE1),
                variable(TEMPLATE_RESOURCE_ID2, null),
                variable(AGE_TEMPLATE_RESOURCE_ID, null),
                variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, null),
                variable(URL_RESOURCE_ID, null),
                variable(TEXT_RESOURCE_ID, null)
        ));
        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void withExtraResource_VariablesDoNotFitToTemplateInfo() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), BannerDefects.templateVariablesMismatch())));
    }

    @Test
    public void withExtraUnknownResource_VariablesDoNotFitToTemplateInfo() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE,
                variable(UNKNOWN_TEMPLATE_RESOURCE_ID, null)
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), BannerDefects.templateVariablesMismatch())));
    }

    @Test
    public void unknownResource_VariablesDoNotFitToTemplateInfo() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                variable(UNKNOWN_TEMPLATE_RESOURCE_ID, null),
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), BannerDefects.templateVariablesMismatch())));
    }

    @Test
    public void resourceWithNullId_VariablesDoNotFitToTemplateInfo() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                new TemplateVariable().withTemplateResourceId(null),
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), BannerDefects.templateVariablesMismatch())));
    }

    @Test
    public void emptyRequiredVariable() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                variable(TEMPLATE_RESOURCE_ID2, null),
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(1)), CommonDefects.requiredButEmpty())));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), InternalAdDefects.resourceRestrictionsNotFollowed(
                        INTERNAL_TEMPLATE_INFO.getResourceRestrictionsErrorMessage()))));
    }

    @Test
    public void mustBeAbsentRequiredVariable() {
        var validationResult = validator.apply(List.of(
                variable(TEMPLATE_RESOURCE_ID1, VALUE_CHOICE1),
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(0)), CommonDefects.mustBeEmpty())));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(), InternalAdDefects.resourceRestrictionsNotFollowed(
                        INTERNAL_TEMPLATE_INFO.getResourceRestrictionsErrorMessage()))));
    }

    @Test
    public void wrongChoice() {
        var validationResult = validator.apply(List.of(
                variable(TEMPLATE_RESOURCE_ID1, "wrong choice"),
                variable(TEMPLATE_RESOURCE_ID2, null),
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(0)), CommonDefects.invalidValue())));
    }

    @Test
    public void wrongImageSize() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                variable(TEMPLATE_RESOURCE_ID2, IMAGE_HASH2),
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));
        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(1)),
                        InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_NOT_EQUAL)));
    }

    @Test
    public void emptyAgeValue_WhenValueRequired() {
        internalAdsProduct.withOptions(Set.of(InternalAdsProductOption.SOFTWARE));

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                variable(AGE_TEMPLATE_RESOURCE_ID, null),
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(2)), CommonDefects.requiredButEmpty())));
    }

    @Test
    public void emptyAgeValue_WhenValueNotRequired() {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                variable(AGE_TEMPLATE_RESOURCE_ID, null),
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void closeByCampaignCounter_WhenCampaignHasImpressionRate() {
        campaign.setImpressionRateCount(RandomNumberUtils.nextPositiveInteger());
        validator = createValidator();

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_URL_VARIABLE,
                variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, Constants.CLOSE_BY_CAMPAIGN_COUNTER_VALUE),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void closeByCampaignCounter_WhenCampaign_HasNotImpressionRate() {
        campaign.setImpressionRateCount(null);
        validator = createValidator();

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_URL_VARIABLE,
                variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, Constants.CLOSE_BY_CAMPAIGN_COUNTER_VALUE),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)), BannerDefects.requiredCampaignsImpressionRateButEmpty())));
    }

    @Test
    public void closeByAdGroupCounter_WhenAdGroupHasImpressionRate() {
        adGroup.setRf(RandomNumberUtils.nextPositiveInteger());
        validator = createValidator();

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_URL_VARIABLE,
                variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, Constants.CLOSE_BY_AD_GROUP_COUNTER_VALUE),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void closeByAdGroupCounter_WhenAdGroup_HasNotImpressionRate() {
        adGroup.setRf(null);
        validator = createValidator();

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_URL_VARIABLE,
                variable(CLOSE_COUNTER_TEMPLATE_RESOURCE_ID, Constants.CLOSE_BY_AD_GROUP_COUNTER_VALUE),
                CORRECT_TEXT_VARIABLE)
        );

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)), BannerDefects.requiredAdGroupsImpressionRateButEmpty())));
    }

    @Test
    public void urlWithUnreachableVariable_httpError() {
        var unreachable = "https://kjergkjerbkerjbgkjerb.com/";
        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.HTTP_ERROR))
                .when(bannerUrlCheckService).isUrlReachable(unreachable);

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                variable(URL_RESOURCE_ID, unreachable),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)),
                        InternalAdDefects.urlUnreachable(UrlCheckResult.Error.HTTP_ERROR.name()))));
    }

    @Test
    public void urlWithUnreachableVariable_timeout() {
        var unreachable = "https://kjergkjerbkerjbgkjerb.com/";
        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT))
                .when(bannerUrlCheckService).isUrlReachable(unreachable);

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                variable(URL_RESOURCE_ID, unreachable),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)),
                        InternalAdDefects.urlUnreachable(UrlCheckResult.Error.TIMEOUT.name()))));
    }

    @Test
    public void urlWithUnreachableVariable_tooManyRedirects() {
        var unreachable = "https://kjergkjerbkerjbgkjerb.com/";
        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.TOO_MANY_REDIRECTS))
                .when(bannerUrlCheckService).isUrlReachable(unreachable);

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                variable(URL_RESOURCE_ID, unreachable),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)),
                        InternalAdDefects.urlUnreachable(UrlCheckResult.Error.TOO_MANY_REDIRECTS.name()))));
    }

    @Test
    public void urlWithUnreachableVariable_bannerIsNotActive() {
        banner = new InternalBanner().withStatusShow(false);
        createValidator();

        var unreachable = "https://kjergkjerbkerjbgkjerb.com/";
        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT))
                .when(bannerUrlCheckService).isUrlReachable(unreachable);

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                variable(URL_RESOURCE_ID, unreachable),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(index(4)),
                        InternalAdDefects.urlUnreachable(UrlCheckResult.Error.TIMEOUT.name()))));
    }

    @Test
    public void checkAddingUnreachableUrlIntoWarnings() {
        container = mock(BannersAddOperationContainer.class);
        doReturn(true).when((BannersAddOperationContainer) container).isCopy();
        validator = createValidator();
        banner = new InternalBanner().withStatusShow(true);
        var unreachable = "https://kjergkjerbkerjbgkjerb.com/";
        doReturn(new UrlCheckResult(false, UrlCheckResult.Error.TIMEOUT))
                .when(bannerUrlCheckService).isUrlReachable(unreachable);

        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                variable(URL_RESOURCE_ID, unreachable),
                CORRECT_TEXT_VARIABLE
        ));

        assertThat(validationResult, Matchers.allOf(hasNoErrors(), hasWarningWithDefinition(
                validationError(path(index(4)),
                        InternalAdDefects.urlUnreachable(UrlCheckResult.Error.TIMEOUT.name())))));
    }

    @SuppressWarnings("unused")
    private Object[] testDataForSpecSymbolsValidation() {
        return new Object[][]{
                {"Текст без спецсимволов", "text> <text text> &text text; &text",
                        null},
                {"Текст с корректными спецсимволами",
                        "<br> text &nbsp;&nbsp; text &nbsp;text <login><br> <br>text &nbsp;",
                        null},

                {"Текст с не поддерживаемым спецсимволом", "<br> text &nspb; text <br> text &nbsp;",
                        invalidSpecSymbols("&nspb;")},
                {"Текст с не поддерживаемым тегом", "<br> text &nbsp; text <bt> text &nbsp;",
                        invalidSpecSymbols("<bt>")},

                {"Текст с лишним '&' в начале спецсимвола", "<br> text &&nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("&&nbsp;")},
                {"Текст с лишним ';' в конце спецсимвола", "<br> text &nbsp;; text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;;")},
                {"Текст с лишним ';' и '&", "<br> text & &&&nbsp;;;; ; text <br> text &nbsp;",
                        invalidSpecSymbols("&&&nbsp;;;;")},
                {"Текст с лишним '<' в начале спецсимвола", "<<br> text &nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("<<br>")},
                {"Текст с лишним '>' в конце спецсимвола", "<br> text &nbsp; text <br> text <br>>",
                        invalidSpecSymbols("<br>>")},
                {"Текст с лишним '>' и '<'", "<br> text &nbsp; text < <<<br>> > text <br>>",
                        invalidSpecSymbols("<<<br>>")},

                {"Текст с пустым спецсимволом", "<br> text &; text <br> text &nbsp;",
                        invalidSpecSymbols("&;")},
                {"Текст с пустым тегом", "<br> text &nbsp; text <> text &nbsp;",
                        invalidSpecSymbols("<>")},

                {"Текст со спецсимволом без & и ;", "<br> text nbsp text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без & и ; в слове", "<br> text textnbsptext text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без & и ; в конце текста", "<br> text <br> text nbsp",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без ;", "<br> text &nbsp text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без ; в конце текста", "<br> text &nbsp; text <br> text &nbsp",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без ; в слове", "<br> text &nbsp; &nbsptext text <br> text",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без &", "&nbsp; text nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без & в начале текста", "nbsp; text &nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},
                {"Текст со спецсимволом без & в слове", "textnbsp;text text &nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("&nbsp;")},

                {"Текст с тегом без < и >", "<br> text &nbsp; text br text &nbsp;",
                        null},
                {"Текст с тегом без < и > в начале текста", "br text &nbsp; text <br> text &nbsp;",
                        null},
                {"Текст с тегом без < и > перед переносом строки", "login\n text &nbsp; text <br> text &nbsp;",
                        null},
                {"Текст с тегом без < и > в слове", "browser <br> text textbr &nbsp; textbrtext browser &nbsp; tbr",
                        null},
                {"Текст с тегом без <", "<br> text &nbsp; text br> text &nbsp;",
                        invalidSpecSymbols("<br>")},
                {"Текст с тегом без < в начале текста", "strong> text &nbsp; text <br> text &nbsp;",
                        invalidSpecSymbols("<strong>")},
                {"Текст с тегом без < в слове", "<br> text &nbsp; text/strong> text &nbsp;",
                        invalidSpecSymbols("</strong>")},
                {"Текст с тегом без >", "<br> text &nbsp; text text &nbsp; <login text",
                        invalidSpecSymbols("<login>")},
                {"Текст с тегом без > в конце текста", "<br> text &nbsp; text text &nbsp; <br",
                        invalidSpecSymbols("<br>")},
                {"Текст с тегом без > в слове", "<br> text &nbsp; text text &nbsp; <brtext",
                        invalidSpecSymbols("<br>")},
                {"Текст с открывающим и закрывающим тегом", "<strong>/strong</strong><strong>strong</strong>strong",
                        null}
        };
    }

    @Test
    @Parameters(method = "testDataForSpecSymbolsValidation")
    @TestCaseName("{0}")
    public void specSymbolsValidation(@SuppressWarnings("unused") String testName, String text, Defect defect) {
        var validationResult = validator.apply(List.of(
                CORRECT_TEMPLATE_VARIABLE_1,
                CORRECT_TEMPLATE_VARIABLE_2,
                CORRECT_TEMPLATE_AGE_VARIABLE,
                CORRECT_TEMPLATE_CLOSE_COUNTER_VARIABLE,
                CORRECT_URL_VARIABLE,
                variable(TEXT_RESOURCE_ID, text))
        );

        if (defect == null) {
            assertThat(validationResult, hasNoDefectsDefinitions());
        } else {
            assertThat(validationResult, hasDefectDefinitionWith(validationError(path(index(5)), defect)));
        }
    }

    private TemplateVariablesValidator createValidator() {
        return templateVariablesValidator(banner, INTERNAL_TEMPLATE_INFO, internalAdsProduct,
                geoTree, bannerUrlCheckService, container, campaign, adGroup,
                BANNER_IMAGE_FORMAT_MAP);
    }

    private static TemplateVariable variable(long resourceId, String value) {
        return new TemplateVariable()
                .withTemplateResourceId(resourceId)
                .withInternalValue(value);
    }
}
