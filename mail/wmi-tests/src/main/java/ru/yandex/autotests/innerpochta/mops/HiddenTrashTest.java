package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.hasMsgIn;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;


@Aqua.Test
@Title("[MOPS] Работа с hidden_trash")
@Description("Переносим письма в Скрытую Корзину и обратно")
@Features(MyFeatures.MOPS)
@Credentials(loginGroup = "MopsHiddenTrash")
public class HiddenTrashTest extends MopsBaseTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Before
    public void prepare() throws Exception {
        createHiddenTrash().post(shouldBe(okFid()));
    }

    @Test
    @Title("complex_move и Скрытая Корзина")
    @Description("Проверяем работу /complex_move при включенной Скрытой Корзине")
    public void complexMoveAndHiddenTrash() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        complexMove(folderList.hiddenTrashFID(), new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Скрытой Корзине\"",
                authClient, hasMsgIn(subject, folderList.hiddenTrashFID()));

        complexMove(folderList.defaultFID(), new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется в \"Входящие\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));
        assertThat("Ожидалось, что сообщение перенесется из \"Скрытой Корзины\"",
                authClient, not(hasMsgIn(subject, folderList.hiddenTrashFID())));
    }

    @Test
    @Title("remove и Скрытая Корзина")
    @Description("Проверяем работу /remove при включенной Скрытой Корзине")
    public void removeAndHiddenTrash() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Корзину\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));
        assertThat("Ожидалось, что сообщение не перенесется в \"Скрытую Корзину\"",
                authClient, not(hasMsgIn(subject, folderList.hiddenTrashFID())));

        remove(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не перенесется в \"Входящие\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение перенесется из \"Корзины\"",
                authClient, not(hasMsgIn(subject, folderList.deletedFID())));
        assertThat("Ожидалось, что сообщение перенесется в \"Скрытую Корзину\"",
                authClient, hasMsgIn(subject, folderList.hiddenTrashFID()));

        remove(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не перенесется из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не перенесется в \"Корзину\"",
                authClient, not(hasMsgIn(subject, folderList.deletedFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Скрытой Корзины\"",
                authClient, not(hasMsgIn(subject, folderList.hiddenTrashFID())));
    }

    @Test
    @Title("purge и Скрытая Корзина")
    @Description("Проверяем работу /purge при включенной Скрытой Корзине")
    public void purgeAndHiddenTrash() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение появится в \"Скрытой Корзине\"",
                authClient, hasMsgIn(subject, folderList.hiddenTrashFID()));

        purge(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Скрытой Корзины\"",
                authClient, not(hasMsgIn(subject, folderList.hiddenTrashFID())));

    }

    @Test
    @Title("Очистка Скрытая Корзина")
    @Description("Проверяем работу /purge_hidden_trash при включенной Скрытой Корзине")
    public void purgeHiddenTrashTest() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение появится в \"Скрытой Корзине\"",
                authClient, hasMsgIn(subject, folderList.hiddenTrashFID()));

        purgeHiddenTrash().post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Скрытой Корзины\"",
                authClient, not(hasMsgIn(subject, folderList.hiddenTrashFID())));

    }

}
