package ru.yandex.autotests.innerpochta.mbody;

import org.junit.*;

import static org.junit.Assert.*;


import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.mbody.*;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitForMessage;
import ru.yandex.qatools.allure.annotations.*;

import static java.util.function.Function.identity;

import java.io.IOException;

import javax.mail.MessagingException;


@Aqua.Test
@Issue("MAILDEV-1176")
@Features(MyFeatures.MBODY)
@Stories(MyStories.MBODY)
@Credentials(loginGroup = "DkimTest")
@Title("Проверка флагов MedalMaker")
public class DkimTest extends MbodyBaseTest {

    private static String midPassMail;
    private static String midFailMail;
    private static Dkim dkim;

    @Before
    public  void openSession() {
        WaitForMessage waitWith = new WaitForMessage(authClient);
        midPassMail = waitWith.subj("dkim=pass").waitDeliver().getMid();
        assertNotEquals("Письмо с флагом dkim=pass не существует", midPassMail, null);

        midFailMail = waitWith.subj("dkim=fail").waitDeliver().getMid();
        assertNotEquals("Письмо с флагом dkim=fail не существует", midFailMail, null);

        dkim = new Dkim();
        dkim.setHeaderi("@yandex.ru");
    }


    @Test
    @Title("Проверяем наличие флагов MedalMaker и dkim=pass")
    public void shouldReturnMbodyWithDkimStatusPass() throws IOException, MessagingException {
        Mbody res = apiMbody().message()
                .withMid(midPassMail)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);

        assertEquals("Тело письма должно содержать флаг dkimStatus со значением pass",
                res.getInfo().getDkimStatus(), "pass");

        assertEquals("Тело письма должно содержать флаг signedBy со значением yandex.ru",
                res.getInfo().getSignedBy(), "yandex.ru");

        assertEquals("Тело письма должно содержать флаг senderDomain со значением yandex.ru",
                res.getInfo().getSenderDomain(), "yandex.ru");

        assertEquals("Тело письма должно содержать флаг encrypted со значением true",
                res.getInfo().getEncrypted(), true);

        assertEquals("Тело письма должно содержать флаг isSpam со значением false",
                res.getInfo().getIsSpam(), false);

        assertEquals("Тело письма должно содержать флаг collectedRpopId",
                res.getInfo().getCollectedRpopId(), "");

        dkim.setStatus(new Long(1));
        assertEquals("Тело письма должно содержать  dkim = [1, @yandex.ru]",
                res.getInfo().getDkim(), dkim);
    }


    @Test
    @Title("Проверяем наличие флагов MedalMaker и dkim=fail")
    public void shouldReturnMbodyWithDkimStatusFail() throws IOException, MessagingException {
        Mbody res = apiMbody().message()
                .withMid(midFailMail)
                .withUid(uid())
                .get(identity()).peek().as(Mbody.class);

        assertEquals("Тело письма должно содержать флаг dkim со значением fail",
                res.getInfo().getDkimStatus(), "fail");

        assertEquals("Тело письма должно содержать флаг signedBy",
                res.getInfo().getSignedBy(), "");

        assertEquals("Тело письма должно содержать флаг senderDomain со значением yandex.ru",
                res.getInfo().getSenderDomain(), "yandex.ru");

        assertEquals("Тело письма должно содержать флаг encrypted со значением true",
                res.getInfo().getEncrypted(), true);

        assertEquals("Тело письма должно содержать флаг isSpam со значением false",
                res.getInfo().getIsSpam(), false);

        assertEquals("Тело письма должно содержать флаг collectedRpopId",
                res.getInfo().getCollectedRpopId(), "");

        dkim.setStatus(new Long(3));
        assertEquals("Тело письма должно содержать флаг dkim = [3, @yandex.ru]",
                res.getInfo().getDkim(), dkim);
    }
}
