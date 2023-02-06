package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithFrontpageTypes;
import ru.yandex.direct.core.entity.campaign.model.CpmYndxFrontpageCampaign;
import ru.yandex.direct.core.entity.currency.model.cpmyndxfrontpage.FrontpageCampaignShowType;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;

public class CampaignWithFrontpageTypesValidatorTest {

    @Test
    public void tesWithNullTypes() {
        ValidationResult<CampaignWithFrontpageTypes, Defect> vr =
                validate(createComapany(null));
        assertThat(vr, hasDefectDefinitionWith(validationError(CANNOT_BE_NULL)));
    }

    @Test
    public void tesWith0Type() {
        ValidationResult<CampaignWithFrontpageTypes, Defect> vr =
                validate(createComapany(ImmutableSet.of()));
        assertThat(vr, hasDefectDefinitionWith(validationError(SIZE_CANNOT_BE_LESS_THAN_MIN)));
    }

    @Test
    public void tesWith1Type() {
        ValidationResult<CampaignWithFrontpageTypes, Defect> vr =
                validate(createComapany(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void tesWith2Type() {
        ValidationResult<CampaignWithFrontpageTypes, Defect> vr =
                validate(createComapany(ImmutableSet.of(FrontpageCampaignShowType.FRONTPAGE, FrontpageCampaignShowType.FRONTPAGE_MOBILE)));
        assertThat(vr, hasNoDefectsDefinitions());
    }


    private CampaignWithFrontpageTypes createComapany(Set<FrontpageCampaignShowType> frontpageCampaignShowTypes) {
        return new CpmYndxFrontpageCampaign()
                .withAllowedFrontpageType(frontpageCampaignShowTypes);
    }



    private ValidationResult<CampaignWithFrontpageTypes, Defect> validate(CampaignWithFrontpageTypes campaign) {
        var validator = new CampaignWithFrontpageTypesValidator();
        return validator.apply(campaign);
    }
}
