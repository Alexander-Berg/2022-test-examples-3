package ru.yandex.autotests.innerpochta.mops;

import lombok.val;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.*;
import ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils;
import ru.yandex.autotests.innerpochta.wmi.core.utils.WaitForMessage;
import ru.yandex.qatools.allure.annotations.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.innerpochta.mops.MopsCommonTest.*;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.matchers.messages.IsThereMessagesMatcher.*;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.invalidRequest;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.rules.HttpClientManagerRule.auth;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 07.10.14
 * Time: 19:38
 * <p/>
 * https://wiki.yandex-team.ru/users/shelkovin/asyncoperations/http-interface
 */
@Aqua.Test
@Title("[MOPS] Ручка purge. Новый сервис mops на 8814 порту")
@Description("Удаляем  навсегда письма по mid, tid")
@Features(MyFeatures.MOPS)
@Stories(MyStories.MOPS_PURGE)
@Issue("AUTOTESTPERS-142")
@Credentials(loginGroup = "PurgeMopsTest")
public class PurgeTest extends MopsBaseTest {
    private static final int COUNT_OF_LETTERS = 2;

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @ClassRule
    public static HttpClientManagerRule sender = auth().with("PurgeMops2Test");

    @Test
    @Title("PURGE без mid-ов и tid-ов")
    @Description("Делаем запрос purge без mid и tid [DARIA-41183]")
    public void purgeWithoutMidsAndTids() throws Exception {
        val expectedError = "invalid arguments: mids or tids or fid or lid or tab not found";
        purge(new EmptySource()).post(shouldBe(invalidRequest(equalTo(expectedError))));
    }

    @Test
    @Title("PURGE с несуществующим fid")
    @Description("Делаем запрос purge с несуществующим fid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void purgeWithNotExistFid() throws Exception {
        purge(new FidSource(NOT_EXIST_FID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("PURGE с невалидным fid")
    @Description("Делаем запрос purge с невалидным fid")
    public void purgeWithInvalidFid() throws Exception {
        purge(new FidSource(INVALID_FID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с несуществующим mid")
    @Description("Делаем запрос purge с несуществующим mid [DARIA-41183].\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void purgeWithNotExistMid() throws Exception {
        purge(new MidsSource(NOT_EXIST_MID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("PURGE с невалидным mid")
    @Description("Делаем запрос purge с невалидным mid")
    public void purgeWithInvalidMid() throws Exception {
        purge(new MidsSource(INVALID_MID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с невалидным tid и невалидным mid")
    @Description("Делаем запрос purge с невалидным tid и невалидным mid")
    public void purgeWithInvalidTidAndInvalidMid() throws Exception {
        purge(new MidsWithTidsSource(INVALID_MID, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с невалидным tid и несуществующим mid")
    @Description("Делаем запрос purge с невалидным tid и несуществующим mid")
    public void purgeWithInvalidTidAndNotExistsMid() throws Exception {
        purge(new MidsWithTidsSource(NOT_EXIST_MID, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с невалидным tid и mid")
    @Description("Делаем запрос purge с невалидным tid и mid")
    public void purgeWithInvalidTidAndMid() throws Exception {
        val mid = sendMail().firstMid();
        purge(new MidsWithTidsSource(mid, INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с несуществующим tid и невалидным mid")
    @Description("Делаем запрос purge с несуществующим tid и невалидным mid")
    public void purgeWithNotExistsTidAndInvalidMid() throws Exception {
        purge(new MidsWithTidsSource(INVALID_MID, NOT_EXIST_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с несуществующим tid и mid")
    @Description("Делаем запрос purge с несуществующим tid и mid")
    public void purgeWithNotExistsTidAndMid() throws Exception {
        val context = sendMail();
        val mid = context.firstMid();
        val subject = context.subject();

        purge(new MidsWithTidsSource(mid, NOT_EXIST_TID)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Title("PURGE с tid и невалидным mid")
    @Description("Делаем запрос purge с tid и невалидным mid")
    public void purgeWithTidAndInvalidMid() throws Exception {
        val tid = sendMail().firstTid();
        purge(new MidsWithTidsSource(INVALID_MID, tid)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с tid и несуществующим mid")
    @Description("Делаем запрос purge с tid и несуществующим mid")
    public void purgeWithTidAndNotExistsMid() throws Exception {
        val context = sendMail();
        val subject = context.subject();
        purge(new MidsWithTidsSource(NOT_EXIST_MID, context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение будет удалено из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Title("PURGE с tid и mid")
    @Description("Делаем запрос purge с tid и mid")
    public void purgeWithTidAndMid() throws Exception {
        val tidContext = sendMail();
        val midContext = sendMail();
        val tidSubject = tidContext.subject();
        val midSubject = midContext.subject();

        purge(new MidsWithTidsSource(midContext.firstMid(), tidContext.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(tidSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение будет удалено из \"Отправленных\"",
                authClient, not(hasMsgIn(tidSubject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(midSubject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(tidSubject, folderList.deletedFID())));
        assertThat("Ожидалось, что сообщения не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(midSubject, folderList.deletedFID())));
    }

    @Test
    @Title("PURGE с tid и mid из tid")
    @Description("Делаем запрос purge с tid и mid из tid")
    public void purgeWithTidAndMidFromTid() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new MidsWithTidsSource(context.firstMid(), context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение будет удалено из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение будет удалено из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Title("PURGE с существующим и несуществующим mid")
    @Description("Делаем запрос purge с одним существующим и одним с несуществующим mid")
    public void purgeWithExistAndNotExistsMids() throws Exception {
        val context = sendMail();
        val mids = context.mids();
        val subject = context.subject();

        mids.add(NOT_EXIST_MID);
        purge(new MidsSource(mids)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не появится в \"Удаленных\"", authClient,
                not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Title("PURGE с несуществующим tid")
    @Description("Делаем запрос purge с несуществующим tid.\n" +
                 "В MAILDEV-588 решили не выдавать ошибки для несуществующих элементов.")
    public void purgeWithNotExistTid() throws Exception {
        purge(new TidsSource(NOT_EXIST_TID)).post(shouldBe(okSync()));
    }

    @Test
    @Title("PURGE с невалидным tid")
    @Description("Делаем запрос purge с невалидным tid")
    public void purgeWithInvalidTid() throws Exception {
        purge(new TidsSource(INVALID_TID)).post(shouldBe(invalidRequest()));
    }

    @Test
    @Title("PURGE с существующим и несуществующим tid")
    @Description("Делаем запрос purge с одним существующим и одним с несуществующим tid")
    public void purgeWithExistAndNotExistsTids() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new TidsSource(context.firstTid(), NOT_EXIST_TID)).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"", authClient,
                not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение удалится из \"Отправленных\"", authClient,
                not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения не появятся в \"Удаленных\"", authClient,
                not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Description("Удаляем одно письмо по миду навсегда, минуя \"Удаленные\"")
    @Title("PURGE с параметром mid")
    public void purgeMid() throws Exception {
        val context = sendMail();
        purge(new MidsSource(context.firstMid())).post(shouldBe(okSync()));
        shouldSeeMsgInDeletedAndOutbox(context.subject());
    }

    @Test
    @Description("Удаляем письма по тиду навсегда, минуя \"Удаленные\".\n" +
            "Ожидаемый результат: если тред удаляется, то удаляются и отправленные")
    @Title("PURGE с параметром tid")
    public void purgeTid() throws Exception {
        val context = sendMail(COUNT_OF_LETTERS);
        val subject = context.subject();

        purge(new TidsSource(context.firstTid())).post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщения удалятся из \"Входящих\"",
                authClient, not(hasMsgIn(subject, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщения удалятся из \"Отправленных\"",
                authClient, not(hasMsgIn(subject, folderList.sentFID())));
        assertThat("Ожидалось, что сообщения не появятся в \"Удаленных\"",
                authClient, not(hasMsgIn(subject, folderList.deletedFID())));
    }

    @Test
    @Issue("DARIA-42692")
    @Title("Проверка PURGE фильтра from с папкой inbox")
    @Description("Удаляем письма с фильтром from")
    public void purgeWithFilterFrom() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new FidSource(folderList.defaultFID()).withFrom("testtoemail@yandex.ru"))
                .post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не удалится из \"Входящих\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));

        purge(new FidSource(folderList.defaultFID())
                .withFrom(authClient.acc().getLogin().toLowerCase()))
                .post(shouldBe(okSync()));
        shouldSeeMsgInDeletedAndOutbox(subject);
    }

    @Test
    @Issue("DARIA-42692")
    @Title("Проверка PURGE фильтра age с папкой inbox")
    @Description("Удаляем письма с фильтром age")
    public void purgeAgeWithFilterAge() throws Exception {
        val context = sendMail();
        val subject = context.subject();

        purge(new FidSource(folderList.defaultFID()).withAge(100)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не удалится из \"Входящих\"",
                authClient, hasMsgIn(subject, folderList.defaultFID()));

        purge(new FidSource(folderList.defaultFID()).withAge(0)).post(shouldBe(okSync()));
        shouldSeeMsgInDeletedAndOutbox(subject);
    }

    @Test
    @Issue("DARIA-42692")
    @Title("Проверка PURGE фильтра subject с папкой inbox")
    @Description("Удаляем письма с фильтром subject")
    public void purgeWithFilterSubject() throws Exception {
        val controlContext = sendMail();
        val controlSubj = controlContext.subject();
        val context = sendMail();
        val subject = context.subject();
        val mergedSubj = controlSubj + subject;

        purge(new FidSource(folderList.defaultFID()).withSubject(mergedSubj)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщения не удалится из \"Входящих\"", authClient,
                allOf(hasMsgIn(subject, folderList.defaultFID()), hasMsgIn(controlSubj, folderList.defaultFID())));

        purge(new FidSource(folderList.defaultFID()).withSubject(subject)).post(shouldBe(okSync()));
        assertThat("Ожидалось, что сообщение не удалится из \"Входящих\"",
                authClient, hasMsgIn(controlSubj, folderList.defaultFID()));
        shouldSeeMsgInDeletedAndOutbox(subject);
    }

    @Test
    @Issue("DARIA-42692")
    @Title("Проверка PURGE с фильтрами с папкой inbox")
    @Description("Удаляем письма с различными фильтрами, проверяем комбинацию |")
    public void purgeWitauthClientombinationFilter() throws Exception {
        val controlSubj = getRandomString();
        SendbernarUtils.sendWith(sender).subj(controlSubj).to(authClient.acc().getSelfEmail()).send();
        new WaitForMessage(authClient).subj(controlSubj).waitDeliver();

        val subject = getRandomString();
        sendMail(subject);

        purge(new FidSource(folderList.defaultFID())
                .withSubject(subject)
                .withFrom(sender.acc().getSelfEmail()))
                .post(shouldBe(okSync()));

        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(controlSubj, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не появится в \"Удаленных\"",
                authClient, not(hasMsgIn(controlSubj,  folderList.deletedFID())));

        shouldSeeMsgInDeletedAndOutbox(subject);
    }

    private void shouldSeeMsgInDeletedAndOutbox(String subj) {
        assertThat("Ожидалось, что сообщение удалится из \"Входящих\"",
                authClient, not(hasMsgIn(subj, folderList.defaultFID())));
        assertThat("Ожидалось, что сообщение не удалится из \"Отправленных\"",
                authClient, hasMsgIn(subj, folderList.sentFID()));
        assertThat("Ожидалось, что сообщение не появится в \"Удаленных\"",
                authClient, not(hasMsgIn(subj, folderList.deletedFID())));
    }


}
