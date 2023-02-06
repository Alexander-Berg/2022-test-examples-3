package ru.yandex.direct.api.v5.entity.bidmodifiers;

import java.util.Arrays;
import java.util.Set;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.DemographicsAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.MobileAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.RegionalAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.RetargetingAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.SerpLayoutAdjustmentFieldEnum;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentFieldEnum;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

/**
 * Проверяется соответствие значений один к одному BidModifierAnyFieldEnum и перечислений BidModifierAnyFieldEnum
 * MobileAdjustmentFieldEnum, RegionalAdjustmentFieldEnum, DemographicsAdjustmentFieldEnum,
 * RegionalAdjustmentFieldEnum, SmartAdAdjustmentFieldEnum и SerpLayoutAdjustmentFieldEnum
 * Чтобы при заведении новых значений все было проставлено правильно
 */
public class BidModifierAnyFieldEnumTest {

    @Test
    public void fromBidModifierFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(BidModifierFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromBidModifierFieldEnum).collect(toSet());
        assertEquals(BidModifierFieldEnum.values().length, result.size());
    }

    @Test
    public void fromMobileAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(MobileAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromMobileAdjustmentFieldEnum).collect(toSet());
        assertEquals(MobileAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void fromRegionalAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(RegionalAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromRegionalAdjustmentFieldEnum).collect(toSet());
        assertEquals(RegionalAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void fromDemographicsAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(DemographicsAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromDemographicsAdjustmentFieldEnum).collect(toSet());
        assertEquals(DemographicsAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void fromRetargetingAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(RetargetingAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromRetargetingAdjustmentFieldEnum).collect(toSet());
        assertEquals(RetargetingAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void fromSmartAdAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(SmartAdAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromSmartAdjustmentFieldEnum).collect(toSet());
        assertEquals(SmartAdAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void fromSerpLayoutAdjustmentFieldEnum() {
        Set<BidModifierAnyFieldEnum> result = Arrays.stream(SerpLayoutAdjustmentFieldEnum.values()).map(
                BidModifierAnyFieldEnum::fromSerpLayoutAdjustmentFieldEnum).collect(toSet());
        assertEquals(SerpLayoutAdjustmentFieldEnum.values().length, result.size());
    }

    @Test
    public void allValuesHaveDifferentFields() {
        Set<Enum> result = Arrays.stream(BidModifierAnyFieldEnum.values()).map(
                BidModifierAnyFieldEnum::getValue).collect(toSet());
        assertEquals(BidModifierAnyFieldEnum.values().length, result.size());
    }
}
