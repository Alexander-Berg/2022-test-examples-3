package ru.yandex.direct.core.entity.campaign.service.accesschecker;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignAccessType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.matchesWith;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;

public class CampaignSubObjectAccessValidatorTest {
    private static final Long OBJECT_ID = 999L;

    @Test
    public void testConstraintProperties() {
        CampaignSubObjectAccessValidator validator = new CampaignSubObjectAccessValidator(
                mock(CampaignSubObjectAccessChecker.class), CampaignAccessType.READ,
                new AffectedCampaignIdsContainer());

        CampaignSubObjectAccessConstraint accessConstraint = validator.getAccessConstraint();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(accessConstraint.getAccessDefects())
                    .isEqualToComparingFieldByField(CampaignSubObjectAccessValidator.ACCESS_DEFECTS);
            softly.assertThat(accessConstraint.getDesiredAccess())
                    .isEqualTo(CampaignAccessType.READ);
            softly.assertThat(accessConstraint.getCheckedObjectIdProvider().apply(OBJECT_ID))
                    .isEqualTo(OBJECT_ID);
        });
    }

    @Test
    public void testConstraintSuccess() {
        CampaignSubObjectAccessConstraint accessConstraint = mock(CampaignSubObjectAccessConstraint.class);
        when(accessConstraint.apply(anyLong())).thenReturn(null);
        CampaignSubObjectAccessValidator validator = new CampaignSubObjectAccessValidator(accessConstraint);

        ValidationResult<Long, Defect> vr = validator.apply(OBJECT_ID);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
        verify(accessConstraint).apply(OBJECT_ID);
    }

    @Test
    public void testConstraintFail() {
        CampaignSubObjectAccessConstraint accessConstraint = mock(CampaignSubObjectAccessConstraint.class);
        when(accessConstraint.apply(anyLong())).thenReturn(invalidValue());
        CampaignSubObjectAccessValidator validator = new CampaignSubObjectAccessValidator(accessConstraint);

        ValidationResult<Long, Defect> vr = validator.apply(OBJECT_ID);

        assertThat(vr).is(matchedBy(hasDefectWithDefinition(matchesWith(invalidValue()))));
        verify(accessConstraint).apply(OBJECT_ID);
    }
}
