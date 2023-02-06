package ru.yandex.autotests.innerpochta.mops;

import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;

import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static org.hamcrest.Matchers.is;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;

@Aqua.Test
@Features(MyFeatures.MOPS)
@Stories(MyStories.FOLDERS)
@Issue("MAILDEV-114")
@Credentials(loginGroup = "MopsFoldersUpdate2Test")
public class FoldersUpdatePop3Test extends MopsBaseTest {
    @Test
    @Issue("MAILDEV-114")
    @Description("Включаем pop3 для папок Inbox и Sent. Проверяем, что метка успешно проставилась.")
    public void enableFolderPop3() {
        updatePop3(folderList.defaultFID(), folderList.sentFID()).post(shouldBe(okEmptyJson()));
        assertThat("Pop3 включен для папки Inbox", updatedFolderList().folderPop3(Symbol.INBOX), is("1"));
        assertThat("Pop3 включен для папки Sent", updatedFolderList().folderPop3(Symbol.SENT), is("1"));
    }

    @Test
    @Issue("MAILDEV-114")
    @Description("Включаем pop3 для папок Inbox и Sent. Затем выключаем его для Inbox" +
            "и проверяем, что метка отсутствует.")
    public void disableFolderPop3() {
        updatePop3(folderList.defaultFID(), folderList.sentFID()).post(shouldBe(okEmptyJson()));
        updatePop3(folderList.sentFID()).post(shouldBe(okEmptyJson()));
        assertThat("Pop3 включен для папки Sent", updatedFolderList().folderPop3(Symbol.INBOX), is("0"));
    }
}
