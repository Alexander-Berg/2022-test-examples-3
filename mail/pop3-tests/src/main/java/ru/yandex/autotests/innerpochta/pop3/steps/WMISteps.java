package ru.yandex.autotests.innerpochta.pop3.steps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.impl.client.DefaultHttpClient;

import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Step;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLid;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.should;
import static ru.yandex.qatools.htmlelements.matchers.MatcherDecorators.timeoutHasExpired;

public class WMISteps {

    private DefaultHttpClient hc;

    private List<String> mids;
    private List<String> lids = new ArrayList<>();

    public WMISteps withLogin(String login, String pwd) {
        hc = auth().with(login, pwd).login().authHC();
        return this;
    }

    @Step("Получаем mid'ы писем с темой {0}")
    public WMISteps getMids(String subject) throws IOException {
        mids = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject);
        return this;
    }

    @Step("Должны видеть письмо прочитанным")
    public void shouldSeeMessageAsRead(String subject, String folder) throws IOException {
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folder);
        mids = jsx(MailBoxList.class).params(MailBoxListObj.inFid(fid))
                .post().via(hc).getMidsOfMessagesWithSubject(subject);
        lids.add(WmiConsts.FAKE_SEEN_LBL);
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", hc,
                should(hasMsgWithLidInFolder(mids.get(0), fid, WmiConsts.FAKE_SEEN_LBL))
                        .whileWaitingUntil(timeoutHasExpired(SECONDS.toMillis(20))));
    }

    @Step("Должны видеть письмо прочитанным")
    public void shouldNotSeeMessageAsRead(String subject, String folder) throws IOException {
        String fid = jsx(FolderList.class).post().via(hc).getFolderId(folder);
        mids = jsx(MailBoxList.class).params(MailBoxListObj.inFid(fid))
                .post().via(hc).getMidsOfMessagesWithSubject(subject);
        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", hc,
                not(hasMsgWithLidInFolder(mids.get(0), fid, WmiConsts.FAKE_SEEN_LBL)));
    }

    @Step("Должны видеть одно письмо прочитанным")
    public void shouldSeeOnlyOneMessageAsRead(String subject1, String subject2) throws IOException {
        List<String> mids1 = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject1);
        List<String> mids2 = jsx(MailBoxList.class)
                .post().via(hc).getMidsOfMessagesWithSubject(subject2);

        assertThat("Ожидалось что письмо будет помечено как прочитанное, но письмо оказалось без пометки", hc,
                anyOf(
                        both(hasMsgWithLid(mids1.get(0), WmiConsts.FAKE_SEEN_LBL))
                                .and(not(hasMsgWithLid(mids2.get(0), WmiConsts.FAKE_SEEN_LBL))),
                        both(hasMsgWithLid(mids2.get(0), WmiConsts.FAKE_SEEN_LBL))
                                .and(not(hasMsgWithLid(mids1.get(0), WmiConsts.FAKE_SEEN_LBL)))
                ));
    }

    public List<String> mids() {
        return mids;
    }

}
