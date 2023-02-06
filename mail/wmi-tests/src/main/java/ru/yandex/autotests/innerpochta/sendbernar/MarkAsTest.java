package ru.yandex.autotests.innerpochta.sendbernar;


import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.*;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.folderlist.Symbol;
import ru.yandex.autotests.innerpochta.beans.labels.LabelSymbol;
import ru.yandex.autotests.innerpochta.beans.sendbernar.SendMessageResponse;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.IsNot.not;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.labels.MidsWithLabelMatcher.hasMsgWithLidInFolder;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okFid;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.sendbernar.SendbernarResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;
import static ru.yandex.autotests.innerpochta.wmi.core.api.WmiApis.*;


@Aqua.Test
@Title("Пометка метками в send_message")
@Description("Помечаем форварженные и отвеченные письма метками")
@Features(MyFeatures.SENDBERNAR)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "MailSendMops")
@RunWith(DataProviderRunner.class)
public class MarkAsTest extends BaseSendbernarClass {
    String messageId;
    String mid;
    String anotherSubj;

    @ClassRule
    public static HttpClientManagerRule another = auth().with("MailSendReply");

    @Rule
    public CleanMessagesMopsRule cleanAnother = new CleanMessagesMopsRule(another).inbox().outbox().draft().deleted();

    @DataProvider
    public static Object[][] cases() {
        return new Object[][]{
                {"replied", "answered"},
                {"forwarded", "forwarded"},
        };
    }

    @DataProvider
    public static Object[][] excludedFolders() {
        return new Object[][]{
                {Symbol.HIDDEN_TRASH},
                {Symbol.TRASH},
                {Symbol.DRAFT},
                {Symbol.TEMPLATE},
                {Symbol.OUTBOX},
        };
    }

    @Before
    public void prepareData() {
        messageId = apiSendbernar(another.account().userTicket()).sendMessage()
                .withUid(another.account().uid())
                .withCaller(caller)
                .withTo(authClient.acc().getSelfEmail())
                .withSubj(subj)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class)
                .getMessageId();

        Mops.createHiddenTrash(authClient).post(shouldBe(okFid()));
        mid = waitWith.subj(subj).inbox().waitDeliver().getMid();
        anotherSubj = getRandomString();
    }

    @Test
    @Description("Помечаем письма метками")
    @UseDataProvider("cases")
    public void replyAndCheckMessageToBeMarked(String markAs, String labelName) {
        sendMessage()
                .withTo(another.acc().getSelfEmail())
                .withSubj(anotherSubj)
                .withInreplyto(messageId)
                .withMarkAs(markAs)
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class);

        waitWith.subj(anotherSubj).sent().waitDeliver();


        String lid = lidByName(labelName);


        assertThat("Письмо не помечено как " + markAs,
                authClient,
                hasMsgWithLidInFolder(mid, folderList.defaultFID(), lid));
    }

    @Test
    @Description("Не помечаем письма из определённых папок")
    @UseDataProvider("excludedFolders")
    public void shouldNotMarkMessagesFromThisFolders(Symbol folder) throws Exception {
        saveTemplate()
                .withTo(authClient.acc().getSelfEmail())
                .post(shouldBe(ok200()));

        String fid = folderList.fidBySymbol(folder);

        Mops.complexMove(authClient, fid, new MidsSource(mid))
                .post(shouldBe(okSync()));

        sendMessage()
                .withTo(another.acc().getSelfEmail())
                .withSubj(anotherSubj)
                .withInreplyto(messageId)
                .withMarkAs("replied")
                .post(shouldBe(ok200()))
                .as(SendMessageResponse.class);

        waitWith.subj(anotherSubj).sent().waitDeliver();

        assertThat("Письмо помечено, хотя не должно",
                authClient,
                not(hasMsgWithLidInFolder(mid, fid, lidByName(LabelSymbol.ANSWERED.toString()))));
    }
}
