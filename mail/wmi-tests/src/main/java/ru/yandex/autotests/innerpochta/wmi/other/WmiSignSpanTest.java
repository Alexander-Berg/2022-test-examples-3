package ru.yandex.autotests.innerpochta.wmi.other;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.base.BaseTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.filter.VDirectCut;
import ru.yandex.autotests.innerpochta.wmi.core.obj.MessageObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.MessageBody;
import ru.yandex.autotests.innerpochta.wmi.core.rules.local.CleanMessagesRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.Matchers.containsString;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.jsx;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.05.15
 * Time: 18:50
 */
@Aqua.Test
@Title("Проверка наличия wmi-sign span у подписей")
@Description("Проверяем наличие тэга у различных подписей")
@Features(MyFeatures.WMI)
@Stories(MyStories.SETTINGS)
@Issue("DARIA-46930")
@Credentials(loginGroup = "WmiSignSpanTest")
public class WmiSignSpanTest extends BaseTest {

    @Rule
    public CleanMessagesRule clean = CleanMessagesRule.with(authClient).all().allfolders();

    @Test
    @Title("Подпись должна сворачиваться")
    @Description("Проверяем наличие <span class=\"wmi-sign\"></span>")
    public void testSignSpan() throws Exception {
       String mid = sendWith.send("--\nя подпис").waitDeliver().send().getMid();
        jsx(MessageBody.class).params(MessageObj.getMsg(mid)).filters(new VDirectCut())
                .get().via(hc).assertResponse(containsString("<span class=\"wmi-sign\">-- <br/>я подпис<br/></span>"));
    }

    @Test
    @Title("Отправляем письмо только с \">\"")
    @Description("Откатывались, так как коркались из-за письма определенного вида, содержащий 1 символ >")
    public void testOnlyOneSymbol() throws Exception {
        String mid = sendWith.send(">").waitDeliver().send().getMid();
        jsx(MessageBody.class).params(MessageObj.getMsg(mid)).filters(new VDirectCut())
                .get().via(hc).assertResponse(containsString("<blockquote class=\"wmi-quote\">" +
                "<br/>" +
                "</blockquote>"));
    }
}


