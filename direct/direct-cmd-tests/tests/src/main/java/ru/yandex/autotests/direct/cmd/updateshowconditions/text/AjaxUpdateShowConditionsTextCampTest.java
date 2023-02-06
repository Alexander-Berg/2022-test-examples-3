package ru.yandex.autotests.direct.cmd.updateshowconditions.text;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.TextBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.base.AjaxUpdateShowConditionsTestBase;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;


@Aqua.Test
@Description("Добавление фраз в группу текстовой кампании со страницы статистики cmd_ajaxUpdateShowConditions")
@Stories(TestFeatures.UpdateShowConditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.UPDATE_SHOW_CONDITIONS)
@Tag(CmdTag.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.PHRASE)
@Tag(TrunkTag.YES)
public class AjaxUpdateShowConditionsTextCampTest extends AjaxUpdateShowConditionsTestBase {
    @Override
    protected BannersRule getBannerRule() {
        return new TextBannersRule();
    }

    @Test
    @Description("Возможно добавление одной и той же фразы в группу, с заменой фразы")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10625")
    public void addOnePhraseTwice() {
        super.addOnePhraseTwice();
    }

    @Test
    @Description("При превышении максимального числа фраз в группе создается новая группа")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10624")
    public void addPhrasesWithGroupCopy() {
        super.addPhrasesWithGroupCopy();
    }

    @Test
    @Description("Фраза добавляется с приоритетом для автобюджетной стратегии")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10623")
    public void phraseForAutobudgetStrategy() {
        super.phraseForAutobudgetStrategy();
    }

    @Test
    @Description("Фраза добавляется в группу с разными ценами на контексте и поиске")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10621")
    public void phraseWithDifferentContextAndSearchPrices() {
        super.phraseWithDifferentContextAndSearchPrices();
    }

    @Test
    @Description("Фраза добавляется в группу")
    @ru.yandex.qatools.allure.annotations.TestCaseId("10622")
    public void addOnePhrase() {
        super.addOnePhrase();
    }

}
