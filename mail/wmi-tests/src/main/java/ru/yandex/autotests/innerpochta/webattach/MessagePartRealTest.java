package ru.yandex.autotests.innerpochta.webattach;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.webattach.MessagePartReal;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.AttachUtils;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.weattach.MessagePartRealObj.emptyObj;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.03.15
 * Time: 20:28
 */
@Aqua.Test
@Title("Ручка message_part_real")
@Description("Проверяем ручку message_part_real с невалидными данными")
@Features(MyFeatures.WEBATTACH)
@Stories(MyStories.ATTACH)
@Issue("DARIA-45531")
@Credentials(loginGroup = "RetreiverMessagePartRealTest")
@RunWith(DataProviderRunner.class)
public class MessagePartRealTest extends BaseWebattachTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @DataProvider
    public static Object[][] houndUri() {
        return new Object[][] {
                {props().houndUri()},
                {props().houndB2bUri()}
        };
    }

    @Test
    @Title("Отсутствие sid")
    @Issue("DARIA-45531")
    @Description("Когда не передавали sid получали неправильный статус код 404 и ошибку в логе:" +
            "message_part_real status=error reason=[can't get part: libmulca get_part returns error: UNIT_NOT_FOUND (:)]" +
            "Сейчас должны получать 400")
    public void messagePartRealWithEmptySidShouldSee400() {
        api(MessagePartReal.class).setHost(props().webattachHost()).get().via(hc)
                .statusCodeShouldBe(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @Title("Неправильный sid")
    @Description("Делаем запрос message_part_real с неправильным параметром sid. \n" +
            "Ожидаемый результат: 401")
    public void messagePartRealWithWrongSidShouldSee401() {
        api(MessagePartReal.class).setHost(props().webattachHost())
                .params(emptyObj().setSid(Util.getRandomString())).get().via(hc)
                .statusCodeShouldBe(HttpStatus.SC_UNAUTHORIZED);
    }

    @Test
    @Title("Message_part_real без авторизации")
    @Description("Делаем запрос message_part_real без кук")
    public  void messagePartRealWithoutAuthButWithCorrectSidShouldSee200() throws IOException {
        File attach = AttachUtils.genFile(33);
        attach.deleteOnExit();

        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();
        final  List<String> hids = Collections.singletonList("1.1");
        List<String> sids = getAttachSid(mid, hids);
        assertEquals("Неожиданное кол-во сидов", sids.size(), 1);

        api(MessagePartReal.class).setHost(props().webattachHost())
                .params(emptyObj().setSid(sids.get(0))).get().via(authClient.notAuthHC())
                .statusCodeShouldBe(HttpStatus.SC_OK);
    }

    @Test
    @Title("Правильный sid")
    @Description("Проверяем, что retriever сможет распаковать attach_sid, сгенерированный как houndUri, так и houndB2bUri")
    @UseDataProvider("houndUri")
    public void messagePartRealWithRightSidShouldSee200(String houndHost) throws IOException {
        File attach = AttachUtils.genFile(33);
        attach.deleteOnExit();

        String mid = sendWith(authClient).viaProd().addAttaches(attach).send().waitDeliver().getMid();

        shouldSeeFile(urlOfAttach(mid, attach.getName(), houndHost), attach);
    }
}
