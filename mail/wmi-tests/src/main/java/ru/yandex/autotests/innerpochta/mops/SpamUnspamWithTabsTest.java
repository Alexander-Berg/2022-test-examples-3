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
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
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
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("[MOPS] Ручка spam и unspam. Автоматическое перемещение между табами")
@Description("Проверяем автоматическую установку/снятие таба")
@Features(MyFeatures.MOPS)
@Issue("MAILPG-2410")
@Credentials(loginGroup = "SpamUnspamWithTabsMopsTest")
@RunWith(DataProviderRunner.class)
public class SpamUnspamWithTabsTest extends MopsBaseTest {
    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @UseDataProvider("existingTabs")
    @Title("SPAM должен выставлять письму пустой таб при перемещении")
    public void shouldSetEmptyTabOnSpam(Tab tab) throws Exception {
        val mid = makeMessageForTab(authClient, tab);

        spam(new MidsSource(mid)).post(shouldBe(okSync()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient)
                .parsed().getEnvelopes().get(0);
        assertThat("Не найдено письмо с mid=" + mid,
                envelope.getMid(), equalTo(mid));
        assertThat("Письмо должно лежать в папке \"Спам\"",
                envelope.getFid(), equalTo(folderList.spamFID()));
        assertThat("У письма должен быть пустой таб",
                envelope.getTab(), equalTo(Tab.EMPTY.getName()));
    }

    @Test
    @UseDataProvider("existingTabs")
    @Title("UNSPAM должен выставлять письму заданный таб при перемещении во \"Входящие\"")
    public void shouldMoveToDestTabTabOnUnspamWithMoveToInbox(Tab tab) throws Exception {
        val mid = makeMessageForTab(authClient, Tab.EMPTY);

        unspam(new MidsSource(mid))
                .withDestFid(folderList.inboxFID())
                .withDestTab(tab.getName())
                .post(shouldBe(okSync()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient)
                .parsed().getEnvelopes().get(0);
        assertThat("Не найдено письмо с mid=" + mid,
                envelope.getMid(), equalTo(mid));
        assertThat("Письмо должно лежать в папке \"Входящие\"",
                envelope.getFid(), equalTo(folderList.inboxFID()));
        assertThat("У письма должен быть таб [" + tab.getName() + "]",
                envelope.getTab(), equalTo(tab.getName()));
    }

    @Test
    @Title("UNSPAM должен выставлять письму таб relevant при перемещении во \"Входящие\", если не задан dest_tab")
    public void shouldMoveToRelevantTabTabOnUnspamWithMoveToInboxWithoutDestTab() throws Exception {
        val mid = makeMessageForTab(authClient, Tab.EMPTY);

        unspam(new MidsSource(mid))
                .withDestFid(folderList.inboxFID())
                .post(shouldBe(okSync()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient)
                .parsed().getEnvelopes().get(0);
        assertThat("Не найдено письмо с mid=" + mid,
                envelope.getMid(), equalTo(mid));
        assertThat("Письмо должно лежать в папке \"Входящие\"",
                envelope.getFid(), equalTo(folderList.inboxFID()));
        assertThat("У письма должен быть таб [relevant]",
                envelope.getTab(), equalTo(Tab.RELEVANT.getName()));
    }

    @Test
    @UseDataProvider("existingTabs")
    @Title("UNSPAM должен игнорировать dest_tab таб при перемещении в пользовательскую папку")
    public void shouldIgnoreDestTabTabOnUnspamWithMoveToInbox(Tab tab) throws Exception {
        val fid = newFolder(getRandomString());
        val mid = makeMessageForTab(authClient, Tab.EMPTY);

        unspam(new MidsSource(mid))
                .withDestFid(fid)
                .withDestTab(tab.getName())
                .post(shouldBe(okSync()));

        val envelope = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient)
                .parsed().getEnvelopes().get(0);
        assertThat("Не найдено письмо с mid=" + mid,
                envelope.getMid(), equalTo(mid));
        assertThat("Письмо должно лежать в папке c fid=" + fid,
                envelope.getFid(), equalTo(fid));
        assertThat("У письма должен быть пустой таб",
                envelope.getTab(), equalTo(Tab.EMPTY.getName()));
    }
}
