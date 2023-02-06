package ru.yandex.autotests.direct.cmd.groups.dynamic;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.Geo;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.groups.CreateGroupBsSyncedBaseTest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.DynamicBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static org.hamcrest.core.IsEqual.equalTo;

//Task: TESTIRT-9418.
@Aqua.Test
@Description("Статус синхронизации динамический групп")
@Stories(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_DYNAMIC_AD_GROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.DYNAMIC)
@Tag(TrunkTag.YES)
@Tag(TestFeatures.Groups.SAVE_DYNAMIC_AD_GROUPS)
@Tag(TestFeatures.GROUPS)
public class DtoBsSyncedTest extends CreateGroupBsSyncedBaseTest {
    public DtoBsSyncedTest() {
        bannersRule = new DynamicBannersRule().withUlogin(getClient());
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test
    @Description("Изменение домена для дто, статус синхронизации группы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9778")
    public void changeDomainNameCheckGroupStatus() {
        Group group = bannersRule.getCurrentGroup().withMainDomain("vk.com");

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("Изменение домена для дто, статус синхронизации баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9779")
    public void changeDomainNameCheckBannerStatus() {
        Group group = bannersRule.getCurrentGroup().withMainDomain("vk.com");

        saveAndCheckBanner(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Override
    @Description("изменение геотаргетинга, проверка статуса синхронизации баннеров")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9772")
    public void changeGeoTargetingCheckGroupStatus() {
        Group group = bannersRule.getCurrentGroup().withGeo(Geo.GERMANY.getGeo());
        saveAndCheckBanner(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("Изменение мобильных коэффициентов на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9777")
    public void changeMobileKOnGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.setHierarchicalMultipliers(new HierarchicalMultipliers()
                .withMobileMultiplier(new MobileMultiplier().withMultiplierPct("100")));
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Override
    protected String getClient() {
        return Logins.DEFAULT_CLIENT;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9773")
    public void changeNothingCheckBannerStatus() {
        super.changeNothingCheckBannerStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9774")
    public void changeMinusWordsCheckGroup() {
        super.changeMinusWordsCheckGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9775")
    public void changeDemographKOnGroup() {
        super.changeDemographKOnGroup();
    }

}
