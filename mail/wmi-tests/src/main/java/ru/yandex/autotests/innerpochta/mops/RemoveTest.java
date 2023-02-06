package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.remove.ApiRemove.WithSentParam;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 14.10.14
 * Time: 17:26
 */
@Aqua.Test
@Title("[MOPS] Ручка remove. Новый сервис mops на 8814 порту")
@Description("Удаляем письма по mid, tid (переносим в корзину)")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_REMOVE)
@Issue("AUTOTESTPERS-142")
@Credentials(loginGroup = "RemoveMopsTest")
public class RemoveTest extends MopsBaseTest {
    private static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Test
    @Title("REMOVE без mid-ов и tid-ов")
    @Description("Делаем запрос remove без mid и tid")
    public void removeWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        remove(new EmptySource()).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("REMOVE с несуществующим mid")
    @Description("Делаем запрос remove с несуществующим mid [DARIA-41183].\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void removeWithNotExistMid() throws Exception {
        remove(new MidsSource(NOT_EXIST_MID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("REMOVE с невалидным mid")
    @Description("Делаем запрос remove с невалидным mid")
    public void removeWithInvalidMid() throws Exception {
        remove(new MidsSource(INVALID_MID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с несуществующим tid")
    @Description("Делаем запрос remove с несуществующим tid [DARIA-41183].\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void removeWithNotExistTid() throws Exception {
        remove(new TidsSource(NOT_EXIST_TID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("REMOVE с невалидным tid")
    @Description("Делаем запрос remove с невалидным tid")
    public void removeWithInvalidTid() throws Exception {
        remove(new TidsSource(INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с несуществующим fid")
    @Description("Делаем запрос remove с несуществующим fid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void removeWithNotExistFid() throws Exception {
        remove(new FidSource(NOT_EXIST_FID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("REMOVE с невалидным fid")
    @Description("Делаем запрос remove с невалидным fid")
    public void removeWithInvalidFid() throws Exception {
        remove(new FidSource(INVALID_FID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с существующим и несуществующим mid")
    @Description("Делаем запрос remove с существующим и несуществующим mid")
    public void removeWithExistAndNotExistFid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new MidsSource(context.firstMid(), NOT_EXIST_MID)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение будет перенесно в \"Удаленные\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));
    }

    @Test
    @Title("REMOVE с существующим и несуществующим tid")
    @Description("Делаем запрос remove с существующим и несуществующим tid")
    public void removeWithExistAndNotExistTid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new TidsSource(context.firstTid(), NOT_EXIST_TID)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение будет удалено из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения будут перенесены в \"Удаленные\"",
                authClient, hasMsgsIn(subject, 2, folderList.deletedFID()));
    }

    @Test
    @Title("REMOVE с невалидным tid и невалидным mid")
    @Description("Делаем запрос remove с невалидным tid и невалидным mid.")
    public void removeWithInvalidTidAndInvalidMid() throws Exception {
        remove(new MidsWithTidsSource(INVALID_MID, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с невалидным tid и несуществующим mid")
    @Description("Делаем запрос remove с невалидным tid и несуществующим mid.")
    public void removeWithInvalidTidAndNotExistsMid() throws Exception {
        remove(new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с невалидным tid и mid")
    @Description("Делаем запрос remove с невалидным tid и mid.")
    public void removeWithInvalidTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        remove(new MidsWithTidsSource(mid, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с несуществующим tid и невалидным mid")
    @Description("Делаем запрос remove с несуществующим tid и невалидным mid.")
    public void removeWithNotExistsTidAndInvalidMid() throws Exception {
        remove(new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с несуществующим tid и mid")
    @Description("Делаем запрос remove с несуществующим tid и mid.")
    public void removeWithNotExistsTidAndMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new MidsWithTidsSource(context.firstMid(), NOT_EXIST_TID)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение появится в \"Удаленных\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));
    }

    @Test
    @Title("REMOVE с tid и невалидным mid")
    @Description("Делаем запрос remove с tid и невалидным mid.")
    public void removeWithTidAndInvalidMid() throws Exception {
        val tid = sendMail().firstTid();
        remove(new MidsWithTidsSource(INVALID_MID, tid)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("REMOVE с tid и несуществующим mid")
    @Description("Делаем запрос remove с tid и несуществующим mid.")
    public void removeWithTidAndNotExistsMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new MidsWithTidsSource(NOT_EXIST_MID, context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Удаленных\"",
                authClient, hasMsgsIn(subject, 2, folderList.deletedFID()));
    }

    @Test
    @Title("REMOVE с tid и mid")
    @Description("Делаем запрос remove с tid и mid.")
    public void removeWithTidAndMid() throws Exception {
        val tidContext = sendMail();
        val midContext = sendMail();
        val tidSubject = tidContext.subject();
        val midSubject = midContext.subject();

        remove(new MidsWithTidsSource(midContext.firstMid(), tidContext.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(tidSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что второе сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(midSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"",
                authClient, not(hasMsgIn(tidSubject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Удаленных\"",
                authClient, hasMsgsIn(tidSubject, 2, folderList.deletedFID()));
        assertThat("Ожидалось, что второе сообщение появится в \"Удаленных\"",
                authClient, hasMsgIn(midSubject, folderList.deletedFID()));
    }

    @Test
    @Title("REMOVE с tid и mid из tid")
    @Description("Делаем запрос remove с tid и mid из tid.")
    public void removeWithTidAndMidFromTid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new MidsWithTidsSource(context.firstMid(), context.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Удаленных\"",
                authClient, hasMsgsIn(subject, 2, folderList.deletedFID()));
    }

    @Test
    @Description("Удаляем одно письмо по миду, должно переместитья в корзину\n" +
                 "затем удаляем еще раз навсегда, минуя \"Удаленные\"")
    @Title("REMOVE с параметром mid")
    public void removeMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val mid = context.firstMid();

        remove(new MidsSource(mid)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не удалится из \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в \"Удаленных\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));

        remove(new MidsSource(mid)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение исчезнет из \"Удаленных\" после повторного удаления",
                authClient, not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Issue("DARIA-42692")
    @Title("REMOVE c фильтром from")
    @Description("Удаляем письма из папки с фильтром subject")
    public void removeWithFilterSubject() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val subject = context.subject();

        remove(new FidSource(folderList.defaultFID()).withSubject(subject)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщения удалятся из \"Входящих\"",
                authClient, not(hasMsgsIn(COUNT_OF_LETTERS, subject)));
        assertThat("Ожидалось, что сообщения не удалятся из \"Отправленных\"",
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.sentFID()));
        assertThat("Ожидалось, что сообщения появится в \"Удаленных\"",
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.deletedFID()));

        remove(new FidSource(folderList.deletedFID())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщения исчезнут из \"Удаленных\" после повторного удаления",
                authClient, not(hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.deletedFID())));
    }

    @Test

    @Issue("DARIA-42354")
    @Description("Удаляем письмо с непустым nopurge -> должно переместиться в корзину\n" +
            "Повторно удаляем письмо с непустым nopurge -> должно остаться в корзине")
    public void removeFromDeletedNopurge() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new MidsSource(context.firstMid());

        remove(source).withNopurge("yes").post(shouldBe(okSync()));

        //удаляем письмо с непустым nopurge -> должно переместиться в корзину
        remove(source).withNopurge("yes").post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не удалится из \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в \"Удаленных\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));

        //повторно удаляем письмо с непустым nopurge -> должно остаться в корзине
        remove(source).withNopurge("yes").post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение останется в \"Удаленных\" после повторного удаления",
                authClient, hasMsgIn(subject, folderList.deletedFID()));
    }

    @Test
    @Issue("DARIA-42354")
    @Description("Удаляем письмо с пустым nopurge -> должно переместиться в корзину\n" +
            "Повторно удаляем письмо с пустым nopurge -> должно удалиться")
    public void removeFromDeletedWithoutNopurge() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        val source = new MidsSource(context.firstMid());

        //удаляем письмо с пустым nopurge -> должно переместиться в корзину
        remove(source).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не удалится из \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в \"Удаленных\"",
                authClient, hasMsgIn(subject, folderList.deletedFID()));

        //повторно удаляем письмо с пустым nopurge -> должно удалиться
        remove(source).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не останется в \"Удаленных\" после повторного удаления",
                authClient, not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("REMOVE без параметра with_sent")
    @Description("Удаляем цепочку писем по tid без параметра with_sent." +
            "\nПроверяем, что письмо из папки \"отправленные\" также удалено.")
    public void removeWithoutParameterWithSent() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new TidsSource(context.firstTid())).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Удаленных\"",
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.deletedFID()));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("REMOVE с параметром with_sent")
    @Description("Удаляем цепочку писем по tid с параметром with_sent=0." +
            "\nПроверяем, что письмо из папки \"отправленные\" не удалено.")
    public void removeUsingParameterWithSentFalse() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new TidsSource(context.firstTid())).withWithSent(WithSentParam._0).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не удалится из \"Отправленных\"",
                authClient, hasMsgIn(subject, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение появится в \"Удаленных\"",
                authClient, hasMsgsIn(subject, 1, folderList.deletedFID()));
    }

    @Test
    @Issue("MAILDEV-292")
    @Title("REMOVE с параметром with_sent")
    @Description("Удаляем цепочку писем по tid с параметром with_sent=1." +
                 "\nПроверяем, что письмо из папки \"отправленные\" также удалено.")
    public void removeUsingParameterWithSentTrue() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        remove(new TidsSource(context.firstTid())).withWithSent(WithSentParam._1).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения появятся в \"Удаленных\"",
                authClient, hasMsgsIn(subject, COUNT_OF_LETTERS, folderList.deletedFID()));
    }
}
