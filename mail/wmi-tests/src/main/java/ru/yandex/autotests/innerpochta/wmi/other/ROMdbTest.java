package ru.yandex.autotests.innerpochta.wmi.other;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.*;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.FilterSearchObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.YamailStatus;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.YamailStatus.yamailStatus;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.genFile;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 25.11.14
 * Time: 15:20
 */
@Aqua.Test
@Title("RO")
@Description("Проверка отправки и сохранения письма в случае RO")
@Features(MyFeatures.WMI)
@Stories({MyStories.RO, MyFeatures.HOUND})
@Credentials(loginGroup = "ROMdbTest")
@Ignore("MAILDEV-905")
public class ROMdbTest extends BaseTest {

    public static final String NEW_RO = "read_only";
    public static final String NEW_RW = "read_write";

    //кому будем отсылать письма
    @ClassRule
    public static HttpClientManagerRule authClientTo = auth().with("ROToMeTest", "testqa");

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClientTo).all().allfolders();

    private DefaultHttpClient httpClientTo = authClientTo.authHC();

    private static File bigFile;

    @BeforeClass
    public static void getDraftFid() throws IOException {
        bigFile = genFile(1024 * 1024 * 3);
        String dbStatus = api(YamailStatus.class).post().via(authClient.authHC()).getDbStatus();
        String newDbStatus = NEW_RW;
        if (dbStatus.equals("ro")) {
            newDbStatus = NEW_RO;
        }

        yamailStatus(FilterSearchObj.empty().setUid(composeCheck.getUid()))
                .get().via(authClient.authHC())
                .withDebugPrint().shouldBeDbStatus(newDbStatus);

        assumeThat("mdb000 не в RO режиме", newDbStatus, equalTo(NEW_RO));
    }

    @Test
    @Title("Отправка в случае RO api методом")
    @Description("Просто отсылаем письмо через api метод и ждем пока оно дойдет.\n" +
            "Проверяем аттач также")
    public void sendLetterApiROTest() throws Exception {
        MailSendMsgObj msg = msgFactory.getEmptyObj()
                .setTo(authClientTo.acc().getSelfEmail())
                .setSubj("RO + API" + Util.getRandomString())
                .addAtts("binary", bigFile);
        api(MailSend.class).params(msg).post().via(hc).shouldBe().resultOk();
        waitWith.usingHC(httpClientTo).subj(msg.getSubj()).waitDeliver().errorMsg("Письмо не дошло при включенном RO у пользователя (api)");
        AttachUtils.attachInMessageShouldBeSameAs(bigFile, msg.getSubj(), httpClientTo);
    }

    @Test
    @Features(MyFeatures.API_WMI)
    @Title("Отправка в случае RO jsx методом")
    @Description("Просто отсылаем письмо через jsx метод и ждем пока оно дойдет.\n" +
            "Проверяем аттач также")
    public void sendLetterJsxROTest() throws Exception {
        MailSendMsgObj msg = msgFactory
                .getEmptyObj()
                .setTo(authClientTo.acc().getSelfEmail())
                .setSubj("RO + JSX" + Util.getRandomString())
                .addAtts("binary", bigFile);
        jsx(MailSend.class).params(msg).post().via(hc).shouldBe().statusOk();
        waitWith.usingHC(httpClientTo).subj(msg.getSubj()).waitDeliver().errorMsg("Письмо не дошло при включенном RO у пользователя (jsx)");
        AttachUtils.attachInMessageShouldBeSameAs(bigFile, msg.getSubj(), httpClientTo);
    }
}
