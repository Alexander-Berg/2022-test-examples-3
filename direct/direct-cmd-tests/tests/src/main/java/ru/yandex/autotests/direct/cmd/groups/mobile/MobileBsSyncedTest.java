package ru.yandex.autotests.direct.cmd.groups.mobile;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.groups.CreateGroupBsSyncedBaseTest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CampTypeTag;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.util.BeanLoadHelper;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.autotests.directapi.enums.StatusBsSynced;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;

//Task: TESTIRT-9418.
@Aqua.Test
@Description("Проверка статуса синхронизации мобильных-группы")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_MOBILE_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.MOBILE)
@Tag(TrunkTag.YES)
@Tag(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Tag(TestFeatures.GROUPS)
public class MobileBsSyncedTest extends CreateGroupBsSyncedBaseTest {
    public MobileBsSyncedTest() {
        bannersRule = new MobileBannersRule().withUlogin(getClient());
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Test
    @Description("Изменение таргетинга на мобильное устройство в группе РМП")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9812")
    public void changeTargetingOnMobileDevice() {
        Group group = bannersRule.getCurrentGroup();

        group.getDeviceTypeTargeting().remove(1);
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("Изменение таргетинга на тип подключения в группе РМП")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9813")
    public void changeTargetingOnTypeConnection() {
        Group group = bannersRule.getCurrentGroup();

        group.getNetworkTargeting().remove(1);

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("Изменение минимальной версии ОС")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9814")
    public void changeMinimalOSVersion() {
        Group group = bannersRule.getCurrentGroup()
                .withMinOsVersion("8.0");

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("изменение фраз на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9811")
    public void changePhraseCheckGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withPhrases(singletonList(
                        BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class)
                                .withPhrase(RandomStringUtils.randomAlphabetic(20))));

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Override
    protected String getClient() {
        return Logins.DEFAULT_CLIENT;
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9806")
    public void changeGeoTargetingCheckGroupStatus() {
        super.changeGeoTargetingCheckGroupStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9807")
    public void changeNothingCheckBannerStatus() {
        super.changeNothingCheckBannerStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9808")
    public void changeMinusWordsCheckGroup() {
        super.changeMinusWordsCheckGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9809")
    public void changeDemographKOnGroup() {
        super.changeDemographKOnGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9810")
    public void changeRetargetingKOnGroup() {
        super.changeRetargetingKOnGroup();
    }
}
