package ru.yandex.autotests.innerpochta.wmi.folders;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderRemoveSymbol;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderSetSymbol;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.FolderMatchers.hasSpecialValue;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.Symbols.UNKNOWN;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.fid;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsFolderSymbolObj.symbolOn;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsFolderCreate.newFolder;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001;

/**
 * Created with IntelliJ IDEA.
 * User: lanwen
 * Date: 03.04.13
 * Time: 18:03
 */
@Aqua.Test
@Title("Символ «Unknown», системные символы")
@Features(MyFeatures.WMI)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "Foldersymbol")
public class UnknownAndSystemSymbolTest extends BaseTest {

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all();

    @Rule
    public FolderRule folderRule = new FolderRule();

    private class FolderRule extends TestWatcher {
        private String fid;

        public String fid() {
            return fid;
        }

        @Override
        protected void starting(Description description) {
            fid = newFolder(randomAlphabetic(8)).post().via(authClient.authHC()).updated();
        }
    }

    @Test
    @Title("Должны увидеть ошибку 5001 на установку UNKNOWN символа")
    @Issue("DARIA-22684")
    public void setUnknownSymbolOnNewFolder() throws IOException {
        logger.warn("[DARIA-22684] Символ неизвестен (0)");
        String fid = folderRule.fid();

        jsx(SettingsFolderSetSymbol.class).params(symbolOn(fid, UNKNOWN.value()))
                .post().via(hc).shouldBe().errorcode(INVALID_ARGUMENT_5001);
        assertThat(hc, hasSpecialValue(fid, is(UNKNOWN.special())));
    }

    @Test
    @Title("Должны увидеть ошибку 5001 при снятии символа со всех системных папок")
    @Issue("DARIA-22684")
    public void removeSymbolFromSystemFoldersGetError() throws IOException {
        logger.warn("[DARIA-22684] Попытка снять символ с системной папки");
        afterRemoveSymbolShouldSeeErrorCode(folderList.defaultFID(), INVALID_ARGUMENT_5001);
        afterRemoveSymbolShouldSeeErrorCode(folderList.deletedFID(), INVALID_ARGUMENT_5001);
        afterRemoveSymbolShouldSeeErrorCode(folderList.sentFID(), INVALID_ARGUMENT_5001);
        afterRemoveSymbolShouldSeeErrorCode(folderList.spamFID(), INVALID_ARGUMENT_5001);
        afterRemoveSymbolShouldSeeErrorCode(folderList.draftFID(), INVALID_ARGUMENT_5001);
    }

    public void afterRemoveSymbolShouldSeeErrorCode(String fid, WmiConsts.WmiErrorCodes code) throws IOException {
        jsx(SettingsFolderRemoveSymbol.class).params(fid(fid))
                .post().via(hc).errorcode(code);
    }


}
