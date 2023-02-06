package ru.yandex.autotests.testpers;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.oper.EmptyWmiOper;
import ru.yandex.autotests.innerpochta.wmi.core.oper.FolderList;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailBoxList;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;
import ru.yandex.autotests.testpers.misc.OraPgReplaceRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.any;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.StringDiffer.notDiffersWith;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule.viaRemoteHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshRemotePortForwardingRule.localPortForMocking;
import static ru.yandex.autotests.testpers.misc.PgProperties.pgProps;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 28.07.14
 * Time: 20:06
 * <p/>
 * [AUTOTESTPERS-140]
 * [DARIA-34071]
 */
@Title("Тесты на Touch почту")
@Description("Делаем запросы хэндлеров, сравниваем с продакшеном")
@Aqua.Test
@Features(MyFeatures.WMI)
@Stories({MyStories.B2B, MyStories.TOUCH})
@Issue("AUTOTESTPERS-140")
@Credentials(loginGroup = "Touchb2b")
public class ApiTouchHandlersFastB2BTest extends BaseTest {

    private static String mid;

    public static final String JSX_PATH = "/touch/jsx/models.jsx";
    public static final String HANDLERS = "_m";
    public static final String PREFIX = JSX_PATH + "?" + HANDLERS + "=";

    public static SshLocalPortForwardingRule fwd = viaRemoteHost(props().betaURI())
            .forwardTo(pgProps().getDburi())
            .onLocalPort(localPortForMocking());

    public static OraPgReplaceRule replace = new OraPgReplaceRule(fwd);

    @ClassRule
    public static RuleChain rules = RuleChain.emptyRuleChain().around(fwd).around(replace);

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @BeforeClass
    public static void prepare() throws IOException {
        mid = api(MailBoxList.class).post().via(authClient.authHC()).getMidOfFirstMessage();

    }

    @Description("Пример запроса:\n" +
            "https://wmi6-qa.yandex.ru" +
            "/touch/jsx/models.jsx?_m=message-body&_model.0=message-body&mid.0=2400000003321165393" +
            "&spam_fid.0=2400000310010878190&_ckey=OqI4klzwtK995IurFK/OhKUdyeLEPHE7BHGO16qR6jo=")
    @Test
    public void testMessageBody() throws IOException {
        String ckey = api(ComposeCheck.class).post().via(hc).getCKey();

        Obj obj = empty().set("_model.0", "message-body")
                .set("mid.0", mid)
                .set("spam_fid.0", folderList.spamFID())
                .set("_ckey", ckey);

        EmptyWmiOper respBeta = any(PREFIX, "message_body", "Тело письма BETTA").params(obj).post().via(hc);
        EmptyWmiOper respProd = any(PREFIX, "message_body", "Тело письма PROD").params(obj)
                .setHost(props().productionHost()).post().via(hc);


        assertThat(respBeta.toString(), notDiffersWith(replace.replace(respProd.toString()))
                .excludeDefaults().exclude("\"versions\":.*"));
    }

    @Description("Пример запроса:\n" +
            "https://wmi6-qa.yandex.ru/touch/jsx/models.jsx?" +
            "_m=account-information,folders,labels,settings,user-activity" +
            "&_model.0=account-information&_model.1=folders&_model.2=labels&_model.3=settings&_model.4=user-activity")
    @Test
    public void testSettingsAndAccountInformation() throws IOException {
        Obj obj = empty()
                .set("_model.0", "account-information")
                .set("_model.1", "folders")
                .set("_model.2", "labels")
                .set("_model.3", "settings")
                .set("_model.4", "user-activity");

        EmptyWmiOper respProd = any(PREFIX, "account-information,folders,labels,settings,user-activity",
                "Настройки,папки,метки,информация о юзере: BETTA").params(obj)
                .post().via(hc);

        EmptyWmiOper respBetta = any(PREFIX, "account-information,folders,labels,settings,user-activity",
                "Настройки,папки,метки,информация о юзере: PROD").params(obj).setHost(props().productionHost())
                .post().via(hc);


        assertThat(respBetta.toString(), notDiffersWith(replace.replace(respProd.toString()))
                .excludeDefaults().exclude("\"versions\":.*"));
    }

    @Description("Пример запроса:\n" +
            "https://wmi6-qa.yandex.ru/touch/jsx/models.jsx?" +
            "_m=messages-portion" +
            "&_model.0=messages-portion&_fid.0=2400000310010878190&_first.0=0" +
            "&_threaded.0=yes&_ckey=OqI4klzwtK995IurFK/OhKUdyeLEPHE7BHGO16qR6jo=")
    @Test
    public void testMessagesHandler() throws IOException {
        String ckey = api(ComposeCheck.class).post().via(hc).getCKey();

        Obj obj = empty()
                .set("_model.0", "messages-portion")
                .set("_fid.0", folderList.spamFID())
                .set("_first.0", "0")
                .set("_threaded.0", "yes")
                .set("_ckey", ckey);

        EmptyWmiOper respProd = any(PREFIX, "messages-portion", "Лента сообщений: PROD")
                .params(obj).post().via(hc);

        EmptyWmiOper respBetta = any(PREFIX, "messages-portion", "Лента сообщений: BETTA").params(obj)
                .setHost(props().productionHost()).post().via(hc);


        assertThat(respBetta.toString(), notDiffersWith(replace.replace(respProd.toString()))
                .excludeDefaults().exclude("\"versions\":.*"));
    }

}
