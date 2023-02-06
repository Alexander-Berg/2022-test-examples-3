package ru.yandex.direct.core.entity.adgroup.service.validation.types;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.CpmBannerAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.PathNode;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.anyValidationErrorOnPathStartsWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AdGroupTypeSpecificValidationProviderTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    private static final TextAdGroup TEXT_AD_GROUP1 = new TextAdGroup();
    private static final TextAdGroup TEXT_AD_GROUP2 = new TextAdGroup();
    private static final MobileContentAdGroup MOBILE_ADGROUP1 = new MobileContentAdGroup();
    private static final MobileContentAdGroup MOBILE_ADGROUP2 = new MobileContentAdGroup();
    private static final CpmBannerAdGroup GROUP_OF_UNKNOWN_TYPE = new CpmBannerAdGroup();

    private static final ModelChanges<AdGroup> TEXT_AD_GROUP_MODEL_CHANGES1 =
            new ModelChanges<>(22L, TextAdGroup.class).castModelUp(AdGroup.class);

    private static final PathNode.Field ERROR_FIELD_IN_MOBILE = field("storeUrl");
    private static final PathNode.Field ERROR_FIELD_IN_TEXT = field("id");

    private AdGroupTypeSpecificValidationService<AdGroup> validatorForText;
    private AdGroupTypeSpecificValidationService<AdGroup> validatorForMobile;
    private AdGroupTypeSpecificValidationProvider validationProvider;

    @SuppressWarnings("unchecked")
    @Before
    public void before() {
        validatorForText =
                (AdGroupTypeSpecificValidationService<AdGroup>) mock(AdGroupTypeSpecificValidationService.class);
        validatorForMobile =
                (AdGroupTypeSpecificValidationService<AdGroup>) mock(AdGroupTypeSpecificValidationService.class);

        validationProvider = new AdGroupTypeSpecificValidationProvider(ImmutableMap.of(
                TextAdGroup.class, validatorForText,
                MobileContentAdGroup.class, validatorForMobile
        ));
    }

    @Test
    public void validateAdGroups_OnEmptyList_ReturnsSuccessfulResult() {
        ValidationResult<List<AdGroup>, Defect> result = validationProvider.validateAdGroups(CLIENT_ID, emptyList());

        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAdGroups_OnEmptyList_DoesNotInvokeAnyValidator() {
        validationProvider.validateAdGroups(CLIENT_ID, emptyList());

        verify(validatorForText, never()).validateAdGroups(any(), anyList());
        verify(validatorForMobile, never()).validateAdGroups(any(), anyList());
    }

    @Test
    public void validateAdGroups_OnUnknownGroupType_ReturnsSuccessfulResult() {
        List<AdGroup> adGroupsWithUnknownType = singletonList(GROUP_OF_UNKNOWN_TYPE);

        ValidationResult<List<AdGroup>, Defect> result =
                validationProvider.validateAdGroups(CLIENT_ID, adGroupsWithUnknownType);

        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAdGroups_OnUnknownGroupType_DoesNotInvokeAnyValidator() {
        List<AdGroup> adGroupsWithUnknownType = singletonList(GROUP_OF_UNKNOWN_TYPE);

        validationProvider.validateAdGroups(CLIENT_ID, adGroupsWithUnknownType);

        verify(validatorForText, never()).validateAdGroups(any(), anyList());
        verify(validatorForMobile, never()).validateAdGroups(any(), anyList());
    }

    @Test
    public void validateAdGroups_OnTextGroup_DoesNotInvokeMobileValidator() {
        letValidatorForTextReturnsSuccessfulResult(singletonList(TEXT_AD_GROUP1));

        validationProvider.validateAdGroups(CLIENT_ID, singletonList(TEXT_AD_GROUP1));

        verify(validatorForText, only()).validateAdGroups(eq(CLIENT_ID), eq(singletonList(TEXT_AD_GROUP1)));
        verify(validatorForMobile, never()).validateAdGroups(any(), anyList());
    }

    @Test
    public void validateAdGroups_OnTextAndMobileAdGroups_InvokesAllValidator() {
        letValidatorForTextReturnsSuccessfulResult(asList(TEXT_AD_GROUP1, TEXT_AD_GROUP2));
        letValidatorForMobileReturnsSuccessfulResult(asList(MOBILE_ADGROUP1, MOBILE_ADGROUP2));

        validationProvider.validateAdGroups(CLIENT_ID,
                asList(TEXT_AD_GROUP1, MOBILE_ADGROUP1, TEXT_AD_GROUP2, MOBILE_ADGROUP2));

        verify(validatorForText, only()).validateAdGroups(
                eq(CLIENT_ID), eq(asList(TEXT_AD_GROUP1, TEXT_AD_GROUP2)));
        verify(validatorForMobile, only()).validateAdGroups(
                eq(CLIENT_ID), eq(asList(MOBILE_ADGROUP1, MOBILE_ADGROUP2)));
    }

    @Test
    public void validateAdGroups_OnTextAndMobileAdGroups_ReturnsSuccessfulResult() {
        letValidatorForTextReturnsSuccessfulResult(asList(TEXT_AD_GROUP1, TEXT_AD_GROUP2));
        letValidatorForMobileReturnsSuccessfulResult(asList(MOBILE_ADGROUP1, MOBILE_ADGROUP2));

        ValidationResult<List<AdGroup>, Defect> result =
                validationProvider.validateAdGroups(CLIENT_ID,
                        asList(TEXT_AD_GROUP1, MOBILE_ADGROUP1, TEXT_AD_GROUP2, MOBILE_ADGROUP2));

        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateAdGroups_ValidatorsReturnError_CheckMergedResult() {
        letValidatorForTextReturnsErrorOnSecond(asList(TEXT_AD_GROUP1, TEXT_AD_GROUP2));
        letValidatorForMobileReturnsWarningOnFirst(asList(MOBILE_ADGROUP1, MOBILE_ADGROUP2));

        ValidationResult<List<AdGroup>, Defect> result =
                validationProvider.validateAdGroups(CLIENT_ID,
                        asList(TEXT_AD_GROUP1, MOBILE_ADGROUP1, TEXT_AD_GROUP2, MOBILE_ADGROUP2));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).is(matchedBy(
                    not(hasDefectWithDefinition(anyValidationErrorOnPathStartsWith(path(index(0)))))));
            softly.assertThat(result).is(matchedBy(
                    hasWarningWithDefinition(
                            validationError(path(index(1), ERROR_FIELD_IN_MOBILE), CommonDefects.invalidValue()))));
            softly.assertThat(result).is(matchedBy(
                    hasDefectWithDefinition(
                            validationError(path(index(2), ERROR_FIELD_IN_TEXT), CommonDefects.notNull()))));
            softly.assertThat(result).is(matchedBy(
                    not(hasDefectWithDefinition(anyValidationErrorOnPathStartsWith(path(index(0)))))));
        });
    }

    @Test
    public void validateModelChanges_OnEmptyList_ReturnsSuccessfulResult() {
        ValidationResult<List<ModelChanges<AdGroup>>, Defect> result =
                validationProvider.validateModelChanges(CLIENT_ID, emptyList());

        assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void validateModelChanges_OnEmptyList_DoesNotInvokeAnyValidator() {
        validationProvider.validateModelChanges(CLIENT_ID, emptyList());

        verify(validatorForText, never()).validateAdGroups(any(), anyList());
        verify(validatorForMobile, never()).validateAdGroups(any(), anyList());
    }


    @Test
    public void validateModelChanges_OnTextGroup_DoesNotInvokeMobileValidator() {
        letModelChangesValidatorForTextReturnsSuccessfulResult(singletonList(TEXT_AD_GROUP_MODEL_CHANGES1));

        validationProvider.validateModelChanges(CLIENT_ID, singletonList(TEXT_AD_GROUP_MODEL_CHANGES1));

        verify(validatorForText, only())
                .validateModelChanges(eq(CLIENT_ID), eq(singletonList(TEXT_AD_GROUP_MODEL_CHANGES1)));
        verify(validatorForMobile, never()).validateModelChanges(any(), anyList());
    }


    private void letValidatorForTextReturnsSuccessfulResult(List<AdGroup> adGroups) {
        when(validatorForText.validateAdGroups(CLIENT_ID, adGroups))
                .thenReturn(ValidationResult.success(adGroups));
    }

    private void letModelChangesValidatorForTextReturnsSuccessfulResult(List<ModelChanges<AdGroup>> changes) {
        when(validatorForText.validateModelChanges(CLIENT_ID, changes))
                .thenReturn(ValidationResult.success(changes));
    }

    private void letValidatorForMobileReturnsSuccessfulResult(List<AdGroup> adGroups) {
        when(validatorForMobile.validateAdGroups(CLIENT_ID, adGroups))
                .thenReturn(ValidationResult.success(adGroups));
    }

    private void letValidatorForTextReturnsErrorOnSecond(List<AdGroup> adGroups) {
        ValidationResult<List<AdGroup>, Defect> vr = new ValidationResult<>(adGroups);
        vr.getOrCreateSubValidationResult(index(1), adGroups.get(1))
                .getOrCreateSubValidationResult(ERROR_FIELD_IN_TEXT, null)
                .addError(CommonDefects.notNull());
        when(validatorForText.validateAdGroups(CLIENT_ID, adGroups))
                .thenReturn(vr);
    }

    private void letValidatorForMobileReturnsWarningOnFirst(List<AdGroup> adGroups) {
        ValidationResult<List<AdGroup>, Defect> vr = new ValidationResult<>(adGroups);
        vr.getOrCreateSubValidationResult(index(0), adGroups.get(0))
                .getOrCreateSubValidationResult(ERROR_FIELD_IN_MOBILE, null)
                .addWarning(CommonDefects.invalidValue());
        when(validatorForMobile.validateAdGroups(CLIENT_ID, adGroups))
                .thenReturn(vr);
    }
}
