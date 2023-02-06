package ru.yandex.direct.grid.core.entity.touchsocdem.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.grid.core.entity.touchsocdem.service.converter.GridTouchSocdemConverter;
import ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdem;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._0_17;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._25_34;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._45_54;
import static ru.yandex.direct.core.entity.bidmodifier.AgeType._55_;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.FEMALE;
import static ru.yandex.direct.core.entity.bidmodifier.GenderType.MALE;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeBidModifier;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeDisabledBidModifier;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeMultiplier;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.makeSocdem;
import static ru.yandex.direct.grid.core.entity.touchsocdem.converter.TouchSocdemTestCases.zeroMultiplier;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_0;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_18;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_45;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_55;
import static ru.yandex.direct.grid.processing.model.touchsocdem.GdiTouchSocdemAgePoint.AGE_INF;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class BidModifierToTouchSocdemConverterTest {

    public Collection<Object[]> parametersForTestBidModifierToTouchSocdem() {
        ArrayList<TouchSocdemTestCases.Case> cases = new ArrayList<>(TouchSocdemTestCases.getCases());
        cases.addAll(List.of(
                new TouchSocdemTestCases.Case(
                        "[], 18-45, explicit genders",
                        makeSocdem(emptyList(), AGE_18, AGE_45, true),
                        makeBidModifier(
                                zeroMultiplier(MALE, _0_17),
                                zeroMultiplier(null, _45_54),
                                zeroMultiplier(MALE, _55_),
                                zeroMultiplier(FEMALE, _0_17),
                                zeroMultiplier(FEMALE, _55_)
                        )
                ),
                new TouchSocdemTestCases.Case(
                        "[], 18-55. incompatible percent",
                        makeSocdem(emptyList(), AGE_18, AGE_55, true),
                        makeBidModifier(
                                zeroMultiplier(null, _0_17),
                                makeMultiplier(null, _45_54, 1),
                                zeroMultiplier(null, _55_)
                        )
                ),
                new TouchSocdemTestCases.Case(
                        "[MALE], 18-45. missing age",
                        makeSocdem(List.of(MALE), AGE_18, AGE_45, true),
                        makeBidModifier(
                                zeroMultiplier(FEMALE, null),
                                zeroMultiplier(MALE, _0_17),
                                zeroMultiplier(MALE, _25_34),
                                zeroMultiplier(MALE, _45_54),
                                zeroMultiplier(MALE, _55_)
                        )
                ),
                new TouchSocdemTestCases.Case(
                        "[], 0-inf, disabled bid modifier",
                        makeSocdem(emptyList(), AGE_0, AGE_INF, true),
                        makeDisabledBidModifier()
                ),
                new TouchSocdemTestCases.Case(
                        "[], 0-inf, no adjustments",
                        makeSocdem(emptyList(), AGE_0, AGE_INF, true),
                        makeBidModifier()
                )
        ));
        return mapList(cases, c -> new Object[]{
                c.getName(),
                c.getBidModifier(),
                c.getTouchSocdem(),
        });
    }

    @Test
    @Parameters
    @TestCaseName("case = {0}")
    public void testBidModifierToTouchSocdem(
            @SuppressWarnings("unused") String name,
            BidModifierDemographics input,
            GdiTouchSocdem expected
    ) {
        var result = GridTouchSocdemConverter.socdemBidModifierToTouchSocdem(input);
        assertThat(result, beanDiffer(expected));
    }
}
