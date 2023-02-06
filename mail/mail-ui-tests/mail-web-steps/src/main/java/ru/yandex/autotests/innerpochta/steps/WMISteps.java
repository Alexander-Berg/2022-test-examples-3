package ru.yandex.autotests.innerpochta.steps;

import org.apache.http.impl.client.DefaultHttpClient;
import ru.yandex.autotests.innerpochta.objstruct.base.misc.Account;
import ru.yandex.autotests.innerpochta.util.MailConst;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.SettingsSetup;
import ru.yandex.qatools.allure.annotations.Step;

import java.io.IOException;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.folders.IsThereSubfolderMatcher.hasSubfolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;


public class WMISteps {


    private DefaultHttpClient authHC;


    public void withAcc(Account acc) {
        authHC = auth().with(acc.getLogin(), acc.getPassword()).login().authHC();
    }

    public void withLogin(String login, String pwd) {
        authHC = auth().with(login, pwd).login().authHC();
    }

    @Step("Проверяем, что в папке {1} появилась подпапка {0}")
    public void shouldSeeSubfolder(String child, String parent) {
        assertThat(authHC, hasSubfolder(child, parent));
    }

    @Step("getsDefaultFolderFid")
    public String getsDefaultFolderFid() {
        return api(FolderList.class).setHost(MailConst.MAIL_BASE_URL).post().via(authHC).defaultFID();
    }

    @Step("Получаем fid папки {0}")
    public String getsFidByName(String name){
        return api(FolderList.class).setHost(MailConst.MAIL_BASE_URL).post().via(authHC).getFolderId(name);
    }

    @Step("getsFirstMidInDefaultFolder")
    public String getsFirstMidInDefaultFolder() throws IOException {
        return api(MailBoxList.class).setHost(MailConst.MAIL_BASE_URL).get().via(authHC).getMidOfFirstMessage();
    }

    @Step("getsSetting(settingName:{0})")
    public String getsSetting(String settingName) throws IOException {
        return api(SettingsSetup.class).setHost(MailConst.MAIL_BASE_URL).get().via(authHC)
                .getSettingValue(settingName);
    }

}