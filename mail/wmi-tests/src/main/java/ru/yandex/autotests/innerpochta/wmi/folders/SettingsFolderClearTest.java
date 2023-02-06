package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderClear;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.WaitForMatcherDecorator.withWaitFor;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgsWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.CountMsgsMatcher.hasNoMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.CountMsgsMatcher.hasNoMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgs;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.hasMsgsIn;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.clearFid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderClearObj.purgeFid;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.08.14
 * Time: 20:24
 * [DARIA-39798]
 */
@Aqua.Test
@Title("Проверяем ручку: settings_folder_clear")
@Description("Очистка папки \"Входящие\" пустой, письмами по теме, по отправителю и т.д.\n" +
        "Проверяем мягкую очистку и purge")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Issue("DARIA-39798")
@Credentials(loginGroup = "SettingsFolderClearTest")
public class SettingsFolderClearTest extends BaseTest {

    public static final String NOT_EXIST_FID = "12345678";
    public static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient)
            .all().allfolders();

    @Rule
    public DeleteFoldersRule clear = DeleteFoldersRule.with(authClient).all();

    @Test
    @Description("Дергаем settings_folder_clear с несуществующей папкой\n" +
            "Ожидаемый результат: ok")
    public void clearNotExistFolder() throws IOException {
        jsx(SettingsFolderClear.class)
                .params(purgeFid(NOT_EXIST_FID)).post().via(hc).shouldBe().updated(NOT_EXIST_FID);
        jsx(SettingsFolderClear.class)
                .params(clearFid(NOT_EXIST_FID)).post().via(hc).shouldBe().updated("ok");
    }

    @Test
    @Description("Очищаем пустую папку\n" +
            "Ожидаемый результат: все окей")
    public void clearEmptyFolder() throws Exception {
        jsx(SettingsFolderClear.class)
                .params(clearFid(folderList.defaultFID())).post().via(hc).shouldBe().updated("ok");

        jsx(SettingsFolderClear.class)
                .params(purgeFid(folderList.defaultFID())).post().via(hc).shouldBe().updated(folderList.defaultFID());

        assertThat("В пустой пользовательской папке появились сообщения", hc, withWaitFor(hasNoMsgs()));
    }

    @Test
    @Description("Очищаем не пустую папку с методом clear\n" +
            "Ожидаемый результат: письма попали в \"Удаленные\"")
    public void clearNotEmptyFolder() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        String subj = sendUtils.getSubj();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(clearFid(folderList.defaultFID())).post().via(hc).updated("ok");

        assertThat("В пользовательской папке остались сообщения", hc, withWaitFor(hasNoMsgs()));
        assertThat("Письма НЕ перенеслись в папку \"Удаленные\"", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS, folderList.deletedFID())));

    }

    @Test
    @Description("Очищаем не пустую папку с параметром purge\n" +
            "Ожидаемый результат: письма не попали в \"Удаленные\"")
    public void clearPurgeNotEmptyFolder() throws Exception {
        sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //само удаление
        jsx(SettingsFolderClear.class)
                .params(purgeFid(folderList.defaultFID())).post().via(hc);

        assertThat("В пользовательской папке остались сообщения", hc, withWaitFor(hasNoMsgs()));
        assertThat("Письма перенеслись в папку \"Удаленные\"", hc, withWaitFor(hasNoMsgsIn(folderList.deletedName())));
    }

    @Test
    @Description("Очищаем не пустую папку с методом clear\n" +
            "Ожидаемый результат: письма попали в \"Удаленные\"")
    public void clearNotEmptyFolderBySubject() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        String subj = sendUtils.getSubj();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.clearBySubj(folderList.defaultFID(), subj)).post().via(hc).updated("ok");

        assertThat("В пользовательской папке остались сообщения", hc, withWaitFor(hasNoMsgs()));
        assertThat("Письма НЕ перенеслись в папку \"Удаленные\"", hc, withWaitFor(hasMsgsIn(subj, COUNT_OF_LETTERS,
                folderList.deletedFID())));
    }

    @Test
    @Description("Очищаем не пустую папку с методом clear\n" +
            "Ожидаемый результат: письма попали в \"Удаленные\"")
    public void clearNotEmptyFolderByFrom() throws Exception {
        sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.getObjToClearByFrom(folderList.defaultFID(), authClient.acc().getLogin().toLowerCase()))
                .post().via(hc).updated("ok");

        assertThat("В пользовательской папке остались сообщения", hc, withWaitFor(hasNoMsgs()));
        assertThat("Письма перенеслись в папку \"Удаленные\"", hc, withWaitFor(hasNoMsgsIn(folderList.deletedFID())));
    }

    @Test
    @Description("Очищаем не пустую папку с методом clear и параметром old = 100\n" +
            "Ожидаемый результат: письма должны остаться на месте")
    public void clearNotEmptyFolderByBigOld() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.getObjToClearByOld(folderList.defaultFID(), 100))
                .post().via(hc).updated("ok");

        assertThat("В пользовательской папке не остались сообщения", hc, withWaitFor(hasMsgs(sendUtils.getSubj(), COUNT_OF_LETTERS)));
    }

    @Test
    @Description("Очищаем не пустую папку с методом clear и параметром old = 0\n" +
            "Ожидаемый результат: все письма попали в \"Удаленные\"")
    public void clearNotEmptyFolderByOld0() throws Exception {
        sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.getObjToClearByOld(folderList.defaultFID(), 0))
                .post().via(hc).updated("ok");

        assertThat("В пользовательской папке остались сообщения", hc, withWaitFor(hasNoMsgs()));
        assertThat("Письма перенеслись в папку \"Удаленные\"", hc, withWaitFor(hasNoMsgsIn(folderList.deletedFID())));
    }


    @Test
    @Issue("DARIA-45884")
    @Title("Фильтр age с параметром 60")
    @Description("Очищаем не пустую папку с методом clear и параметром old = 60\n" +
            "Ожидаемый результат: все письма попали в \"Удаленные\", должны передать запрос в mops с параметром 60")
    public void clearNotEmptyFolderByOld60() throws Exception {
        String subj = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send().getSubj();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.getObjToClearByOld(folderList.defaultFID(), 60))
                .post().via(hc).updated("ok");

        assertThat("В пользовательской папке не остались сообщения", hc, withWaitFor(hasMsgs(subj, COUNT_OF_LETTERS)));
    }

    @Test
    @Description("Проверка что не ломается при комбинации нескольких фильтров")
    public void clearNotEmptyFolderWithCombination() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //мягкое удаление
        jsx(SettingsFolderClear.class)
                .params(SettingsFolderClearObj.getEmptyObj().setFid(folderList.defaultFID()).setFromF(authClient.acc().getLogin().toLowerCase())
                        .setOldF(0).setSubjF(sendUtils.getSubj()).setMethod(SettingsFolderClearObj.METHOD_MARK_READ))
                .post().via(hc).updated("ok");

        assertThat("Письмо осталось непрочитанно", hc,
                hasMsgsWithLid(sendUtils.getMids(), WmiConsts.FAKE_SEEN_LBL));
    }

    @Test
    @Description("Проверяем, что при вызове функции settings_folder_clear с методом read.\n" +
            "Письма помечаются прочитанными")
    public void clearWithMethodMarkRead() throws Exception {
        SendUtils sendUtils = sendWith.viaProd().waitDeliver().count(COUNT_OF_LETTERS).send();
        //пометка прочитанным

        jsx(SettingsFolderClear.class).params(SettingsFolderClearObj.markRead(folderList.defaultFID()))
                .post().via(hc);

        assertThat("Письмо осталось непрочитанно", hc,
                hasMsgsWithLid(sendUtils.getMids(), WmiConsts.FAKE_SEEN_LBL));
    }

}
