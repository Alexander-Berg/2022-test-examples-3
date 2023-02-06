package ru.yandex.direct.api.v5.entity.ads.validation;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionSetting;
import com.yandex.direct.api.v5.adextensiontypes.AdExtensionSettingItem;
import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.AgeLabelEnum;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.TextAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import com.yandex.direct.api.v5.general.OperationEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import one.util.streamex.LongStreamEx;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;

import ru.yandex.common.util.collections.Triple;
import ru.yandex.direct.api.v5.entity.ads.AdsUpdateRequestItem;
import ru.yandex.direct.api.v5.entity.ads.converter.AdsUpdateRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.core.entity.adgroup.container.AccessibleAdGroupTypes;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static freemarker.template.utility.Collections12.singletonList;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validTextAdUpdate;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AdsUpdateRequestConverterAndInternalValidatorTest {

    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final BannerService bannerService = mock(BannerService.class, Answers.RETURNS_DEEP_STUBS);

    private AdsUpdateRequestConverter converter;
    private AdsUpdateRequestValidator validator;

    @Before
    public void prepare() {
        AdGroupService adGroupService = mock(AdGroupService.class);

        Map<Long, AdGroupType> adGroupTypeMap = new HashMap<>();
        adGroupTypeMap.put(0L, AdGroupType.CPM_BANNER);
        doReturn(adGroupTypeMap).when(adGroupService).getAdGroupTypesByBannerIds(any(), any());

        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(CLIENT_ID));

        validator = new AdsUpdateRequestValidator(auth, adGroupService);
        converter = new AdsUpdateRequestConverter(bannerService);
    }

    private void before(TestCase testCase) {
        doReturn(singletonList(new TextBanner().withCalloutIds(testCase.calloutsBeforeUpdate)))
                .when(bannerService).getBannersByIds(any());
    }

    @Test
    @Parameters(method = "positive")
    @TestCaseName("{0}")
    public void converterAndInternalValidatorIntegration_positive(TestCase testCase) {
        before(testCase);

        var actualResult = convertAndValidateInternal(new UpdateRequest().withAds(testCase.adUpdateItem));

        @SuppressWarnings("unchecked")
        ModelChanges<BannerWithCallouts> castMc = (ModelChanges) actualResult.getValue().get(0).getInternalItem();
        List<Long> calloutsAfterUpdate = castMc.getChangedProp(BannerWithCallouts.CALLOUT_IDS);

        Assert.assertThat(actualResult, hasNoDefects());

        assertThat(calloutsAfterUpdate).isEqualTo(testCase.expectedCalloutsAfterUpdate);
    }

    @Test
    @Parameters(method = "errors")
    @TestCaseName("{0}")
    public void converterAndInternalValidatorIntegration_errors(TestCase testCase) {
        before(testCase);

        var actualResult = convertAndValidateInternal(new UpdateRequest().withAds(testCase.adUpdateItem));


        assertThat(actualResult.flattenWarnings()).isEmpty();
        assertThat(actualResult.flattenErrors()).isNotEmpty();

        // ошибка на весь список, но могут повторяться коды
        Iterator<Integer> expected = testCase.expectedCodes.iterator();
        for (DefectInfo<DefectType> actualDefect : actualResult.flattenErrors()) {
            assertThat(actualDefect.getDefect().getCode()).isEqualTo(expected.next());
        }
    }

    @Test
    @Parameters(method = "warnings")
    @TestCaseName("{0}")
    public void converterAndInternalValidatorIntegration_warnings(TestCase testCase) {
        before(testCase);

        var actualResult = convertAndValidateInternal(new UpdateRequest().withAds(testCase.adUpdateItem));


        assertThat(actualResult.flattenErrors()).isEmpty();
        assertThat(actualResult.flattenWarnings()).isNotEmpty();

        Iterator<Integer> expected = testCase.expectedCodes.iterator(); // ворнинги проверяем поэлементно
        for (DefectInfo<DefectType> actualDefect : actualResult.flattenWarnings()) {
            assertThat(actualDefect.getDefect().getCode()).isEqualTo(expected.next());
        }
        assertThat(expected.hasNext()).isFalse();
    }

    @Test
    @Parameters(method = "validationAgeLabel")
    @TestCaseName("{index}")
    public void converterAndInternalValidatorIntegrationAgeUpdate(
            Triple<List<BannerWithSystemFields>,
                    AdUpdateItem,
                    Predicate<ValidationResult<List<AdsUpdateRequestItem<BannerWithSystemFields>>, DefectType>>> arguments) {
        doReturn(arguments.first).when(bannerService).getBannersByIds(any());

        String allowedAdTypes = AdTypeNames.getApi5AllowedAdTypes();

        var result = validator.validateInternalRequest(
                converter.convert(new UpdateRequest().withAds(arguments.second), false),
                AccessibleAdGroupTypes.API5_ALLOWED_AD_GROUP_TYPES,
                allowedAdTypes);

        assertThat(arguments.third.test(result)).isTrue();
    }

    private ValidationResult<List<AdsUpdateRequestItem<BannerWithSystemFields>>, DefectType> convertAndValidateInternal(
            UpdateRequest request) {
        String allowedAdTypes = AdTypeNames.getApi5AllowedAdTypes();
        return validator.validateInternalRequest(converter.convert(request, false),
                AccessibleAdGroupTypes.API5_ALLOWED_AD_GROUP_TYPES, allowedAdTypes);
    }

    Iterable<TestCase> positive() {
        return asList(
                new TestCase("Positive: ADD + REMOVE")
                        .calloutsBeforeUpdate(1L, 2L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(3L).withOperation(OperationEnum.ADD),
                                new AdExtensionSettingItem().withAdExtensionId(2L)
                                        .withOperation(OperationEnum.REMOVE)))
                        .expectCalloutsAfterUpdate(1L, 3L),
                new TestCase("Positive: SET + SET")
                        .calloutsBeforeUpdate(1L, 2L, 3L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(4L).withOperation(OperationEnum.SET),
                                new AdExtensionSettingItem().withAdExtensionId(5L).withOperation(OperationEnum.SET)))
                        .expectCalloutsAfterUpdate(4L, 5L)
        );
    }

    Iterable<TestCase> errors() {
        return asList(
                new TestCase("Callouts list too long")
                        .calloutsBeforeUpdate(1L, 2L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                LongStreamEx.range(51).mapToObj(
                                        id -> new AdExtensionSettingItem()
                                                .withAdExtensionId(id)
                                                .withOperation(OperationEnum.ADD)
                                ).toList()))
                        .expect(7000),
                new TestCase("Different callout operations: SET + REMOVE")
                        .calloutsBeforeUpdate(1L, 2L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(3L).withOperation(OperationEnum.SET),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.REMOVE)))
                        .expect(4006),
                new TestCase("Different callout operations: SET + ADD")
                        .calloutsBeforeUpdate(1L, 2L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(3L).withOperation(OperationEnum.SET),
                                new AdExtensionSettingItem().withAdExtensionId(4L).withOperation(OperationEnum.ADD)))
                        .expect(4006),
                new TestCase("Duplicate ID: ADD + REMOVE")
                        .calloutsBeforeUpdate(1L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.ADD),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.REMOVE)))
                        .expect(9802),
                new TestCase("Duplicate ID: SET + SET")
                        .calloutsBeforeUpdate(1L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.SET),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.SET)))
                        .expect(9802),
                new TestCase("Several calloutids duplicated in modifying operation")
                        .calloutsBeforeUpdate()
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(1L).withOperation(OperationEnum.ADD),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.ADD),
                                new AdExtensionSettingItem().withAdExtensionId(1L).withOperation(OperationEnum.REMOVE),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.REMOVE)))
                        .expect(9802, 9802)
        );
    }

    Iterable<TestCase> warnings() {
        return asList(
                new TestCase("ADD already assigned")
                        .calloutsBeforeUpdate(1L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(1L).withOperation(OperationEnum.ADD)))
                        .expect(10170),
                new TestCase("REMOVE not assigned")
                        .calloutsBeforeUpdate(1L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.REMOVE)))
                        .expect(10171),
                new TestCase("ADD already assigned + REMOVE not assigned")
                        .calloutsBeforeUpdate(1L)
                        .update(new AdExtensionSetting().withAdExtensions(
                                new AdExtensionSettingItem().withAdExtensionId(1L).withOperation(OperationEnum.ADD),
                                new AdExtensionSettingItem().withAdExtensionId(2L).withOperation(OperationEnum.REMOVE)))
                        .expect(10170, 10171)
        );
    }

    Iterable<Triple<List<BannerWithSystemFields>, AdUpdateItem, Predicate<ValidationResult<List, DefectType>>>> validationAgeLabel() {
        Predicate<ValidationResult<List, DefectType>> hasWarningsPredicate =
                result -> result.getSubResults().values().stream()
                        .findFirst()
                        .get().getWarnings().stream()
                        .anyMatch(warning -> warning.getCode() == 6000);

        Predicate<ValidationResult<List, DefectType>> noWarningsPredicate =
                result -> !hasWarningsPredicate.test(result);

        return asList(
                Triple.of(
                        singletonList(new TextBanner().withId(404340L)),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.AGE_6)),
                        hasWarningsPredicate),
                Triple.of(
                        singletonList(new TextBanner().withId(404340L)),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.MONTHS_6)),
                        hasWarningsPredicate),
                Triple.of(
                        singletonList(new TextBanner().withId(404340L).withFlags(new BannerFlags().withFlags(new HashMap<>() {{
                            put(BannerFlags.AGE.getKey(), "age6");
                        }}))),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.MONTHS_6)),
                        noWarningsPredicate),
                Triple.of(
                        singletonList(new TextBanner().withId(404340L).withFlags(new BannerFlags().withFlags(new HashMap<>() {{
                            put(BannerFlags.BABY_FOOD.getKey(), "baby_food6");
                        }}))),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.AGE_6)),
                        noWarningsPredicate),
                Triple.of(
                        singletonList(new TextBanner().withId(404340L).withFlags(new BannerFlags().withFlags(new HashMap<>() {{
                            put(BannerFlags.AGE.getKey(), "age6");
                        }}))),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.AGE_12)),
                        noWarningsPredicate),
                Triple.of(
                        singletonList(new TextBanner().withId(404340L).withFlags(new BannerFlags().withFlags(new HashMap<>() {{
                            put(BannerFlags.BABY_FOOD.getKey(), "baby_food6");
                        }}))),
                        new AdUpdateItem().withId(404340L).withTextAd(validTextAdUpdate().withAgeLabel(AgeLabelEnum.MONTHS_8)),
                        noWarningsPredicate)
        );
    }

    private class TestCase {
        final String description;
        final AdUpdateItem adUpdateItem;
        List<Long> calloutsBeforeUpdate;
        List<Integer> expectedCodes;
        List<Long> expectedCalloutsAfterUpdate;

        TestCase(String description) {
            this.description = description;
            this.adUpdateItem = new AdUpdateItem().withTextAd(new TextAdUpdate());
        }

        TestCase calloutsBeforeUpdate(Long... ids) {
            this.calloutsBeforeUpdate = ImmutableList.copyOf(ids);
            return this;
        }

        TestCase expect(Integer... codes) {
            this.expectedCodes = asList(codes);
            return this;
        }

        TestCase expectCalloutsAfterUpdate(Long... ids) {
            this.expectedCalloutsAfterUpdate = asList(ids);
            return this;
        }

        TestCase update(AdExtensionSetting settings) {
            this.adUpdateItem.getTextAd().withCalloutSetting(
                    objectFactory.createTextAdUpdateBaseCalloutSetting(settings));
            return this;
        }

        @Override
        public String toString() {
            return description;
        }
    }

}
