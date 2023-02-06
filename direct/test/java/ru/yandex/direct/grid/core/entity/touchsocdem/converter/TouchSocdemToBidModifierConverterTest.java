package ru.yandex.direct.grid.core.entity.touchsocdem.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.grid.core.entity.touchsocdem.service.converter.GridTouchSocdemConverter;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdem;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._0_17;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._45_54;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._55_;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.FEMALE;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.MALE;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeBidModifier;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeSocdem;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.zeroMultiplier;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_18;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_45;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class TouchSocdemToBidModifierConverterTest {

    public static Collection<Object[]> parametersForTestSocdemToBidModifier() {
        ArrayList<TouchSocdemTestCases.Case> cases = new ArrayList<>(TouchSocdemTestCases.getCases());
        cases.add(new TouchSocdemTestCases.Case(
                "[all genders], 18-45",
                makeSocdem(List.of(MALE, FEMALE), AGE_18, AGE_45),
                makeBidModifier(
                        zeroMultiplier(null, _0_17),
                        zeroMultiplier(null, _45_54),
                        zeroMultiplier(null, _55_)
                )
        ));
        return mapList(cases, c -> new Object[]{
                c.getName(),
                c.getTouchSocdem(),
                c.getBidModifier()
        });
    }

    @Test
    @Parameters
    @TestCaseName("case = {0}")
    public void testSocdemToBidModifier(
            @SuppressWarnings("unused") String name,
            GdiTouchSocdem input,
            @Nullable BidModifierDemographics expected
    ) {
        var result = GridTouchSocdemConverter.toSocdemBidModifier(input);
        if (expected == null) {
            assertThat(result, nullValue());
        } else {
            assertThat(result, notNullValue());
            // списки Adjustment в корректировке проверяем отдельно, т.к. в них не важен порядок
            Set<BidModifierDemographicsAdjustment> resultAdjSet =
                    new HashSet<>(result.getDemographicsAdjustments());
            Set<BidModifierDemographicsAdjustment> expectedAdjSet =
                    new HashSet<>(expected.getDemographicsAdjustments());
            result.setDemographicsAdjustments(null);
            expected.setDemographicsAdjustments(null);
            assertThat(result, beanDiffer(expected));
            assertThat(resultAdjSet, beanDiffer(expectedAdjSet));
        }
    }
}
