package ru.yandex.autotests.innerpochta.recognizer;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.data.Langs;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageHeader;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 23.09.14
 * Time: 17:26
 * https://jira.yandex-team.ru/browse/DARIA-38188, https://st.yandex-team.ru/MAILDEV-413
 * Ахтунг!
 * Менять тестового пользователя нельзя.
 * Удалять письма из ящика тоже.
 */
@Aqua.Test
@Title("Рекогнайзер. Новые тесты. Определение языка с помощью AnalyzeText.")
@Description("Проверка определения языка для различных писем")
@RunWith(Parameterized.class)
@Credentials(loginGroup = "ZooNew")
@Features(MyFeatures.RECOGNIZER)
@Stories(MyStories.B2B)
@Ignore("MAILDEV-1348")
public class NewLangRecognizerTest extends BaseTest {

    private String messageId;
    private Langs expectedLang;

    @Parameterized.Parameters(name = "mid-{0} expected_lang-{1}")
    public static Collection<Object[]> messagesId() throws Exception {
        return Langs.data();
    }

    public NewLangRecognizerTest(String messageId, Langs expectedLang) {
        this.messageId = messageId;
        this.expectedLang = expectedLang;
    }

    @Test
    @Issue("DARIA-32952")
    @Title("Проверка 'lang' на письмах из зоопарка")
    @Description("Проверяем выдачу 'lang' на письмах из зоопарка\n" +
            "Без записи на вики.\n" +
            "[DARIA-32952]")
    public void recognizerB2Btest() throws IOException {
        MessageBody msgOper = jsx(MessageBody.class).params(MessageObj.getMsg(messageId));

        Message headerRespTest = jsx(MessageHeader.class).params(MessageObj.getMsg(messageId)).post().via(hc)
                .as(Message.class);

        Message bodyResp = msgOper.post().via(hc).as(Message.class);

        String lang = bodyResp.getLangCode();

        assertThat(String.format("Для письма с темой: <%s>, неправильно определили язык: %s. ",
                headerRespTest.getSubject(), lang), lang, equalTo(expectedLang.code()));
    }
}
