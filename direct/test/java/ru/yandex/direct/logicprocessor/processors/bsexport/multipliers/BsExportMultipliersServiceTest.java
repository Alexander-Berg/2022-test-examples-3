package ru.yandex.direct.logicprocessor.processors.bsexport.multipliers;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.adv.direct.expression.CustomExpression;
import ru.yandex.adv.direct.expression.DeleteDirectMultipliersRow;
import ru.yandex.adv.direct.expression.DirectMultipliersRow;
import ru.yandex.adv.direct.expression.InventoryTypeEnum;
import ru.yandex.adv.direct.expression.MultiplierAtom;
import ru.yandex.adv.direct.expression.MultiplierChangeRequest;
import ru.yandex.adv.direct.expression.TargetingExpression;
import ru.yandex.adv.direct.expression.TargetingExpressionAtom;
import ru.yandex.adv.direct.expression.keywords.KeywordEnum;
import ru.yandex.adv.direct.expression.multipler.type.MultiplierTypeEnum;
import ru.yandex.adv.direct.expression.operations.OperationEnum;
import ru.yandex.direct.bstransport.yt.repository.MultipliersYtRepository;
import ru.yandex.direct.common.log.service.LogBsExportEssService;
import ru.yandex.direct.core.bsexport.repository.BsExportMultipliersRepository;
import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.AbstractBidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnly;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopOnlyAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventory;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierInventoryAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilter;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingFilterAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTV;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierSmartTVAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTablet;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierTabletAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeather;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierWeatherLiteral;
import ru.yandex.direct.core.entity.bidmodifier.InventoryType;
import ru.yandex.direct.core.entity.bidmodifier.OperationType;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.entity.bidmodifier.TabletOsType;
import ru.yandex.direct.core.entity.bidmodifier.WeatherType;
import ru.yandex.direct.core.entity.bidmodifiers.container.BidModifierKey;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.AccessibleGoalChangedInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.BsExportMultipliersObject;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.DeleteInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.TimeTargetChangedInfo;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.UpsertInfo;
import ru.yandex.direct.libs.timetarget.TimeTarget;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.BidModifierMultiplierHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.DeviceMultiplierHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.InventoryTypeHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.RetargetingMultiplierHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.TimeMultiplierHandler;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.WeatherMultiplierHandler;

import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.handler.RetargetingMultiplierHandler.RSYA_TARGET_TYPE;

public class BsExportMultipliersServiceTest {

    public static final long CAMPAIGN_ID = 100L;
    public static final long AD_GROUP_ID = 1000L;
    public static final long HIERARCHICAL_MULTIPLIER_ID = 444L;
    public static final long HIERARCHICAL_MULTIPLIER_SECOND_ID = 555L;
    public static final long ORDER_ID = 1111100L;
    public static final long RETARGETING_CONDITION_ID = 57775L;
    public static final long RETARGETING_CONDITION_SECOND_ID = 68886L;
    public static final String CAMPAIGN_TIME_TARGET_COEF =
            "1IJnKLnMNOnPQRnST2IJnKLnMNOnPQRnST3IJnKLnMNOnPQRnST4IJnKLnMNOnPQRnST5IJnKLnMNOnPQRnST6IJnKLnMNOnPQRnST7IJnKLnMNOnPQRnST;p:o";
    public static final String CAMPAIGN_TIME_TARGET_WITHOUT_COEF =
            "1ABCDEFGHIJKLMNOPQRSTUVWX2ABCDEFGHIJKLMNOPQRSTUVWX3ABCDEFGHIJKLMNOPQRSTUVWX4ABCDEFGHIJKLMNOPQRSTUVWX5ABCDEFGHIJKLMNOPQRSTUVWX6ABCDEFGHIJKLMNOPQRSTUVWX7ABCDEFGHIJKLMNOPQRSTUVWX9;p:o";
    public static final String CAMPAIGN_TIME_TARGET_COEF_HOLIDAY_WORKING =
            "1An8Bm9";

    private BsExportMultipliersService bsExportMultipliersService;
    private BsExportMultipliersRepository bsExportMultipliersRepository;
    private BidModifierRepository bidModifierRepository;
    private MultipliersYtRepository multipliersYtRepository;

    private final BidModifierKey bidModifierKeyDesktop = new BidModifierKey(
            CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.DESKTOP_MULTIPLIER);
    private final BidModifierKey bidModifierKeySmartTV = new BidModifierKey(
            CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.SMARTTV_MULTIPLIER);
    private final BidModifierKey bidModifierKeyMobile = new BidModifierKey(
            CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.MOBILE_MULTIPLIER);

    @BeforeEach
    void before() {
        LogBsExportEssService logBsExportEssService = mock(LogBsExportEssService.class);
        bsExportMultipliersRepository = mock(BsExportMultipliersRepository.class);
        when(bsExportMultipliersRepository.getSmartTVEnableByCampaignIds(1,Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID,true));

        bidModifierRepository = mock(BidModifierRepository.class);
        multipliersYtRepository = mock(MultipliersYtRepository.class);

        BsOrderIdCalculator bsOrderIdCalculator = mock(BsOrderIdCalculator.class);
        when(bsOrderIdCalculator.calculateOrderIdIfNotExist(1, Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID, ORDER_ID));

        BidModifierMultiplierHandler weatherMultiplierHandler = new WeatherMultiplierHandler();
        BidModifierMultiplierHandler retargetingMultiplierHandler = new RetargetingMultiplierHandler();
        BidModifierMultiplierHandler deviceMultiplierHandler = new DeviceMultiplierHandler(bsExportMultipliersRepository);
        BidModifierMultiplierHandler inventoryTypeHandler = new InventoryTypeHandler();
        TimeMultiplierHandler timeMultiplierHandler = new TimeMultiplierHandler();
        bsExportMultipliersService = new BsExportMultipliersService(logBsExportEssService, bidModifierRepository,
                bsExportMultipliersRepository, multipliersYtRepository, bsOrderIdCalculator,
                List.of(weatherMultiplierHandler, retargetingMultiplierHandler,
                        deviceMultiplierHandler, inventoryTypeHandler),
                timeMultiplierHandler);
    }

    @Test
    void updateMultipliers_UpsertWeather() {
        BidModifierKey bidModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.WEATHER_MULTIPLIER);
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierWeather()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.WEATHER_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withWeatherAdjustments(List.of(new BidModifierWeatherAdjustment()
                                .withPercent(50)
                                .withExpression(List.of(List.of(
                                        new BidModifierWeatherLiteral()
                                                .withParameter(WeatherType.CLOUDNESS)
                                                .withOperation(OperationType.EQ)
                                                .withValue(30))))))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.WEATHER, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.Weather)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.Cloudness)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("30")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateCampaignMultipliers_Upsert() {
        BidModifierKey bidModifierKey = new BidModifierKey(CAMPAIGN_ID, null, BidModifierType.WEATHER_MULTIPLIER);
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierWeather()
                        .withCampaignId(CAMPAIGN_ID)
                        .withType(BidModifierType.WEATHER_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withWeatherAdjustments(List.of(new BidModifierWeatherAdjustment()
                                .withPercent(50)
                                .withExpression(List.of(List.of(
                                        new BidModifierWeatherLiteral()
                                                .withParameter(WeatherType.CLOUDNESS)
                                                .withOperation(OperationType.EQ)
                                                .withValue(30))))))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.WEATHER, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Weather)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.Cloudness)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("30")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_Delete() {
        BsExportMultipliersObject object = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.WEATHER, CAMPAIGN_ID, AD_GROUP_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.Weather)
                        .build())
                .build()));
    }

    @Test
    void updateCampaignMultipliers_Delete() {
        BsExportMultipliersObject object = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.WEATHER, CAMPAIGN_ID, null),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Weather)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_AccessibleGoalChanged_Accessible() {
        when(bsExportMultipliersRepository.getMultiplierIdsByRetargetingConditionIds(
                1, Set.of(RETARGETING_CONDITION_ID, RETARGETING_CONDITION_SECOND_ID)))
                .thenReturn(List.of(HIERARCHICAL_MULTIPLIER_ID, HIERARCHICAL_MULTIPLIER_SECOND_ID));
        BidModifierKey retargetingModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.RETARGETING_MULTIPLIER);
        BidModifierKey retargetingFilterModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.RETARGETING_FILTER);
        when(bidModifierRepository.getBidModifierKeysByIds(
                1, List.of(HIERARCHICAL_MULTIPLIER_ID, HIERARCHICAL_MULTIPLIER_SECOND_ID)))
                .thenReturn(Map.of(
                        HIERARCHICAL_MULTIPLIER_ID, retargetingModifierKey,
                        HIERARCHICAL_MULTIPLIER_SECOND_ID, retargetingFilterModifierKey
                ));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(
                        retargetingModifierKey, bidModifierRetargetingWithDefaults(
                                new BidModifierRetargeting(),
                                HIERARCHICAL_MULTIPLIER_ID,
                                List.of(retargetingAdjustmentWithDefaults(
                                        new BidModifierRetargetingAdjustment(),
                                        50, RETARGETING_CONDITION_ID))
                        ),
                        retargetingFilterModifierKey, bidModifierRetargetingWithDefaults(
                                new BidModifierRetargetingFilter(),
                                HIERARCHICAL_MULTIPLIER_SECOND_ID,
                                List.of(retargetingAdjustmentWithDefaults(
                                        new BidModifierRetargetingFilterAdjustment(),
                                        0, RETARGETING_CONDITION_ID),
                                        retargetingAdjustmentWithDefaults(
                                                new BidModifierRetargetingFilterAdjustment(),
                                                0, RETARGETING_CONDITION_SECOND_ID))
                        ))
                );

        BsExportMultipliersObject object1 = BsExportMultipliersObject.accessibleGoalChanged(
                new AccessibleGoalChangedInfo(RETARGETING_CONDITION_ID),
                0L, "", "");
        BsExportMultipliersObject object2 = BsExportMultipliersObject.accessibleGoalChanged(
                new AccessibleGoalChangedInfo(RETARGETING_CONDITION_SECOND_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object1, object2));

        verify(multipliersYtRepository).changeMultipliers(
                List.of(MultiplierChangeRequest.newBuilder()
                        .setUpsertRequest(DirectMultipliersRow.newBuilder()
                            .setOrderID(ORDER_ID)
                            .setAdGroupID(AD_GROUP_ID)
                            .setType(MultiplierTypeEnum.Retargeting)
                            .setIsEnabled(true)
                            .addAllMultipliers(List.of(
                                    MultiplierAtom.newBuilder()
                                            .setCondition(TargetingExpression.newBuilder()
                                                    .addAND(buildSingleDisjunction(
                                                            KeywordEnum.GoalContextId,
                                                            OperationEnum.MatchGoalContext,
                                                            Long.toString(RETARGETING_CONDITION_ID))
                                                    )
                                                    .build())
                                            .setMultiplier(500000)
                                            .build(),
                                    MultiplierAtom.newBuilder()
                                            .setCondition(TargetingExpression.newBuilder()
                                                    .addAND(buildSingleDisjunction(
                                                            KeywordEnum.GoalContextId,
                                                            OperationEnum.NotMatchGoalContext,
                                                            Long.toString(RETARGETING_CONDITION_ID)))
                                                    .addAND(buildSingleDisjunction(
                                                            KeywordEnum.GoalContextId,
                                                            OperationEnum.NotMatchGoalContext,
                                                            Long.toString(RETARGETING_CONDITION_SECOND_ID)))
                                                    .addAND(buildSingleDisjunction(
                                                            KeywordEnum.PageTargetType,
                                                            OperationEnum.NotEqual,
                                                            Long.toString(RSYA_TARGET_TYPE))).build())
                                            .setMultiplier(0)
                                            .build()))
                            .build())
                    .build()));
    }

    @Test
    void updateMultipliers_AccessibleGoalChanged_Inaccessible() {
        when(bsExportMultipliersRepository.getMultiplierIdsByRetargetingConditionIds(
                1, Set.of(RETARGETING_CONDITION_ID)))
                .thenReturn(List.of(HIERARCHICAL_MULTIPLIER_ID));
        BidModifierKey bidModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.RETARGETING_MULTIPLIER);
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(
                        bidModifierKey, bidModifierRetargetingWithDefaults(new BidModifierRetargeting(),
                                HIERARCHICAL_MULTIPLIER_ID,
                                List.of(new BidModifierRetargetingAdjustment()
                                        .withPercent(50)
                                        .withAccessible(Boolean.FALSE)
                                        .withRetargetingConditionId(RETARGETING_CONDITION_ID)))
                        .withType(BidModifierType.RETARGETING_MULTIPLIER)));

        BsExportMultipliersObject object = BsExportMultipliersObject.accessibleGoalChanged(
                new AccessibleGoalChangedInfo(RETARGETING_CONDITION_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.Retargeting)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_retargetingFilterDeleted() {
        //удалили корректировки с типом retargeting_filter, проверяем что retargeting_multiplier все еще отправляются
        BidModifierKey retargetingModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.RETARGETING_MULTIPLIER);
        BidModifierKey retargetingFilterModifierKey = new BidModifierKey(CAMPAIGN_ID, AD_GROUP_ID,
                BidModifierType.RETARGETING_FILTER);
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), eq(Set
                .of(retargetingModifierKey, retargetingFilterModifierKey))))
                .thenReturn(Map.of(
                        retargetingModifierKey, bidModifierRetargetingWithDefaults(new BidModifierRetargeting(),
                                HIERARCHICAL_MULTIPLIER_ID,
                                List.of(new BidModifierRetargetingAdjustment()
                                        .withPercent(50)
                                        .withAccessible(Boolean.TRUE)
                                        .withRetargetingConditionId(RETARGETING_CONDITION_ID)))
                                .withType(BidModifierType.RETARGETING_MULTIPLIER)));

        BsExportMultipliersObject object = BsExportMultipliersObject.delete(
                new DeleteInfo(MultiplierType.RETARGETING, CAMPAIGN_ID, AD_GROUP_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));
        verify(multipliersYtRepository).changeMultipliers(argThat(t -> t.containsAll(
                List.of(MultiplierChangeRequest.newBuilder()
                            .setUpsertRequest(DirectMultipliersRow.newBuilder()
                                    .setOrderID(ORDER_ID)
                                    .setAdGroupID(AD_GROUP_ID)
                                    .setType(MultiplierTypeEnum.Retargeting)
                                    .setIsEnabled(true)
                                    .addAllMultipliers(List.of(
                                            MultiplierAtom.newBuilder()
                                                    .setCondition(TargetingExpression.newBuilder()
                                                            .addAND(buildSingleDisjunction(
                                                                    KeywordEnum.GoalContextId,
                                                                    OperationEnum.MatchGoalContext,
                                                                    Long.toString(RETARGETING_CONDITION_ID))
                                                            )
                                                            .build())
                                                    .setMultiplier(500000)
                                                    .build()))
                                    .build())
                                .build(),
                        MultiplierChangeRequest.newBuilder()
                                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                                        .setOrderID(ORDER_ID)
                                        .setAdGroupID(AD_GROUP_ID)
                                        .setType(MultiplierTypeEnum.Retargeting)
                                        .build())
                                .build()))));

    }

    private AbstractBidModifierRetargeting bidModifierRetargetingWithDefaults(
            AbstractBidModifierRetargeting bidModifier,
            long hierarchicalMultiplierId,
            List<AbstractBidModifierRetargetingAdjustment> adjustments) {
        return bidModifier
                .withCampaignId(CAMPAIGN_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withType(bidModifier instanceof  BidModifierRetargeting
                        ? BidModifierType.RETARGETING_MULTIPLIER
                        : BidModifierType.RETARGETING_FILTER)
                .withId(hierarchicalMultiplierId)
                .withEnabled(Boolean.TRUE)
                .withRetargetingAdjustments(adjustments);
    }

    private AbstractBidModifierRetargetingAdjustment retargetingAdjustmentWithDefaults(
            AbstractBidModifierRetargetingAdjustment adjustment,
            int percent,
            long retCondId
    ) {
        return adjustment
                .withPercent(percent)
                .withAccessible(Boolean.TRUE)
                .withRetargetingConditionId(retCondId);
    }

    private TargetingExpression.Disjunction buildSingleDisjunction(KeywordEnum keyword,
                                                                   OperationEnum operation,
                                                                   String value) {
        return TargetingExpression.Disjunction.newBuilder()
                .addOR(TargetingExpressionAtom.newBuilder()
                        .setKeyword(keyword)
                        .setOperation(operation)
                        .setValue(value)
                        .build())
                .build();
    }

    @Test
    void updateMultipliers_TimeTargetChanged_WithCoefs() {
        when(bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(
                1, Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID, TimeTarget.parseRawString(CAMPAIGN_TIME_TARGET_COEF)));

        BsExportMultipliersObject object = BsExportMultipliersObject.timeTargetChanged(
                new TimeTargetChangedInfo(CAMPAIGN_ID), 0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Time)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.Timetable)
                                                        .setOperation(OperationEnum.TimeIgnoreHolidayLike)
                                                        .setValue("1234567ABCDEFGHUVWX")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.Timetable)
                                                        .setOperation(OperationEnum.TimeIgnoreHolidayLike)
                                                        .setValue("1234567IKMNPQST")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(1000000)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.Timetable)
                                                        .setOperation(OperationEnum.TimeIgnoreHolidayLike)
                                                        .setValue("1234567JLOR")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(1300000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TimeTargetChanged_CoefsHolidayWorking() {
        when(bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(
                1, Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID, TimeTarget.parseRawString(CAMPAIGN_TIME_TARGET_COEF_HOLIDAY_WORKING)));

        BsExportMultipliersObject object = BsExportMultipliersObject.timeTargetChanged(
                new TimeTargetChangedInfo(CAMPAIGN_ID), 0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Time)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.TimetableSimple)
                                                        .setOperation(OperationEnum.TimeLike)
                                                        .setValue("1BCDEFGHIJKLMNOPQRSTUVWX")
                                                        .build())
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.TimetableSimple)
                                                        .setOperation(OperationEnum.TimeLike)
                                                        .setValue("234567ABCDEFGHIJKLMNOPQRSTUVWX")
                                                        .build())
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.TimetableSimple)
                                                        .setOperation(OperationEnum.TimeLike)
                                                        .setValue("8ACDEFGHIJKLMNOPQRSTUVWX")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.TimetableSimple)
                                                        .setOperation(OperationEnum.TimeLike)
                                                        .setValue("8B")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(1200000)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.TimetableSimple)
                                                        .setOperation(OperationEnum.TimeLike)
                                                        .setValue("1A")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(1300000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TimeTargetChanged_WithoutCoefs() {
        // без коэфициентов не шлём корректировку вообще, это согласуется с поведением старого транспорта
        when(bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(
                1, Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID, TimeTarget.parseRawString(CAMPAIGN_TIME_TARGET_WITHOUT_COEF)));

        BsExportMultipliersObject object = BsExportMultipliersObject.timeTargetChanged(
                new TimeTargetChangedInfo(CAMPAIGN_ID), 0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Time)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TimeTargetChanged_WithoutTimetarget() {
        // без коэфициентов не шлём корректировку вообще, это согласуется с поведением старого транспорта
        when(bsExportMultipliersRepository.getCampaignsTimeTargetWithoutAutobudget(
                1, Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of());

        BsExportMultipliersObject object = BsExportMultipliersObject.timeTargetChanged(
                new TimeTargetChangedInfo(CAMPAIGN_ID), 0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(0L)
                        .setType(MultiplierTypeEnum.Time)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_IosMobileChanged_DbMobileEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.MOBILE_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierMobile()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(50)
                                .withOsType(OsType.IOS))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsMobile)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DetailedDeviceType)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("3")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_AndroidMobileChanged_DbMobileAndroidEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.MOBILE_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierMobile()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(50)
                                .withOsType(OsType.ANDROID))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsMobile)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DetailedDeviceType)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("2")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.SmartTv)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("0")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_AndroidMobileChanged_DbMobileEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.MOBILE_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierMobile()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsMobile)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.SmartTv)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("0")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_MobileChanged_DbMobileAndroidDisabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.MOBILE_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierMobile()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                        .withEnabled(Boolean.FALSE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                .withPercent(50)
                                .withOsType(OsType.ANDROID))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_SmartTVChanged_DbSmartTVEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.SMARTTV_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierSmartTV()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.SMARTTV_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsMobile)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.SmartTv)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_SmartTVChanged_DbSmartTVDisabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.SMARTTV_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierSmartTV()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.SMARTTV_MULTIPLIER)
                        .withEnabled(Boolean.FALSE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_DesktopChanged_DbDesktopEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.DESKTOP_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierDesktop()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsDesktop)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsTablet)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TabletChanged_IOS_DbTabletEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.TABLET_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierTablet()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withTabletAdjustment(new BidModifierTabletAdjustment()
                                .withPercent(40)
                                .withOsType(TabletOsType.IOS)
                        )
                ));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsTablet)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DetailedDeviceType)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("3")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(400000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TabletChanged_ANDROID_DbTabletEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.TABLET_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierTablet()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withTabletAdjustment(new BidModifierTabletAdjustment()
                                .withPercent(40)
                                .withOsType(TabletOsType.ANDROID)
                        )
                ));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsTablet)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DetailedDeviceType)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("2")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(400000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_TabletChanged_ANY_DbTabletEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.TABLET_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierTablet()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withTabletAdjustment(new BidModifierTabletAdjustment()
                                .withPercent(40)
                                .withOsType(null)
                        )
                ));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsTablet)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(400000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_DesktopOnlyChanged_DbDesktopEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.DESKTOP_ONLY_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierDesktopOnly()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_ONLY_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withDesktopOnlyAdjustment(new BidModifierDesktopOnlyAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCondition(TargetingExpression.newBuilder()
                                        .addAND(TargetingExpression.Disjunction.newBuilder()
                                                .addOR(TargetingExpressionAtom.newBuilder()
                                                        .setKeyword(KeywordEnum.DeviceIsDesktop)
                                                        .setOperation(OperationEnum.Equal)
                                                        .setValue("1")
                                                        .build())
                                                .build())
                                        .build())
                                .setMultiplier(500000)
                                .build())
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_DesktopChanged_DbDesktopDisabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.DESKTOP_MULTIPLIER);

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierDesktop()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                        .withEnabled(Boolean.FALSE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                .withPercent(50))));

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_DesktopChanged_DbAllEnabled() {
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKeyDesktop));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(
                        bidModifierKeyMobile, new BidModifierMobile()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withEnabled(Boolean.TRUE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withMobileAdjustment(new BidModifierMobileAdjustment()
                                        .withPercent(50)
                                        .withOsType(OsType.ANDROID)
                                ),
                        bidModifierKeyDesktop, new BidModifierDesktop()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                .withEnabled(Boolean.TRUE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                        .withPercent(60)),
                        bidModifierKeySmartTV, new BidModifierSmartTV()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.SMARTTV_MULTIPLIER)
                                .withEnabled(Boolean.TRUE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                        .withPercent(70))
                        )
                );

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(
                List.of(MultiplierChangeRequest.newBuilder()
                        .setUpsertRequest(DirectMultipliersRow.newBuilder()
                                .setOrderID(ORDER_ID)
                                .setAdGroupID(AD_GROUP_ID)
                                .setType(MultiplierTypeEnum.DeviceType)
                                .setIsEnabled(true)
                                .addMultipliers(MultiplierAtom.newBuilder()
                                        .setCondition(TargetingExpression.newBuilder()
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsMobile)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DetailedDeviceType)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("2")
                                                                .build())
                                                        .build())
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.SmartTv)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("0")
                                                                .build())
                                                        .build())
                                                .build())
                                        .setMultiplier(500000)
                                        .build())
                                .addMultipliers(MultiplierAtom.newBuilder()
                                        .setCondition(TargetingExpression.newBuilder()
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsDesktop)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsTablet)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .setMultiplier(600000)
                                        .build())
                                .addMultipliers(MultiplierAtom.newBuilder()
                                        .setCondition(TargetingExpression.newBuilder()
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsMobile)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.SmartTv)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .setMultiplier(700000)
                                        .build())
                                .build())
                        .build()
                )
        );
    }

    @Test
    void updateMultipliers_DesktopChanged_DbAllDisabled() {
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKeyDesktop));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(
                        bidModifierKeyDesktop, new BidModifierDesktop()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                .withEnabled(Boolean.FALSE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                        .withPercent(50)),
                        bidModifierKeyMobile, new BidModifierMobile()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.MOBILE_MULTIPLIER)
                                .withEnabled(Boolean.FALSE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withMobileAdjustment(new BidModifierMobileAdjustment()
                                        .withPercent(50)
                                        .withOsType(OsType.ANDROID)
                                ),
                        bidModifierKeySmartTV, new BidModifierSmartTV()
                                .withCampaignId(CAMPAIGN_ID)
                                .withAdGroupId(AD_GROUP_ID)
                                .withType(BidModifierType.SMARTTV_MULTIPLIER)
                                .withEnabled(Boolean.FALSE)
                                .withId(HIERARCHICAL_MULTIPLIER_ID)
                                .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                        .withPercent(70))
                        )
                );

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setDeleteRequest(DeleteDirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.DeviceType)
                        .build())
                .build()));
    }

    @Test
    void updateMultipliers_DesktopChanged_DbAllEnabledWithoutSmartTVFeature() {
        when(bsExportMultipliersRepository.getSmartTVEnableByCampaignIds(1,Set.of(CAMPAIGN_ID)))
                .thenReturn(Map.of(CAMPAIGN_ID, false));

        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKeyDesktop));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(
                                bidModifierKeyMobile, new BidModifierMobile()
                                        .withCampaignId(CAMPAIGN_ID)
                                        .withAdGroupId(AD_GROUP_ID)
                                        .withType(BidModifierType.MOBILE_MULTIPLIER)
                                        .withEnabled(Boolean.TRUE)
                                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                                        .withMobileAdjustment(new BidModifierMobileAdjustment()
                                                .withPercent(50)
                                                .withOsType(OsType.ANDROID)
                                        ),
                                bidModifierKeyDesktop, new BidModifierDesktop()
                                        .withCampaignId(CAMPAIGN_ID)
                                        .withAdGroupId(AD_GROUP_ID)
                                        .withType(BidModifierType.DESKTOP_MULTIPLIER)
                                        .withEnabled(Boolean.TRUE)
                                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                                        .withDesktopAdjustment(new BidModifierDesktopAdjustment()
                                                .withPercent(60)),
                                bidModifierKeySmartTV, new BidModifierSmartTV()
                                        .withCampaignId(CAMPAIGN_ID)
                                        .withAdGroupId(AD_GROUP_ID)
                                        .withType(BidModifierType.SMARTTV_MULTIPLIER)
                                        .withEnabled(Boolean.TRUE)
                                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                                        .withSmartTVAdjustment(new BidModifierSmartTVAdjustment()
                                                .withPercent(70))
                        )
                );

        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.DEVICE, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(
                List.of(MultiplierChangeRequest.newBuilder()
                        .setUpsertRequest(DirectMultipliersRow.newBuilder()
                                .setOrderID(ORDER_ID)
                                .setAdGroupID(AD_GROUP_ID)
                                .setType(MultiplierTypeEnum.DeviceType)
                                .setIsEnabled(true)
                                .addMultipliers(MultiplierAtom.newBuilder()
                                        .setCondition(TargetingExpression.newBuilder()
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsMobile)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DetailedDeviceType)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("2")
                                                                .build())
                                                        .build())
                                                .build())
                                        .setMultiplier(500000)
                                        .build())
                                .addMultipliers(MultiplierAtom.newBuilder()
                                        .setCondition(TargetingExpression.newBuilder()
                                                .addAND(TargetingExpression.Disjunction.newBuilder()
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsDesktop)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .addOR(TargetingExpressionAtom.newBuilder()
                                                                .setKeyword(KeywordEnum.DeviceIsTablet)
                                                                .setOperation(OperationEnum.Equal)
                                                                .setValue("1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .setMultiplier(600000)
                                        .build())
                                .build())
                        .build()
                )
        );
    }

    @Test
    void updateMultipliers_BannerTypeChanged_DbInventoryEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.INVENTORY_MULTIPLIER);
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierInventory()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.INVENTORY_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withInventoryAdjustments(List.of(
                                new BidModifierInventoryAdjustment()
                                        .withPercent(30)
                                        .withInventoryType(InventoryType.INAPP),
                                new BidModifierInventoryAdjustment()
                                        .withPercent(70)
                                        .withInventoryType(InventoryType.POSTROLL)
                        ))));
        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.INVENTORY, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.InventoryType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoInApp)
                                        .build())
                                .setMultiplier(300000)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoPostroll)
                                        .build())
                                .setMultiplier(700000)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoDefault)
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .build())
                .build()));

    }
    @Test
    void updateMultipliers_ZeroMultiplies_DbInventoryEnabled() {
        BidModifierKey bidModifierKey = new BidModifierKey(
                CAMPAIGN_ID, AD_GROUP_ID, BidModifierType.INVENTORY_MULTIPLIER);
        when(bidModifierRepository.getBidModifierKeysByIds(1, List.of(HIERARCHICAL_MULTIPLIER_ID)))
                .thenReturn(Map.of(HIERARCHICAL_MULTIPLIER_ID, bidModifierKey));
        when(bidModifierRepository.getBidModifiersByKeys(eq(1), anyCollection()))
                .thenReturn(Map.of(bidModifierKey, new BidModifierInventory()
                        .withCampaignId(CAMPAIGN_ID)
                        .withAdGroupId(AD_GROUP_ID)
                        .withType(BidModifierType.INVENTORY_MULTIPLIER)
                        .withEnabled(Boolean.TRUE)
                        .withId(HIERARCHICAL_MULTIPLIER_ID)
                        .withInventoryAdjustments(List.of(
                                new BidModifierInventoryAdjustment()
                                        .withPercent(0)
                                        .withInventoryType(InventoryType.INAPP),
                                new BidModifierInventoryAdjustment()
                                        .withPercent(0)
                                        .withInventoryType(InventoryType.POSTROLL)
                        ))));
        BsExportMultipliersObject object = BsExportMultipliersObject.upsert(
                new UpsertInfo(MultiplierType.INVENTORY, HIERARCHICAL_MULTIPLIER_ID),
                0L, "", "");
        bsExportMultipliersService.updateMultipliers(1, List.of(object));

        verify(multipliersYtRepository).changeMultipliers(List.of(MultiplierChangeRequest.newBuilder()
                .setUpsertRequest(DirectMultipliersRow.newBuilder()
                        .setOrderID(ORDER_ID)
                        .setAdGroupID(AD_GROUP_ID)
                        .setType(MultiplierTypeEnum.InventoryType)
                        .setIsEnabled(true)
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoInApp)
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoPostroll)
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .addMultipliers(MultiplierAtom.newBuilder()
                                .setCustomCondition(CustomExpression.newBuilder()
                                        .setInventoryType(InventoryTypeEnum.VideoDefault)
                                        .build())
                                .setMultiplier(0)
                                .build())
                        .build())
                .build()));

    }
}
