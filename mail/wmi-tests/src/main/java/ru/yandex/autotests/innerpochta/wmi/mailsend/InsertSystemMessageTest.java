package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.message.BasicHeader;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.InsertSystemMessage;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.InsertSystemMessageObj.getEmptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.MAIL_SEND_BLOCKED_5004;

/**
 * Покладка себе системных писем
 * + [DARIA-39873]
 */

@Aqua.Test
@Title("Отправка писем. Отправка системного письма")
@Description("Кладем в ящик пользователя специальные системные письма")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Issue("DARIA-39873")
@IgnoreForPg
@Credentials(loginGroup = "Insertsystemmail")
public class InsertSystemMessageTest extends BaseTest {

    public static final String SYSTEM_MID = "10";
    public static final String EXPECTED_SUBJ = "Один логин — много возможностей";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String IP_HOME = "37.140.190.1";

    @Rule
    public CleanMessagesRule clean = with(authClient).inbox().outbox().all();

    @Test
    @Description("Кладем письмо пользователю с несуществующим uid\n" +
            "Ожидаемый результат: 0")
    public void insertingSystemInIncorrectUid() throws Exception {
        String notExistUid = "666666666666";
        jsx(InsertSystemMessage.class).headers(new BasicHeader(X_REAL_IP, IP_HOME))
                .setHost(props().on8079Host())
                .params(getEmptyObj().setMid(SYSTEM_MID).setUid(notExistUid)).post().via(authClient.notAuthHC())
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.UNKNOWN_ERROR_0);

        waitWith.subj(EXPECTED_SUBJ).count(0).waitDeliver();

    }

    @Test
    @Issue("MAILPG-381")
    @Description("Положительный тест: кладем юзеру письмо с мидом 10. Для pg не работает пока!")
    public void insertingSystemMailTest() throws Exception {
        jsx(InsertSystemMessage.class).headers(new BasicHeader(X_REAL_IP, IP_HOME))
                .setHost(props().on8079Host())
                .params(getEmptyObj().setMid(SYSTEM_MID).setUid(composeCheck.getUid())).post().via(authClient.notAuthHC())
                .withDebugPrint();

        waitWith.subj("Один логин — много возможностей").errorMsg("Не положили письмо с мидом 10. " +
                "Для pg не работает [MAILPG-381]").waitDeliver();
    }

    @Test
    @Issue("MAILPG-381")
    @Description("Проверяем ручку insert_system_message не из внутренней сетки\n" +
            "Ожидаемый результат 5004. Для pg не работает!")
    public void insertFromNotHome() throws Exception {
        jsx(InsertSystemMessage.class)
                .params(getEmptyObj().setMid(SYSTEM_MID).setUid(composeCheck.getUid())).post().via(authClient.notAuthHC())
                .shouldBe().errorcode(MAIL_SEND_BLOCKED_5004);

        waitWith.subj(EXPECTED_SUBJ).count(0).waitDeliver();
    }

    @Test
    @Issue("MAILPG-381")
    @Description("Кладем не системное письмо\n" +
            "Ожидаемый результат: 5000")
    public void insertingNotSystemMailTest() throws Exception {
        String mid = "2210000002038578505";
        jsx(InsertSystemMessage.class).headers(new BasicHeader(X_REAL_IP, IP_HOME))
                .setHost(props().on8079Host())
                .params(getEmptyObj().setMid(mid).setUid(composeCheck.getUid())).post().via(authClient.notAuthHC())
                .shouldBe().errorcode(WmiConsts.WmiErrorCodes.NO_SUCH_MESSAGE_5000);
    }


}
