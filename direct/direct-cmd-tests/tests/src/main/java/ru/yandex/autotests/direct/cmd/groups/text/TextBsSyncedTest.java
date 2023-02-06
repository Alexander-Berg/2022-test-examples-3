package ru.yandex.autotests.direct.cmd.groups.text;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.data.CmdBeans;
import ru.yandex.autotests.direct.cmd.data.Logins;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.HierarchicalMultipliers;
import ru.yandex.autotests.direct.cmd.data.commons.adjustment.MobileMultiplier;
import ru.yandex.autotests.direct.cmd.data.commons.group.Group;
import ru.yandex.autotests.direct.cmd.data.commons.phrase.Phrase;
import ru.yandex.autotests.direct.cmd.groups.CreateGroupBsSyncedBaseTest;
import ru.yandex.autotests.direct.cmd.rules.DirectCmdRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
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
@Description("Проверка статуса синхронизации смарт-группы")
@Stories(TestFeatures.Groups.BANNER_MULTI_SAVE)
@Features(TestFeatures.GROUPS)
@Tag(CmdTag.SAVE_TEXT_ADGROUPS)
@Tag(ObjectTag.GROUP)
@Tag(CampTypeTag.TEXT)
@Tag(TrunkTag.YES)
public class TextBsSyncedTest extends CreateGroupBsSyncedBaseTest {
    public TextBsSyncedTest() {
        bannersRule = new TextBannersRule().withUlogin(getClient());
        cmdRule = DirectCmdRule.defaultRule().withRules(bannersRule);
    }

    @Override
    protected String getClient() {
        return Logins.DEFAULT_CLIENT;
    }


    @Test
    @Description("изменение фраз на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9867")
    public void changePhraseCheckGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withPhrases(singletonList(
                        BeanLoadHelper.loadCmdBean(CmdBeans.COMMON_REQUEST_PHRASE_DEFAULT2, Phrase.class)
                                .withPhrase(RandomStringUtils.randomAlphabetic(20))));

        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("добавление phraseid")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9869")
    public void changeHasPhraseIdHrefs() {
        Group group = bannersRule.getCurrentGroup();
        group.getBanners().get(0).withHref("http://www.ya.ru/?phraseId={phrase_id}");
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Description("Изменение мобильных коэффициентов на группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("9868")
    public void changeMobileKOnGroup() {
        Group group = bannersRule.getCurrentGroup()
                .withAdGroupID(String.valueOf(bannersRule.getGroupId()));
        group.setHierarchicalMultipliers(new HierarchicalMultipliers()
                .withMobileMultiplier(new MobileMultiplier().withMultiplierPct("100")));
        saveAndCheckGroup(group, equalTo(StatusBsSynced.NO.toString()));
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9862")
    public void changeGeoTargetingCheckGroupStatus() {
        super.changeGeoTargetingCheckGroupStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9863")
    public void changeNothingCheckBannerStatus() {
        super.changeNothingCheckBannerStatus();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9864")
    public void changeMinusWordsCheckGroup() {
        super.changeMinusWordsCheckGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9865")
    public void changeDemographKOnGroup() {
        super.changeDemographKOnGroup();
    }

    @Test
    @Override
    @ru.yandex.qatools.allure.annotations.TestCaseId("9866")
    public void changeRetargetingKOnGroup() {
        super.changeRetargetingKOnGroup();
    }
}
