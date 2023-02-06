package ru.yandex.autotests.testpers;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Document;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mdoby.Flag;
import ru.yandex.autotests.innerpochta.data.B2BMessages;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule;
import ru.yandex.autotests.testpers.misc.OraPgReplaceRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.IGNORE_HIGHLIGHT;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.OUTPUT_AS_CDATA;
import static ru.yandex.autotests.innerpochta.beans.mdoby.Flag.SHOW_CONTENT_META;
import static ru.yandex.autotests.innerpochta.data.B2BMessages.excludeNodes;
import static ru.yandex.autotests.innerpochta.wmi.core.base.DocumentConverter.from;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.DocumentCompareMatcher.equalToDoc;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj.getMsg;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshLocalPortForwardingRule.viaRemoteHost;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.SshRemotePortForwardingRule.localPortForMocking;
import static ru.yandex.autotests.testpers.misc.PgProperties.pgProps;

@Aqua.Test
@Title("Вывод сообщения через api с флагами")
@Description("Сравнение, используя ручку api/message с кукой и указанием различных флагов")
@RunWith(Parameterized.class)
@Features(MyFeatures.WMI)
@Stories({MyStories.B2B, MyStories.MESSAGE_BODY})
@Credentials(loginGroup = ApiMessageB2B.LOGIN_GROUP)
public class ApiMessageB2B extends BaseTest {

    public static final Flag[] FLAGS = {OUTPUT_AS_CDATA, SHOW_CONTENT_META, IGNORE_HIGHLIGHT};
    public static final String LOGIN_GROUP = "ZooNew";

    public static SshLocalPortForwardingRule fwd = viaRemoteHost(props().betaURI())
            .forwardTo(pgProps().getDburi())
            .onLocalPort(localPortForMocking());

    public static OraPgReplaceRule replace = new OraPgReplaceRule(fwd);

    @ClassRule
    public static RuleChain rules = RuleChain.emptyRuleChain().around(fwd).around(replace);

    @Parameterized.Parameters(name = "MID-{0}")
    public static Collection<Object[]> messagesId() throws Exception {
        List<Object[]> msgs = B2BMessages.messagesId(LOGIN_GROUP);
        int limit = 100;
        return msgs.subList(0, msgs.size() > limit ? limit : msgs.size());
    }

    @Parameterized.Parameter
    public String messageId;

    @Test
    public void apiMessageDiffWithFlags() throws Exception {
        // Игнорируем динамическую часть у вдирект-урлов
        MessageObj msg = getMsg(messageId);
        Message msgOper = api(Message.class).params(msg).filters(new VDirectCut());
        MessageObj msgProd = getMsg(replace.resolveOraFromPg(messageId, OraPgReplaceRule.Kind.MID));
        Message msgOperProd = api(Message.class).params(msgProd)
                .filters(new VDirectCut());
        for (Flag flag : FLAGS) {
            msg.setFlags(flag);
            msgProd.setFlags(flag);

            Document actual = msgOper.get().via(hc).toDocument();
            Message expected = msgOperProd.setHost(props().productionHost()).get().via(hc);


            assertThat(String.format("Сообщение с темой: '%s', MID: %s", expected.getSubject(), messageId),
                    actual, excludeNodes(
                            equalToDoc(from(replace.replace(expected.toString())).getConverted())
                            .exclude("//hdr_status")
                    ).urlcomment(messageId)
            );
        }
    }
}
