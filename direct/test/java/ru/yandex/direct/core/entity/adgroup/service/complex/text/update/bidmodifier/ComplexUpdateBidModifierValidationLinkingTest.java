package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.bidmodifier;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BID_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop.DESKTOP_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile.MOBILE_ADJUSTMENT;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.DEMOGRAPHY_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.DESKTOP_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier.MOBILE_MODIFIER;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.invalidPercentShouldBePositive;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class ComplexUpdateBidModifierValidationLinkingTest extends ComplexUpdateBidModifierTestBase {

    @Test
    public void adGroupWithInvalidMobileModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobile();
        complexBidModifier.getMobileModifier().getMobileAdjustment().withPercent(-1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path errPath = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(MOBILE_MODIFIER.name()), field(MOBILE_ADJUSTMENT.name()),
                field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithInvalidDesktopModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierDesktop();
        complexBidModifier.getDesktopModifier().getDesktopAdjustment().withPercent(-1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path adjustmentErrPath = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(DESKTOP_MODIFIER.name()), field(DESKTOP_ADJUSTMENT.name()));
        Path errPath = path(Lists.newArrayList(Iterables.concat(
                adjustmentErrPath.getNodes(), singletonList(field(PERCENT.name())))));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithInvalidDemographicsModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierDemographics();
        complexBidModifier.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path errPath = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidMobileModifierAndInvalidDemographicsModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        complexBidModifier.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path errPath = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithInvalidMobileModifierAndInvalidDemographicsModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        complexBidModifier.getMobileModifier().getMobileAdjustment().withPercent(400_000);
        complexBidModifier.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckFirstItemIsInvalid(singletonList(complexAdGroup));

        Path errPath1 = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(MOBILE_MODIFIER.name()), field(MOBILE_ADJUSTMENT.name()),
                field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath1, lessThanOrEqualTo(1300))));

        Path errPath2 = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath2, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(2));
    }

    @Test
    public void validAdGroupWithoutModifiersAndAdGroupWithInvalidDemographicsModifier() {
        createSecondAdGroup();

        ComplexBidModifier complexBidModifier = randomComplexBidModifierDemographics();
        complexBidModifier.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier);

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(complexAdGroup1, complexAdGroup2));

        Path errPath = path(index(1), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithValidMobileAndDemographicsModifiersAndAdGroupWithInvalidDemographicsModifier() {
        createSecondAdGroup();

        ComplexBidModifier complexBidModifier1 = randomComplexBidModifierMobileAndDemographics();
        ComplexBidModifier complexBidModifier2 = randomComplexBidModifierDemographics();
        complexBidModifier2.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier2);

        MassResult<Long> result = updateAndCheckSecondItemIsInvalid(asList(complexAdGroup1, complexAdGroup2));

        Path errPath = path(index(1), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    @Test
    public void adGroupWithInvalidMobileModifierAndAdGroupWithInvalidDemographicsModifier() {
        createSecondAdGroup();

        ComplexBidModifier complexBidModifier1 = randomComplexBidModifierMobile();
        complexBidModifier1.getMobileModifier().getMobileAdjustment().withPercent(400_000);
        ComplexBidModifier complexBidModifier2 = randomComplexBidModifierDemographics();
        complexBidModifier2.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);

        ComplexTextAdGroup complexAdGroup1 = createValidAdGroupForUpdate(adGroupInfo1)
                .withComplexBidModifier(complexBidModifier1);
        ComplexTextAdGroup complexAdGroup2 = createValidAdGroupForUpdate(adGroupInfo2)
                .withComplexBidModifier(complexBidModifier2);

        MassResult<Long> result = updateAndCheckBothItemsAreInvalid(asList(complexAdGroup1, complexAdGroup2));

        Path errPath1 = path(index(0), field(COMPLEX_BID_MODIFIER.name()),
                field(MOBILE_MODIFIER.name()), field(MOBILE_ADJUSTMENT.name()),
                field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath1, lessThanOrEqualTo(1300))));

        Path errPath2 = path(index(1), field(COMPLEX_BID_MODIFIER.name()),
                field(DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath2, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(2));
    }
}
