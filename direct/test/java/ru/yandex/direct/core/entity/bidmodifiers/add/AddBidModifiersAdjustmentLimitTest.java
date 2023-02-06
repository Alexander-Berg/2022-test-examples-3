package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BannerType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierBannerTypeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTrafaretPositionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.Constants;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.defect.ids.NumberDefectIds;
import ru.yandex.direct.validation.defect.params.NumberDefectParams;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientBannerTypeModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientInventoryModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientTrafaretPositionModifier;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientWeatherModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AddBidModifiersAdjustmentLimitTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private long clientUid;
    private long campaignId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = campaignSteps.createActiveTextCampaign();
        clientId = campaignInfo.getClientId();
        clientUid = campaignInfo.getUid();
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    @Description("Количество корректировок по типу видео превышает максимальное")
    public void overlimitInventoryAdjustmentsInOneRequestTest() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        Long clientUid = campaignInfo.getUid();
        Long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientInventoryModifier()
                        .withCampaignId(campaignId)
                        .withInventoryAdjustments(
                                Collections.nCopies(
                                        Constants.INVENTORY_ADJUSTMENTS_LIMIT + 1,
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INPAGE)
                                                .withPercent(110)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(BidModifierInventory.INVENTORY_ADJUSTMENTS)),
                        new Defect<>(BidModifiersDefectIds.Number.TOO_MANY_INVENTORY_CONDITIONS,
                                new NumberDefectParams().withMax(Constants.INVENTORY_ADJUSTMENTS_LIMIT)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(BidModifierInventory.INVENTORY_ADJUSTMENTS), index(1)),
                        new Defect<>(BidModifiersDefectIds.GeneralDefects.DUPLICATE_ADJUSTMENT))));
    }

    @Test
    @Description("Процент корректировок по типу видео превышает максимальное")
    public void overlimitPercentAdjustments_NotPassValidation() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        Long clientUid = campaignInfo.getUid();
        Long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientInventoryModifier()
                        .withCampaignId(campaignId)
                        .withInventoryAdjustments(
                                singletonList(
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INPAGE)
                                                .withPercent(1400)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)));
    }

    @Test
    @Description("Процент корректировок по типу баннера превышает максимальное")
    public void overlimitPercentAdjustmentsBannerType_NotPassValidation() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        Long clientUid = campaignInfo.getUid();
        Long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientBannerTypeModifier()
                        .withCampaignId(campaignId)
                        .withBannerTypeAdjustments(
                                singletonList(
                                        new BidModifierBannerTypeAdjustment()
                                                .withBannerType(BannerType.CPM_BANNER)
                                                .withPercent(1400)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)));
    }

    @Test
    @Description("Процент корректировок по типу видео граничные значения")
    public void zeroPercentInAdjustments_Success() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        long clientUid = campaignInfo.getUid();
        long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientInventoryModifier()
                        .withCampaignId(campaignId)
                        .withInventoryAdjustments(
                                Arrays.asList(
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INPAGE)
                                                .withPercent(0),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INSTREAM_WEB)
                                                .withPercent(0),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INAPP)
                                                .withPercent(1300),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.INBANNER)
                                                .withPercent(1300),
                                        new BidModifierInventoryAdjustment()
                                                .withInventoryType(InventoryType.REWARDED)
                                                .withPercent(1300)
                                )
                        )), clientId, clientUid);
        assertFalse(result.getValidationResult().hasAnyErrors());
    }


    @Test
    @Description("Если все корректировки 0 - не пропускаем")
    public void zeroInAllPercentInInventoryAndCpmBannerAdjustments_NotPass() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        long clientUid = campaignInfo.getUid();
        long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(Arrays.asList(
                        createEmptyClientInventoryModifier()
                                .withCampaignId(campaignId)
                                .withInventoryAdjustments(
                                        Arrays.asList(
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INPAGE)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INSTREAM_WEB)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INTERSTITIAL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INAPP)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INBANNER)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.REWARDED)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.PREROLL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.MIDROLL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.POSTROLL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.PAUSEROLL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.OVERLAY)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.POSTROLL_OVERLAY)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.POSTROLL_WRAPPER)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INROLL_OVERLAY)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INROLL)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.FULLSCREEN)
                                                        .withPercent(0)
                                        )
                                ),
                        createEmptyClientBannerTypeModifier()
                                .withCampaignId(campaignId)
                                .withBannerTypeAdjustments(
                                        singletonList(
                                                new BidModifierBannerTypeAdjustment()
                                                        .withBannerType(BannerType.CPM_BANNER)
                                                        .withPercent(0)
                                        )
                                )
                ), clientId, clientUid);
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(
                validationError(BidModifiersDefectIds.GeneralDefects.DEVICE_BID_MODIFIERS_ALL_ZEROS)));
    }

    @Test
    @Description("Если  0 только коректировки типа инвентаря - пропускаем")
    public void zeroInAllPercentInInventoryAdjustmentsOnly_Pass() {
        CampaignInfo campaignInfo = campaignSteps.createActiveCpmBannerCampaign();
        ClientId clientId = campaignInfo.getClientId();
        long clientUid = campaignInfo.getUid();
        long campaignId = campaignInfo.getCampaignId();
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(
                        createEmptyClientInventoryModifier()
                                .withCampaignId(campaignId)
                                .withInventoryAdjustments(
                                        Arrays.asList(
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INPAGE)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INSTREAM_WEB)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INAPP)
                                                        .withPercent(0),
                                                new BidModifierInventoryAdjustment()
                                                        .withInventoryType(InventoryType.INBANNER)
                                                        .withPercent(0)
                                        )
                                )
                ), clientId, clientUid);
        assertFalse(result.getValidationResult().hasAnyErrors());
    }

    @Test
    @Description("Количество погодных корректировок в запросе превышает максимальное")
    public void overlimitWeatherAdjustmentsInOneRequestTest() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientWeatherModifier()
                        .withCampaignId(campaignId)
                        .withWeatherAdjustments(
                                Collections.nCopies(
                                        Constants.WEATHER_ADJUSTMENTS_LIMIT + 1,
                                        new BidModifierWeatherAdjustment()
                                                .withExpression(
                                                        singletonList(singletonList(new BidModifierWeatherLiteral()
                                                                .withParameter(WeatherType.CLOUDNESS)
                                                                .withValue(50).withOperation(OperationType.EQ)))
                                                )
                                                .withPercent(110)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("weatherAdjustments")),
                        new Defect<>(BidModifiersDefectIds.Number.TOO_MANY_WEATHER_CONDITIONS,
                                new NumberDefectParams().withMax(Constants.WEATHER_ADJUSTMENTS_LIMIT)))));
    }

    @Test
    @Description("Количество ретаргетинговых корректировок в запросе превышает максимальное")
    public void overlimitRetargetingAdjustmentsInOneRequestTest() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientRetargetingModifier()
                        .withCampaignId(campaignId)
                        .withRetargetingAdjustments(getRetargetingAdjustmentsList())), clientId, clientUid);
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments")),
                        new Defect<>(BidModifiersDefectIds.Number.TOO_MANY_RETARGETING_CONDITIONS,
                                new NumberDefectParams().withMax(Constants.RETARGETING_ADJUSTMENTS_LIMIT)))));
    }

    @Test
    @Description("Количество корректировок по формату на поиске превышает максимальное")
    public void overlimitTrafaretPositionAdjustmentsInOneRequestTest() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientTrafaretPositionModifier()
                        .withCampaignId(campaignId)
                        .withTrafaretPositionAdjustments(
                                Collections.nCopies(
                                        Constants.TRAFARET_POSITION_ADJUSTMENTS_LIMIT + 1,
                                        new BidModifierTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)
                                                .withPercent(110)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(BidModifierTrafaretPosition.TRAFARET_POSITION_ADJUSTMENTS)),
                        new Defect<>(BidModifiersDefectIds.Number.TOO_MANY_TRAFARET_POSITION_CONDITIONS,
                                new NumberDefectParams().withMax(Constants.TRAFARET_POSITION_ADJUSTMENTS_LIMIT)))));
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field(BidModifierTrafaretPosition.TRAFARET_POSITION_ADJUSTMENTS),
                        index(1)),
                        new Defect<>(BidModifiersDefectIds.GeneralDefects.DUPLICATE_ADJUSTMENT))));
    }

    @Test
    @Description("Процент корректировок по формату на поиске превышает максимальное")
    public void overlimitPercentTrafaretPositionAdjustments_NotPassValidation() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientTrafaretPositionModifier()
                        .withCampaignId(campaignId)
                        .withTrafaretPositionAdjustments(
                                singletonList(
                                        new BidModifierTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)
                                                .withPercent(1301)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)));
    }

    @Test
    @Description("Процент корректировок по формату на поиске меньше минимального")
    public void tooLowPercentTrafaretPositionAdjustments_NotPassValidation() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientTrafaretPositionModifier()
                        .withCampaignId(campaignId)
                        .withTrafaretPositionAdjustments(
                                singletonList(
                                        new BidModifierTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)
                                                .withPercent(99)
                                )
                        )), clientId, clientUid);
        assertThat(result.getValidationResult(),
                hasDefectDefinitionWith(validationError(NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)));
    }

    @Test
    @Description("Процент корректировок по формату на поиске граничные значения")
    public void validPercentInTrafaretPositionAdjustments_Success() {
        MassResult<List<Long>> result =
                addBidModifiers(singletonList(createEmptyClientTrafaretPositionModifier()
                        .withCampaignId(campaignId)
                        .withTrafaretPositionAdjustments(
                                Arrays.asList(
                                        new BidModifierTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.ALONE)
                                                .withPercent(100),
                                        new BidModifierTrafaretPositionAdjustment()
                                                .withTrafaretPosition(TrafaretPosition.SUGGEST)
                                                .withPercent(1300)
                                )
                        )), clientId, clientUid);
        assertFalse(result.getValidationResult().hasAnyErrors());
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }

    private static List<AbstractBidModifierRetargetingAdjustment> getRetargetingAdjustmentsList() {
        List<AbstractBidModifierRetargetingAdjustment> list = new ArrayList<>();
        for (int i = 0; i < Constants.RETARGETING_ADJUSTMENTS_LIMIT + 1; i++) {
            list.add(new BidModifierRetargetingAdjustment().withRetargetingConditionId(1000L + i).withPercent(110));
        }
        return list;
    }
}
