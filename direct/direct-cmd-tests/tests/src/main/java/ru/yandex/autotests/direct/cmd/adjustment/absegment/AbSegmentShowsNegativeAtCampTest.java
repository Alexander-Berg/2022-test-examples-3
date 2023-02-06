package ru.yandex.autotests.direct.cmd.adjustment.absegment;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierShowsNegativeAtCampTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.AbSegmentMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.AbSegmentMultiplierData;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.direct.httpclient.data.campaigns.AbSegmentMultiplierPctErrors;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import static java.util.Collections.singletonList;

@Aqua.Test
@Description("Проверка ошибок при настройке цен на мобильных устройствах кампании (параметр mobile_multiplier_pct)")
@Stories(TestFeatures.Campaigns.ADJUSTMENT_AB_SEGMENT)
@Features(TestFeatures.CAMPAIGNS)
@Tag(CmdTag.SAVE_NEW_CAMP)
@Tag(CmdTag.SAVE_CAMP)
@Tag(ObjectTag.ADJUSTMENT)
@Tag(CampTypeTag.TEXT)
@RunWith(Parameterized.class)
public class AbSegmentShowsNegativeAtCampTest extends MultiplierShowsNegativeAtCampTestBase {

    @Parameterized.Parameter(0)
    public AbSegmentMultiplierPctErrors abSegmentMultiplierPctErrors;
    @Parameterized.Parameter(1)
    public String description;

    @Parameterized.Parameters(name = "Значение параметра ab_segment_multiplier_pct {0} ({1})")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {AbSegmentMultiplierPctErrors.TOO_SHORT, "Сохранение значения меньше допустимого"},
                {AbSegmentMultiplierPctErrors.TOO_LONG, "Сохранение значения больше допустимого"},
                {AbSegmentMultiplierPctErrors.NOT_AN_INT, "Сохранение нецелого значения"},
                {AbSegmentMultiplierPctErrors.NOT_AN_INT_TOO_SHORT, "Сохранение нецелого значения меньше допустимого"},
                {AbSegmentMultiplierPctErrors.NOT_AN_INT_TOO_LONG, "Сохранение нецелого значения больше допустимого"},
        });
    }

    @Override
    public String getClient() {
        return "at-direct-absegment11";
    }

    @Override
    protected String getErrorText() {
        return abSegmentMultiplierPctErrors.getErrorText();
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withAbSegmentMultiplier(new AbSegmentMultiplier()
                        .withAbSegments(singletonList(new AbSegmentMultiplierData()
                                .withMultiplierPct(abSegmentMultiplierPctErrors.getValue())
                                .withSectionId(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId())
                                .withSegmentId(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId()))
                        ));
    }

    @Test
    @TestCaseId("11041")
    public void checkSaveInvalidAbSegmentMultiplierAtSaveNewCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @TestCaseId("11042")
    public void checkSaveInvalidAbSegmentMultiplierAtSaveCamp() {
        super.checkSaveInvalidMobileMultiplierAtSaveCamp();
    }
}
