package ru.yandex.autotests.direct.cmd.adjustment.absegment;

import java.util.Arrays;
import java.util.Collection;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.adjustment.common.MultiplierTestBase;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.AbSegmentMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.AbSegmentMultiplierData;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.counters.MetrikaCountersData;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;

@Aqua.Test
@Description("Проверка в контроллера saveCamp: сохранение корректировок с граничными условиями")
@Stories(TestFeatures.Campaigns.SAVE_CAMP)
@Features(TestFeatures.CAMPAIGNS)
@Tag(TrunkTag.YES)
@Tag(CmdTag.SAVE_CAMP)
@RunWith(Parameterized.class)
@Ignore("Старое редактирование выключено на 100% пользователей")
public class CampaignAbSegmentMultiplierTest extends MultiplierTestBase {

    @Parameterized.Parameter
    public String multiplierPct;

    @Parameterized.Parameters(name = "Значение параметра multiplier_pct = {0}")
    public static Collection<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {"1300"},
                {"100"},
                {"0"}
        });
    }

    @Override
    public String getClient() {
        return "at-direct-absegment10";
    }

    @Override
    protected HierarchicalMultipliers getHierarchicalMultipliers() {
        return new HierarchicalMultipliers().
                withAbSegmentMultiplier(new AbSegmentMultiplier()
                        .withAbSegments(singletonList(new AbSegmentMultiplierData()
                                .withMultiplierPct(multiplierPct)
                                .withSectionId(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSectionId())
                                .withSegmentId(MetrikaCountersData.DEFAULT_COUNTER.getFirstAbSegmentId()))
                        ).withEnabled(true));
    }

    @Test
    @TestCaseId("11043")
    public void checkSaveAbSegmentMultiplierAtSaveNewCamp() {
        super.checkSaveMobileMultiplierAtSaveNewCamp();
    }

    @Test
    @TestCaseId("11044")
    public void checkSaveAbSegmentMultiplierAtSaveCamp() {
        super.checkSaveMobileMultiplierAtSaveCamp();
    }
}
