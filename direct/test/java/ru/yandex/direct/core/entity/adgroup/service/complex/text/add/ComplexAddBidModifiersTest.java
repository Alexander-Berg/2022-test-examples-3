package ru.yandex.direct.core.entity.adgroup.service.complex.text.add;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.complex.text.ComplexTextAdGroupAddOperation;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.container.ComplexTextAdGroup.COMPLEX_BID_MODIFIER;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.emptyAdGroupWithModelForAdd;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobile;
import static ru.yandex.direct.core.entity.adgroup.service.complex.ComplexTextAdGroupTestData.randomComplexBidModifierMobileAndDemographics;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment.PERCENT;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics.DEMOGRAPHICS_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.invalidPercentShouldBePositive;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ComplexAddBidModifiersTest extends ComplexTextAddTestBase {

    @Test
    public void oneAdGroupWithBidModifier() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBidModifier(randomComplexBidModifierMobile());
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void oneAdGroupWithoutBidModifiersAdnOneWith() {
        ComplexTextAdGroup emptyAdGroup = emptyTextAdGroup();
        ComplexTextAdGroup complexAdGroup = adGroupWithBidModifier(randomComplexBidModifierMobile());
        addAndCheckComplexAdGroups(asList(emptyAdGroup, complexAdGroup));
    }

    @Test
    public void adGroupWithSeveralModifiersInComplex() {
        ComplexTextAdGroup complexAdGroup = adGroupWithBidModifier(randomComplexBidModifierMobileAndDemographics());
        addAndCheckComplexAdGroups(singletonList(complexAdGroup));
    }

    @Test
    public void adGroupWithValidMobileModifierAndInvalidDemographicsModifier() {
        ComplexBidModifier complexBidModifier = randomComplexBidModifierMobileAndDemographics();
        complexBidModifier.getDemographyModifier().getDemographicsAdjustments().get(0).withPercent(-1);
        ComplexTextAdGroup complexAdGroup = adGroupWithBidModifier(complexBidModifier);

        ComplexTextAdGroupAddOperation addOperation = createOperation(singletonList(complexAdGroup));
        MassResult<Long> result = addOperation.prepareAndApply();

        Path errPath = path(index(0), field(ComplexTextAdGroup.COMPLEX_BID_MODIFIER.name()),
                field(ComplexBidModifier.DEMOGRAPHY_MODIFIER.name()), field(DEMOGRAPHICS_ADJUSTMENTS.name()),
                index(0), field(PERCENT.name()));
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(errPath, invalidPercentShouldBePositive())));

        assertThat(result.getValidationResult().flattenErrors(), hasSize(1));
    }

    protected ComplexTextAdGroup adGroupWithBidModifier(ComplexBidModifier bidModifier) {
        return emptyAdGroupWithModelForAdd(campaignId, bidModifier, COMPLEX_BID_MODIFIER);
    }
}
