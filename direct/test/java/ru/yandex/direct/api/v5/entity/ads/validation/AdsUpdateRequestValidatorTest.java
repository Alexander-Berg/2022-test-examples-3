package ru.yandex.direct.api.v5.entity.ads.validation;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import com.yandex.direct.api.v5.general.OperationEnum;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.bannersWithCreativeDeprecated;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.logoIsOnlyForBannersWithoutCreative;
import static ru.yandex.direct.api.v5.entity.ads.AdsDefectTypes.maxBannersPerUpdateRequest;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.dynamicAdUpdateWithCalloutsUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.listOfUpdateItems;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.textAdUpdateWithCalloutsUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validImageAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validSmartAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validTextAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.Constants.MAX_ELEMENTS_PER_UPDATE;
import static ru.yandex.direct.api.v5.validation.Matchers.hasDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.hasOnlyWarningDefectWith;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
@ParametersAreNonnullByDefault
public class AdsUpdateRequestValidatorTest {

    public static final ObjectFactory jaxbElementsFactory = new ObjectFactory();

    private final AdsUpdateRequestValidator validator = new AdsUpdateRequestValidator(mock(ApiAuthenticationSource.class),
            mock(AdGroupService.class));

    @Parameter
    public String description;

    @Parameter(1)
    public UpdateRequest request;

    @Parameter(2)
    public Matcher<ValidationResult<UpdateRequest, DefectType>> resultMatcher;

    @Parameter(3)
    public boolean smartNoCreatives;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> params() {
        return asList(
                testCase("Max array length exceeded",
                        new UpdateRequest().withAds(listOfUpdateItems(MAX_ELEMENTS_PER_UPDATE + 1)),
                        err(path(field("Ads")), maxBannersPerUpdateRequest(MAX_ELEMENTS_PER_UPDATE))),

                testCase("Logo extension on smart banner with creative",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withSmartAdBuilderAd(validSmartAdUpdate()
                                        .withLogoExtensionHash(jaxbElementsFactory
                                                .createSmartAdBuilderAdUpdateLogoExtensionHash(null)))),
                        err(path(field("Ads"), index(0), field("SmartAdBuilderAd"), field("LogoExtensionHash")),
                                logoIsOnlyForBannersWithoutCreative())),

                testCaseSmartNoCreatives("Smart banner with creative with SMART_NO_CREATIVES enabled",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withSmartAdBuilderAd(validSmartAdUpdate())),
                        warn(path(field("Ads"), index(0), field("SmartAdBuilderAd"), field("Creative")),
                                bannersWithCreativeDeprecated())),

                // callouts: positive cases

                testCase("Callouts update for text ad (+): SET operations only",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withId(1L)
                                        .withTextAd(textAdUpdateWithCalloutsUpdate(
                                                OperationEnum.SET, OperationEnum.SET))),
                        hasNoDefects()),

                testCase("Callouts update for text ad (+): no SET operations",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withId(1L)
                                        .withTextAd(textAdUpdateWithCalloutsUpdate(
                                                OperationEnum.ADD, OperationEnum.REMOVE))),
                        hasNoDefects()),

                testCase("Callouts update for dynamic ad (+): SET operations only",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withId(1L)
                                        .withDynamicTextAd(dynamicAdUpdateWithCalloutsUpdate(
                                                OperationEnum.SET, OperationEnum.SET))),
                        hasNoDefects()),

                testCase("Callouts update for dynamic ad (+): no SET operations",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withId(1L)
                                        .withDynamicTextAd(dynamicAdUpdateWithCalloutsUpdate(
                                                OperationEnum.ADD, OperationEnum.REMOVE))),
                        hasNoDefects()),

                // positive

                testCase("Max array length of valid items",
                        new UpdateRequest().withAds(listOfUpdateItems(MAX_ELEMENTS_PER_UPDATE)),
                        hasNoDefects()),

                testCase("Two typed objects in one update item",
                        // эта ситуация пропускается и обрабатывается в конвертере,
                        // чтобы навесить ошибку на элемент уже в валидации внутреннего запроса
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withId(1L)
                                        .withTextAd(validTextAdUpdate()),
                                new AdUpdateItem().withId(2L)
                                        .withTextAd(validTextAdUpdate())
                                        .withTextImageAd(validImageAdUpdate())),
                        hasNoDefects()),

                testCaseSmartNoCreatives("Logo extension on banner without creative",
                        new UpdateRequest().withAds(
                                new AdUpdateItem().withSmartAdBuilderAd(new SmartAdBuilderAdUpdate()
                                        .withLogoExtensionHash(jaxbElementsFactory
                                                .createSmartAdBuilderAdUpdateLogoExtensionHash(null)))),
                        hasNoDefects())
        );
    }

    @Test
    public void test() {
        assertThat(validator.validate(request, smartNoCreatives), resultMatcher);
    }

    private static Matcher<ValidationResult<UpdateRequest, DefectType>> err(Path path, DefectType defectType) {
        return hasDefectWith(validationError(path, defectType));
    }

    private static Matcher<ValidationResult<UpdateRequest, DefectType>> warn(Path path, DefectType defectType) {
        return hasOnlyWarningDefectWith(validationError(path, defectType));
    }

    private static Object[] testCase(String description, UpdateRequest request,
                                     Matcher<ValidationResult<UpdateRequest, DefectType>> matcher) {
        return new Object[]{description, request, matcher, false};
    }

    private static Object[] testCaseSmartNoCreatives(String description, UpdateRequest request,
                                                     Matcher<ValidationResult<UpdateRequest, DefectType>> matcher) {
        return new Object[]{description, request, matcher, true};
    }

}
