package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationBannerTypeSupport;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.DeviceModifiersConflictChecker;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerType.BANNER_TYPE_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationBannerTypeSupportTest {
    private static final Path errorPath = path(field(BANNER_TYPE_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private BidModifierBannerType modifier;
    private BidModifierValidationBannerTypeSupport service;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        modifier = new BidModifierBannerType().withBannerTypeAdjustments(singletonList(
                new BidModifierBannerTypeAdjustment().withPercent(120)));
        service = new BidModifierValidationBannerTypeSupport(mock(DeviceModifiersConflictChecker.class));
    }

    @Test
    public void validateAddStep1_cpmYndxFrontpageCampaign_errorIsGenerated() {
        ValidationResult<BidModifierBannerType, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CPM_YNDX_FRONTPAGE, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_contentPromotionCampaign_errorIsGenerated() {
        ValidationResult<BidModifierBannerType, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.CONTENT_PROMOTION, null, clientId, null);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }
}
