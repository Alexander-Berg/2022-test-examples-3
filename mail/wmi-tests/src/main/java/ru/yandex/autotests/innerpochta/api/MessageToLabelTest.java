package ru.yandex.autotests.innerpochta.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.*;
import ru.yandex.autotests.innerpochta.wmi.core.oper.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.DeleteLabelsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static com.google.common.base.Joiner.on;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelMessages;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.MessageToLabelUnlabelObj.labelOne;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.MessageToLabel.messageToLabel;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;

/**
 * Отправка письма через мобильный клиент и пометка его новосозданной меткой
 */
@Aqua.Test
@Title("[API] Метка письма")
@Description("Отправка письма через мобильный клиент и пометка его новосозданной меткой")
@Features(MyFeatures.API_WMI)
@Stories(MyStories.LABELS)
@Credentials(loginGroup = MessageToLabelTest.GROUP_NAME)
public class MessageToLabelTest extends BaseTest {

    public static final String GROUP_NAME = "ApiFunkTest";

    @Rule
    public DeleteLabelsRule deleteLabels = DeleteLabelsRule.with(authClient).all();

    @Rule
    public CleanMessagesRule clean = with(authClient).outbox().inbox().draft().all();

    private String subject;
    private List<String> mids;
    private String labelId;

    @Before
    public void prepare() throws Exception {
        subject = Util.getRandomString();

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setSend("Hello World!").setSubj(subject);
        api(MailSend.class).params(msg).post().via(hc).then().post().via(hc);

        mids = waitWith.subj(msg.getSubj()).count(2).waitDeliver().getMids();
        String labelName = Util.getRandomString();

        jsx(SettingsLabelCreate.class).params(SettingsLabelCreateObj.newLabel(labelName)).post().via(hc);
        labelId = api(Labels.class).post().via(hc).lidByName(labelName);
    }

    @Test
    @Description("Отправка письма самому себе,\n" +
            "ожидание получения письма,\n" +
            "- Проверка что письмо дошло и с таким сабж только 2 письма\n" +
            "получения mid,\n" +
            "создание метки, назначение метки полученному письму\n" +
            "- Проверка что письма получили метку\n" +
            "Удаление метки и письма")
    public void labelMessagesInNotOneIDS() throws Exception {
        messageToLabel(labelMessages(mids, labelId)).post().via(hc).errorcodeShouldBeEmpty();
        assertThat("Письмо не пометилось меткой",
                api(MailBoxList.class).post().via(hc).isSomeMessagesLabeled(mids, asList(labelId)), is(true));
    }

    @Test
    @Description("Аналогичный тест, с одним отличием - миды перечисляются через запятую")
    public void labelMessagesInOneIDS() throws Exception {
        messageToLabel(labelOne(on(",").join(mids), labelId)).post().via(hc).errorcodeShouldBeEmpty();
        assertThat("Письмо не пометилось меткой",
                api(MailBoxList.class).post().via(hc).isSomeMessagesLabeled(mids, asList(labelId)), is(true));
    }
}
