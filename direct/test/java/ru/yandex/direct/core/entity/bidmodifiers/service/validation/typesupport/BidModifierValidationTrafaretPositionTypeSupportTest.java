package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationTrafaretPositionTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition.TRAFARET_POSITION_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.notSupportedMultiplier;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BidModifierValidationTrafaretPositionTypeSupportTest {

    private static final Path errorPath = path(field(TRAFARET_POSITION_ADJUSTMENTS.name()), index(0));
    private ClientId clientId;
    private BidModifierTrafaretPosition modifier;
    private BidModifierValidationTrafaretPositionTypeSupport service;
    private FeatureService featureService;

    @Before
    public void setUp() {
        clientId = ClientId.fromLong(1L);
        modifier = new BidModifierTrafaretPosition().withTrafaretPositionAdjustments(singletonList(
                new BidModifierTrafaretPositionAdjustment().withPercent(120)));
        service = new BidModifierValidationTrafaretPositionTypeSupport();
        featureService = mock(FeatureService.class);
    }

    @Test
    public void validateAddStep1_smartCampaign_errorIsGenerated() {
        ValidationResult<BidModifierTrafaretPosition, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.PERFORMANCE, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(errorPath, notSupportedMultiplier()))));
    }

    @Test
    public void validateAddStep1_textCampaign_success() {
        ValidationResult<BidModifierTrafaretPosition, Defect> vr = service.validateAddStep1(modifier.withCampaignId(1L),
                CampaignType.TEXT, null, clientId, new CachingFeaturesProvider(featureService));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }
}
