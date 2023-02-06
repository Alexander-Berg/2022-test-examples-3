package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailboxOper;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidsInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMsgsMatcher.*;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 22.10.13
 * Time: 18:47
 */
@Aqua.Test
@Title("Проверка пометки спамом/неспамом")
@Description("Тесты связанные с пометкой спамом/неспамом")
@Features(MyFeatures.WMI)
@Stories(MyStories.SPAM)
@Credentials(loginGroup = "Markspam")
public class MarkAsSpamTest extends BaseTest {

    private SendUtils sendMail() throws Exception {
        return new SendUtils(authClient).folderList(folderList).composeCheck(composeCheck)
                .waitDeliver().viaProd().send();
    }

    private SendUtils sendMail(String subject) throws Exception {
        return new SendUtils(authClient).folderList(folderList).composeCheck(composeCheck)
                .subj(subject).waitDeliver().viaProd().send();
    }

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).all().allfolders();

    /**
     * Отправляем себе письмо,
     * помечаем его спамом, проверяем,
     * что оно пометилось прочитанным
     * <p/>
     * throws Exception
     */
    @Test
    @Title("Должны пометить прочитанным после пометки спамом")
    @Issue("DARIA-28405")
    public void shouldBeIsReadWhenMarkAsSpam() throws Exception {
        String mid = sendMail().getMid();
        jsx(MailboxOper.class)
                .params(MailboxOperObj.empty().addIds(mid).setOper(MailboxOperObj.OPER_TOSPAM))
                .post().via(hc);

        assertThat("[DARIA-28405] Ожидалось что письмо будет помечено как прочитанное," +
                        " но письмо оказалось без пометки",
                hc, hasMsgWithLidsInFolder(mid, folderList.spamFID(), FAKE_SEEN_LBL));
    }

    @Test
    @Title("Должны перенести все письма в спам по mid и tid")
    @Issue("MAILDEV-814")
    public void shouldMarkMessageAsSpamByMidAndTid() throws Exception {
        final String tidSubj = Util.getRandomString();
        final String tid = sendMail(tidSubj).getTid();

        final String midSubj = Util.getRandomString();
        final String mid = sendMail(midSubj).getMid();

        jsx(MailboxOper.class)
                .params(MailboxOperObj.empty().addIds(mid).addTids(tid).setOper(MailboxOperObj.OPER_TOSPAM))
                .post().via(hc);

        assertThat("Ожидалось, что письмо будет перенесено из \"Входящих\" по tid", hc,
                not(hasMsg(tidSubj)));
        assertThat("Ожидалось, что письмо будет перенесено из \"Отправленных\" по tid", hc,
                not(hasMsgIn(tidSubj, folderList.sentFID())));
        assertThat("Ожидалось, что письмо будет перенесено из \"Входящих\" по mid", hc,
                not(hasMsg(midSubj)));

        assertThat("Ожидалось, что письма будут перенесены в \"Спам\" по tid", hc,
                hasMsgsIn(tidSubj, 2, folderList.spamFID()));
        assertThat("Ожидалось, что письмо будет перенесено в \"Спам\" по mid", hc,
                hasMsgIn(midSubj, folderList.spamFID()));
    }
}
