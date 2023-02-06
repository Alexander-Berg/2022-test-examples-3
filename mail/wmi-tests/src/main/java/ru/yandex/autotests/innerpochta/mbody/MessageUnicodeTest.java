package ru.yandex.autotests.innerpochta.mbody;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.Mbody;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created by puffyfloof on 29.09.16.
 */
@Aqua.Test
@Title("[MBODY] Отправка писем на адреса с Unicode символами")
@Description("Сохраняем черновик с unicode-адресатом, достаем его через ручку message, проверяем, совпадают ли получатели")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Issues({@Issue("MAILDEV-480"), @Issue("MAILPG-1032")})
@Credentials(loginGroup = "MailWithUnicode")
@RunWith(value = Parameterized.class)
@Ignore("MAILDEV-1348")
public class MessageUnicodeTest extends MbodyBaseTest {

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Parameterized.Parameter
    public String addressee;

    @Parameterized.Parameters(name = "{0}")
    public static List<String> addressees() {
        return asList(
                "jøran@blåbærsyltetøy.gulbrandsen.priv.no",
                "अजय@डाटा.भारत");
    }

    @Test
    public void shouldNotChangeUnicodeAddressee() throws Exception {
        String mid = sendWith(authClient).viaProd().to(addressee).saveDraft().waitDeliver().getMid();

        String mbodyAddressee = apiMbody().message()
            .withMid(mid)
            .withUid(uid())
            .get(identity()).peek().as(Mbody.class)
            .getInfo().getAddressesResult().get(0).getEmail();

        assertThat("После сохранения в черновики Unicode-адресат письма не должен ломаться",
            mbodyAddressee, equalTo(addressee));
    }

    @Test
    @Description("Тест не работает на корпах, разрешены пуникодные адреса только для mxback-out")
    public void shouldSendLetterToUnicodeAddressee() throws Exception {
        sendWith(authClient).viaProd().to(addressee).subj(Util.getRandomString()).send();
    }
}
