package ru.yandex.autotests.direct.cmd.updateshowconditions.mobile;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.direct.cmd.rules.BannersRule;
import ru.yandex.autotests.direct.cmd.rules.MobileBannersRule;
import ru.yandex.autotests.direct.cmd.tags.CmdTag;
import ru.yandex.autotests.direct.cmd.tags.ObjectTag;
import ru.yandex.autotests.direct.cmd.tags.TrunkTag;
import ru.yandex.autotests.direct.cmd.updateshowconditions.base.SyncStatusesAddPhrasesAjaxUpdateShowCondTestBase;
import ru.yandex.autotests.direct.httpclient.TestFeatures;
import ru.yandex.qatools.Tag;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Aqua.Test
@Description("Проверка статусов синхронизации при добавлении фраз в группу мобильной кампании со страницы статистики cmd_ajaxUpdateShowConditions")
@Stories(TestFeatures.UpdateShowConditions.AJAX_UPDATE_SHOW_CONDITIONS)
@Features(TestFeatures.UPDATE_SHOW_CONDITIONS)
@Tag(CmdTag.AJAX_UPDATE_SHOW_CONDITIONS)
@Tag(ObjectTag.GROUP)
@Tag(ObjectTag.PHRASE)
@Tag(TrunkTag.YES)
public class SyncStatusesAddPhrasesMobileCampAjaxUpdateShowCondTest extends SyncStatusesAddPhrasesAjaxUpdateShowCondTestBase {
    @Override
    protected BannersRule getBannerRule() {
        return new MobileBannersRule();
    }
}
