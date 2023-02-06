package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.Arrays;
import java.util.Set;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierTypeEnum;
import com.yandex.direct.api.v5.general.AgeRangeEnum;
import com.yandex.direct.api.v5.general.GenderEnum;
import com.yandex.direct.api.v5.general.SerpLayoutEnum;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.bidmodifier.TrafaretPosition;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;

/**
 * Проверяется соответствие значений один к одному у перечислений
 * BidModifierLevel - BidModifierLevelEnum
 * GenderEnum - GenderType
 * AgeRangeEnum - AgeType
 * HierarchicalMultipliersType - BidModifierTypeEnum
 * PositionEnum - TrafaretPosition
 * Чтобы при заведении новых значений было добалено соответствие
 */
public class GetBidModifiersDelegateConvertEnumsTest {

    @Test
    public void convertLevel() {
        Set<BidModifierLevel> result = Arrays.stream(BidModifierLevelEnum.values()).map(
                GetBidModifiersDelegate::convertLevel).collect(toSet());
        assertEquals(BidModifierLevelEnum.values().length, result.size());
    }

    @Test
    public void genderTypeToExternal() {
        Set<GenderEnum> result = Arrays.stream(GenderType.values()).map(
                GetBidModifiersDelegate::genderTypeToExternal).collect(toSet());
        assertEquals(GenderEnum.values().length, result.size());
    }

    @Test
    public void ageTypeToExternal() {
        // сейчас UNKNOWN не поддерживается в api
        Set<AgeType> ignored = Set.of(AgeType.UNKNOWN);
        Set<AgeRangeEnum> result = Arrays.stream(AgeType.values())
                .filter(ageType -> !ignored.contains(ageType))
                .map(GetBidModifiersDelegate::ageTypeToExternal)
                .collect(toSet());
        assertEquals(AgeRangeEnum.values().length, result.size());
    }

    @Test
    public void hierarchicalMultipliersTypeFromExternal() {
        Set<BidModifierType> result = Arrays.stream(BidModifierTypeEnum.values()).map(
                GetBidModifiersDelegate::hierarchicalMultipliersTypeFromExternal).collect(toSet());
        assertEquals(BidModifierTypeEnum.values().length, result.size());
    }

    @Test
    public void positionToExternal() {
        Set<SerpLayoutEnum> result = Arrays.stream(TrafaretPosition.values()).map(
                GetBidModifiersDelegate::positionToExternal).collect(toSet());
        assertEquals(SerpLayoutEnum.values().length, result.size());
    }
}
