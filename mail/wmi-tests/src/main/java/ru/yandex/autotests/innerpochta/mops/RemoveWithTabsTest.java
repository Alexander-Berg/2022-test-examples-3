package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;

@Aqua.Test
@Title("[MOPS] Ручка remove. Автоматическое перемещение между табами")
@Description("При удалении письма ему должен устанавливаться пустой таб")
@Features(MyFeatures.MOPS)
@Issue("MAILPG-2410")
@Credentials(loginGroup = "RemoveWithTabsMopsTest")
@RunWith(DataProviderRunner.class)
public class RemoveWithTabsTest extends MopsBaseTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @UseDataProvider("existingTabs")
    @Title("REMOVE должен выставлять письму пустой таб при перемещении")
    public void shouldSetEmptyTabOnRemove(Tabs.Tab tab) throws Exception {
        val mid = makeMessageForTab(authClient, tab);

        remove(new MidsSource(mid)).post(shouldBe(okSync()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient)
                .parsed().getEnvelopes().get(0);
        assertThat("Не найдено письмо с mid=" + mid,
                envelope.getMid(), equalTo(mid));
        assertThat("Письмо должно лежать в папке \"Удаленные\"",
                envelope.getFid(), equalTo(folderList.deletedFID()));
        assertThat("У письма должен быть пустой таб",
                envelope.getTab(), equalTo(Tabs.Tab.EMPTY.getName()));
    }
}
