package ru.yandex.autotests.innerpochta.wmi.mailsend;

import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MailSendMsgObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MailSend;
import ru.yandex.autotests.innerpochta.wmi.core.oper.Message;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.io.Files.asByteSink;
import static com.google.common.io.Resources.asByteSource;
import static com.google.common.io.Resources.getResource;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.Obj.XMLVERSION_DARIA2;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule.with;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils.attachsInMessageShouldBeSameAs;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.12.13
 * Time: 15:47
 * <p/>
 * К аккаунту привязан телефон +79213157505
 * на него и отправляем письмо
 */
@Aqua.Test
@Title("Отправка писем. Отправляем письмо на цифровой логин")
@Description("Отправляем письма с различными цифровыми логинами")
@Features(MyFeatures.WMI)
@Stories(MyStories.MAIL_SEND)
@Credentials(loginGroup = "DigitLogin")
@RunWith(Parameterized.class)
public class MailSendToDigitLogin extends BaseTest {

    @ClassRule
    public static HttpClientManagerRule authClient2 = auth().with("DigitLoginTo");

    private static File bmp;

    @Rule
    public CleanMessagesRule clean = with(authClient).all().inbox().outbox();

    @BeforeClass
    public static void init() throws Exception {
        bmp = File.createTempFile("1x1", ".bmp");
        asByteSink(bmp).write(asByteSource(getResource("img/1x1.bmp")).read());
    }

    //параметризуем по получателю
    @Parameterized.Parameters(name = "Login <{0}>")
    public static Collection<Object[]> data() throws Exception {
        List<Object[]> toList = new ArrayList<>();
        toList.add(new Object[]{"79213157505@yandex.ru"});
        toList.add(new Object[]{"+79213157505@yandex.ru"});
        toList.add(new Object[]{"89213157505@yandex.ru"});
        toList.add(new Object[]{"+89213157505@yandex.ru"});
        toList.add(new Object[]{"++79213157505@yandex.ru"});

        return toList;
    }

    private String to;

    public MailSendToDigitLogin(String to) {
        this.to = to;
    }

    @Test
    @Description("Отсылаем сообщения к пользователю с различными цифровыми логинами\n" +
            "Проверяем, что пришедшее письмо соответствует отправленному.")
    public void sendMailToDigitLogin() throws Exception {
        DefaultHttpClient hc2 = authClient2.authHC();

        String sendText = Util.getRandomString() + to;

        MailSendMsgObj msg = msgFactory.getSimpleEmptySelfMsg().setTo(to).setSend(sendText).addAtts(bmp);
        jsx(MailSend.class).params(msg).post().via(hc).statusOk();

        String mid = waitWith.usingHC(authClient2.authHC()).subj(msg.getSubj()).waitDeliver().getMid();
        attachsInMessageShouldBeSameAs(bmp, msg.getSubj(), hc2);

        Message message = api(Message.class).params(MessageObj.getMsg(mid).setXmlVersion(XMLVERSION_DARIA2))
                .post().via(hc2);

        assertThat("<To> в пришедшем письме отображается неправильно ", message.toEmail(), is(to));
        assertThat("<From> в пришедшем письме отображается неправильно ", message.fromEmail(),
                is(authClient.acc().getSelfEmail()));
        assertThat("Отправленный текст изменился ", message.getFirstlineText(), is(sendText));
    }
}
