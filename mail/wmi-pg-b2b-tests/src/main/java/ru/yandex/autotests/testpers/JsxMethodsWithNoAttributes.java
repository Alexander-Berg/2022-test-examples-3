package ru.yandex.autotests.testpers;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.JsxTestingB2BData;
import ru.yandex.autotests.innerpochta.util.rules.LogIPRule;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.Obj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Oper;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;
import ru.yandex.autotests.testpers.misc.OraPgReplaceRule;
import ru.yandex.autotests.testpers.misc.filters.SortFilter;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule.viaRemoteHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshRemotePortForwardingRule.localPortForMocking;
import static ru.yandex.autotests.testpers.misc.PgProperties.pgProps;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 20.04.15
 * Time: 18:00
 */
@Aqua.Test
@Title("[B2B] Вызов JSX методов без атрибутов")
@Description("Сравнение с продакшеном")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories(MyStories.B2B)
@Credentials(loginGroup = ru.yandex.autotests.innerpochta.wmi.b2b.JsxMethodsWithNoAttributes.LOGIN_GROUP)
public class JsxMethodsWithNoAttributes extends BaseTest {


    public static SshLocalPortForwardingRule fwd = viaRemoteHost(props().betaURI())
            .forwardTo(pgProps().getDburi())
            .onLocalPort(localPortForMocking());

    public static OraPgReplaceRule replace = new OraPgReplaceRule(fwd);

    @ClassRule
    public static RuleChain rules = RuleChain.emptyRuleChain().around(fwd).around(replace);


    @Parameterized.Parameters(name = "{2}")
    public static Collection<Object[]> data() {
        return JsxTestingB2BData.jsxHandles();
    }

    private Oper oper;

    public JsxMethodsWithNoAttributes(Oper oper, Obj obj, String opername) {
        this.oper = oper;
        this.oper.params(obj);
    }

    @Rule
    public LogIPRule logIpRule = new LogIPRule();

    @Test
    @Description("Сравниваем с продакшеном выдачу различных jsx ручек БЕЗ атрибутов.")
    public void compareMethodsResponse() throws Exception {
        logger.warn("Проверяем метод - " + oper.getClass().getSimpleName());
        Oper actual = oper.post().via(hc);

        oper.setHost(props().b2bUri().toString());
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
                        .exclude("//yamail/account_information/birthday") //прилетает из бб-рц
                        .urlcomment(oper.getClass().getSimpleName()));
    }
}