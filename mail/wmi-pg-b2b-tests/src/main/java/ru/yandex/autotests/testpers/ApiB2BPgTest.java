package ru.yandex.autotests.testpers;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.ApiTestingB2BData;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailBoxListObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;
import ru.yandex.autotests.testpers.misc.OraPgReplaceRule;
import ru.yandex.autotests.testpers.misc.filters.SortFilter;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import static javax.ws.rs.client.Entity.form;
import static org.glassfish.jersey.client.authentication.HttpAuthenticationFeature.basic;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.testpers.ssh.SSHAuthRule.sshOn;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule.viaRemoteHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshRemotePortForwardingRule.localPortForMocking;
import static ru.yandex.autotests.testpers.misc.PgProperties.pgProps;

/**
 * User: lanwen
 * Date: 28.04.15
 * Time: 16:48
 */
@Aqua.Test
@Title("Вызов API методов c атрибутами или без")
@Description("Сравнение выводов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = "ZooNew")
public class ApiB2BPgTest extends BaseTest {

    public static SshLocalPortForwardingRule fwd = viaRemoteHost(props().betaURI())
            .forwardTo(pgProps().getDburi())
            .onLocalPort(localPortForMocking());

    public static OraPgReplaceRule replace = new OraPgReplaceRule(fwd);

    @ClassRule
    public static RuleChain rules = RuleChain.emptyRuleChain().around(fwd).around(replace);

    public static HttpClientManagerRule authClient2 = auth();
    private static Client client = ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));
    
    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    //{2} имя ручки, которую проверяем
    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return ApiTestingB2BData.apiHandles();
    }


    @Parameterized.Parameter
    public Oper oper;

    @Parameterized.Parameter(1)
    public Obj obj;

    @Parameterized.Parameter(2)
    public String comment;

    @Before
    public void setUp() throws Exception {
        oper.params(obj);
    }

    @BeforeClass
    public static void transfer() throws InterruptedException {
//        RegUser newUser = UserCreate.createNewUser();
        
        String login = "robbitter-0458337744";
        String pwd = "simple123456";
//
//        String s = client.target(UriBuilder.fromPath("/job/{job}/buildWithParameters")
//                .scheme("http").host(LaunchMigration.JENKINS)
//                .build(LaunchMigration.TO_PG))
//                .register(basic(LaunchMigration.LOGIN, LaunchMigration.TOKEN))
//                .request().post(form(
//                        new Form("LOGINS", authClient.acc().getLogin() + ":" + login)
//                                .param("COPY_SETTINGS", "false")
//                )).readEntity(String.class);
//        System.out.println(s);
        
        authClient2.with(login, pwd).login();
        
//        Thread.sleep(TimeUnit.SECONDS.toMillis(30));
    }
    
    @Test
    public void testIsMethodsEquals() throws SQLException, IOException {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper actual = oper.params(MailBoxListObj.empty().setSortType(MailBoxListObj.SORT_DATE))
                .setHost(props().productionHost()).post().via(authClient2.authHC());
        

        Oper expectedOp = oper.post().via(hc);
        Document expected = from(replace.replace(expectedOp.toString())).getConverted();

        assertThat(actual.toDocument(),
                equalToDoc(expected)
                        .filterWithCondition(new SortFilter<>(),
                                oper.cmd().contains("labels")
                                        || oper.cmd().contains("mailbox_list")

                        ) //MAILPG-192
                        .exclude("@status") // https://st/MAILPG-191
                        .exclude("@st_id") // https://st/MAILPG-190
                        .urlcomment(oper.getClass().getSimpleName()));
    }
}
