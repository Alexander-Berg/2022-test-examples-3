package ru.yandex.autotests.innerpochta;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import nu.xom.ParsingException;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.ClassRule;
import org.junit.Test;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.obj.EmptyObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.AccountInformation;
import ru.yandex.autotests.innerpochta.wmi.core.oper.ComposeCheck;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.rules.acclock.AccLockRule;
import ru.yandex.autotests.innerpochta.wmi.threads.ThreadAndLabel;
import ru.yandex.qatools.hazelcast.HazelcastClient;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;

//PG: @Credentials(login = "killmeplease1111", pwd = "testqa")
@Credentials(loginGroup = "MpfsTest")
public class SomeTest extends BaseTest {

//    @Test
    public void someApiTest() throws IOException {
        String host = "http://main.dcr03s.testpers.tst.yandex.ru";

        api(AccountInformation.class).setHost(host).post().via(hc)
                .withDebugPrint();

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("Hello world from simple api send");

        api(MailSend.class).setHost(host).params(msg).post().via(hc).withDebugPrint();
        waitWith.subj(msg.getSubj()).waitDeliver();
    }

//    @Test
    public void someJsxTest() throws IOException {
        String host = "https://main1.dcr03s.testpers.tst.yandex.ru";
//        String host = "https://wmi6-qa.yandex.ru";

        jsx(AccountInformation.class).setHost(host).post().via(hc).withDebugPrint();

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg()
                .setSend("Hello world from simple api send");

        jsx(MailSend.class).setHost(host).params(msg).post().via(hc).withDebugPrint();
        waitWith.subj(msg.getSubj()).waitDeliver();
    }
}