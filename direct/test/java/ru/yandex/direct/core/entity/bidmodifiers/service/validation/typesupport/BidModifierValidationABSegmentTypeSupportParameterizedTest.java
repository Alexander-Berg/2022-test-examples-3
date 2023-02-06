package ru.yandex.direct.core.entity.bidmodifiers.service.validation.typesupport;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimits;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierLimitsAdvanced;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport.BidModifierValidationABSegmentsTypeSupport;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingConditionService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientABSegmentsAdjustments;
import static ru.yandex.direct.core.testing.data.TestRetargetingConditions.defaultABSegmentRetCondition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.NumberDefects.lessThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(Parameterized.class)
public class BidModifierValidationABSegmentTypeSupportParameterizedTest {
    public ClientInfo clientInfo;
    public BidModifierABSegment modifier;
    public BidModifierValidationABSegmentsTypeSupport service;
    private RetargetingConditionService retargetingConditionService;
    private List<BidModifierABSegmentAdjustment> abSegmentAdjustments;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public Field field;

    @Parameterized.Parameter(2)
    public Long fieldValue;

    @Parameterized.Parameter(3)
    public Defect defect;

    public enum Field {
        PERCENT,
        SEGMENT,
        SECTION;
    }

    @Before
    public void setUp() {
        clientInfo = new ClientInfo().withClient(new Client().withClientId(1L));
        abSegmentAdjustments = createAdjustments();
        modifier = new BidModifierABSegment().withAbSegmentAdjustments(abSegmentAdjustments);
        retargetingConditionService = mock(RetargetingConditionService.class);

        service = new BidModifierValidationABSegmentsTypeSupport(retargetingConditionService);
    }

    @Parameterized.Parameters(name = "{0}, {1}, {2}")
    public static Collection<Object[]> data() {
        BidModifierLimits limits = BidModifierLimitsAdvanced
                .getLimits(BidModifierType.AB_SEGMENT_MULTIPLIER, CampaignType.TEXT,
                        null, ClientId.fromLong(1L), null);
        return Arrays.asList(new Object[][]{
                {"Процент больше максимума", Field.PERCENT, limits.percentMax + 1L,
                        lessThanOrEqualTo(limits.percentMax)},
                {"Процент отрицателен", Field.PERCENT, -2L,
                        new Defect<>(BidModifiersDefectIds.GeneralDefects.INVALID_PERCENT_SHOULD_BE_POSITIVE)},
                {"Сегмент отрицателен", Field.SEGMENT, -2L, validId()},
                {"Секция отрицательна", Field.SECTION, -2L, validId()},
        });
    }

    private List<BidModifierABSegmentAdjustment> createAdjustments() {
        RetargetingCondition retCond = defaultABSegmentRetCondition(clientInfo.getClientId());
        RetConditionInfo retConditionInfo = new RetConditionInfo()
                .withClientInfo(clientInfo)
                .withRetCondition(retCond);
        return createDefaultClientABSegmentsAdjustments(retConditionInfo);
    }

    @Test
    public void validateAddStep1_PercentValidation() {
        BidModifierABSegmentAdjustment adjustment = abSegmentAdjustments.get(0);
        String textField = "";
        switch (field) {
            case PERCENT:
                adjustment.setPercent(fieldValue.intValue());
                textField = "percent";
                break;
            case SEGMENT:
                adjustment.setSegmentId(fieldValue);
                textField = "segmentId";
                break;
            case SECTION:
                adjustment.setSectionId(fieldValue);
                textField = "sectionId";
                break;
        }

        ValidationResult<BidModifierABSegment, Defect> vr = service.validateAddStep1(modifier,
                CampaignType.TEXT, null, clientInfo.getClientId(), null);

        assertThat(vr, hasDefectWithDefinition(
                validationError(path(field("abSegmentAdjustments"), index(0), field(textField)), defect)));
    }
}
