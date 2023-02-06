package ru.yandex.autotests.innerpochta.mops;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import lombok.val;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.Tab;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand;
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
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeEnvelopeForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeMessageForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.Tabs.makeThreadForTab;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FilterSearchCommand.filterSearch;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasThreadsInTab;

@Aqua.Test
@Title("[MOPS] Ручка complex_move. Автоматическое перемещение между табами")
@Description("Проверяем автоматическую установку/снятие таба в зависимости от целевой папки")
@Features(MyFeatures.MOPS)
@Issue("MAILPG-2410")
@Credentials(loginGroup = "ComplexMoveWithTabsMopsTest")
@RunWith(DataProviderRunner.class)
public class ComplexMoveWithTabsTest extends MopsBaseTest {
    private static final String USER_FOLDER_NAME = getRandomString();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).before(true).all();

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @UseDataProvider("existingTabs")
    @Title("COMPLEX_MOVE в пользовательскую папку должен выставлять письму пустой таб")
    public void shouldRemoveTabWhenComplexMoveFromInbox(Tab tab) throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val mid = makeMessageForTab(authClient, tab);

        complexMove(fid, new MidsSource(mid)).post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть пустой таб",
                search.tab(), equalTo(Tab.EMPTY.getName()));
    }

    @Test
    @UseDataProvider("existingTabs")
    @Title("COMPLEX_MOVE в пользовательскую папку должен выставлять письму пустой таб, независимо от dest_tab")
    public void shouldIgnoreDestTabWhenComplexMoveFromInbox(Tab tab) throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val mid = makeMessageForTab(authClient, Tab.RELEVANT);

        complexMove(fid, new MidsSource(mid)).withDestTab(tab.getName()).post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть пустой таб ",
                search.tab(), equalTo(Tab.EMPTY.getName()));
    }

    @Test
    @Title("COMPLEX_MOVE в пользовательскую папку должен работать, если передан пустой dest_tab")
    public void shouldWorkOnDestTabWhenComplexMoveFromInbox() throws Exception {
        val fid = newFolder(USER_FOLDER_NAME);
        val mid = makeMessageForTab(authClient, Tab.RELEVANT);

        complexMove(fid, new MidsSource(mid)).withDestTab("").post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть пустой таб ",
                search.tab(), equalTo(Tab.EMPTY.getName()));
    }

    @Test
    @UseDataProvider("existingTabs")
    @Title("COMPLEX_MOVE в папку \"Входящие\" должен выставлять заданный таб")
    public void shouldSetDestTabWhenComplexMoveToInbox(Tab tab) throws Exception {
        val context = sendMail();
        val mid = context.firstMid();

        complexMove(folderList.inboxFID(), new MidsSource(mid)).withDestTab(tab.getName()).post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть таб [" + tab.getName() + "]",
                search.tab(), equalTo(tab.getName()));
    }

    @Test
    @Title("COMPLEX_MOVE в папку \"Входящие\" должен выставлять таб relevant, если не задан dest_tab")
    public void shouldSetRelevantTabWhenComplexMoveToInboxWithoutDestTab() throws Exception {
        val context = sendMail();
        val mid = context.firstMid();

        complexMove(folderList.inboxFID(), new MidsSource(mid)).post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть таб [relevant]",
                search.tab(), equalTo(Tab.RELEVANT.getName()));
    }

    @Test
    @Title("COMPLEX_MOVE в папку \"Входящие\" должен выставлять таб relevant, если задан пустой dest_tab")
    public void shouldSetRelevantTabWhenComplexMoveToInboxWithEmptyDestTab() throws Exception {
        val context = sendMail();
        val mid = context.firstMid();

        complexMove(folderList.inboxFID(), new MidsSource(mid)).withDestTab("").post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(mid)).get().via(authClient);
        assertThat("У письма должен быть таб [relevant]",
                search.tab(), equalTo(Tab.RELEVANT.getName()));
    }

    @Test
    @UseDataProvider("existingTabs")
    @Title("COMPLEX_MOVE из папки \"Входящие\" в нее же должен просто менять таб")
    public void shouldChangeTabWhenComplexMoveFromInboxToInbox(Tab tab) throws Exception {
        val envelope = makeEnvelopeForTab(authClient, Tab.EMPTY);

        assertThat("Письмо должно быть в папке \"Входящие\"",
                envelope.getFid(), equalTo(folderList.inboxFID()));

        complexMove(envelope.getFid(), new MidsSource(envelope.getMid()))
                .withDestTab(tab.getName()).post(shouldBe(okSync()));

        FilterSearchCommand search = filterSearch(empty().setUid(authClient.account().uid())
                .setMids(envelope.getMid())).get().via(authClient);
        assertThat("У письма должен быть таб [" + tab.getName() + "]",
                search.tab(), equalTo(tab.getName()));
    }

    @Test
    @Ignore("MAILPG-3973")
    @Title("COMPLEX_MOVE заголовка треда")
    @Description("Переносим последнее письмо из треда в корзину, проверяем что тред виден в табе")
    @Issue("MAILPG-3868")
    public void shouldSeeThreadAfterComplexMoveLatestFromInboxToTrash() throws Exception {
        String newestMid = makeThreadForTab(authClient, Tab.RELEVANT).getValue().get(0);

        complexMove(folderList.deletedFID(), new MidsSource(newestMid)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что тред будет виден в табе", authClient,
                hasThreadsInTab(equalTo(1), Tab.RELEVANT.getName()));
    }
}
