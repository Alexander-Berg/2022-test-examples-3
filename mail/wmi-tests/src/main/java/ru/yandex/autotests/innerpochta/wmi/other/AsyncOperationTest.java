package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailboxOperObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.SettingsLabelCreateObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.plugins.testpers.LogConfigRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLids;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidHasLabelMatcher.hasMsgWithLidsInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.FAKE_SEEN_LBL;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.07.14
 * Time: 16:10
 */
@Title("Тесты на асинхронные операции")
@Description("Одновременно выполняем несколько операций. Смотрим результат")
@Aqua.Test
@Features(MyFeatures.WMI)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "AsyncTest")
public class AsyncOperationTest extends BaseTest {

    @Rule
    public CleanMessagesRule clearMessages = with(authClient).all().inbox().outbox();

    @Rule
    public RuleChain clearLabels = new LogConfigRule()
            .around(DeleteLabelsRule.with(authClient).all());


    @Description("Отправляем несколько писем. Одновременно удаляем и помечаем прочитанными. " +
            "Письма должны быть прочитаны и удалены")
    @Test
    public void deleteAndMarkReadAsyncTest() throws Exception {
        String subj = Util.getRandomString();
        String mid = sendWith.viaProd().subj(subj).waitDeliver().send().getMid();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.invokeAll(asList(asyncRequest(jsx(MailboxOper.class)
                .params(MailboxOperObj.markReadOneMsg(mid))), asyncRequest(jsx(MailboxOper.class)
                .params(MailboxOperObj.deleteOneMsg(mid)))));

        waitWith.subj(subj).inFid(folderList.deletedFID()).waitDeliver();

        assertThat("Ожидалось что письма будут помечены как прочитанные, но письма оказалось без пометки", hc,
                hasMsgWithLidsInFolder(mid, folderList.deletedFID(), FAKE_SEEN_LBL));
    }


    @Description("Отправляем несколько писем. Одновременно помечаем прочитанными и пользовательской меткой. " +
            "Письма должны пометиться прочитанными и на ней должна стоять своя метка")
    @Test
    public void markReadAndLabelAsyncTest() throws Exception {
        String subj = Util.getRandomString();
        String mid = sendWith.subj(subj).waitDeliver().send().getMid();

        String labelName = Util.getRandomString();
        jsx(SettingsLabelCreate.class)
                .params(SettingsLabelCreateObj.newLabel(labelName))
                .post().via(hc);
        String labelId = jsx(Labels.class).post().via(hc).lidByName(labelName);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.invokeAll(asList(asyncRequest(jsx(MailboxOper.class)
                .params(MailboxOperObj.markReadOneMsg(mid))), asyncRequest(api(MessageToLabel.class)
                .params(labelOne(mid, labelId)))));

        assertThat("Ожидалось что письма будут помечены как прочитанные, но письма оказалось без пометки", hc,
                hasMsgWithLids(mid, FAKE_SEEN_LBL));

        assertThat("Ожидалось что письма будут помечены как прочитанные, но письма оказалось без пометки", hc,
                hasMsgWithLids(mid, labelId));
    }

    public static Callable<String> asyncRequest(final Oper oper) {
        return () -> oper.post().via(authClient.authHC()).toString();
    }
}


