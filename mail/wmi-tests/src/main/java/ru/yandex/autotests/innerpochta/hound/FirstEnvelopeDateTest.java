package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.obj.GetFirstEnvelopeDateObj;
import ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderObj;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.autotests.innerpochta.wmicommon.WmiConsts;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.FirstEnvelopeDate.firstEnvelopeDate;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.wmicommon.WmiConsts.WmiErrorCodes.DB_UNKNOWN_ERROR_1000;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 11.11.15
 * Time: 16:54
 */
@Aqua.Test
@Title("[HOUND] Новая ручка first_envelope_date. Эту ручку мы используем, чтобы рисовать пейджер по датам внизу списка писем.")
@Description("Сравниваем с продакшеном выдачу ручки first_envelope_date")
@Features(MyFeatures.HOUND)
@Stories(MyStories.MESSAGES_LIST)
@Issues({@Issue("DARIA-52616")})
@Credentials(loginGroup = "GetFirstEnvelopeDateTest")
public class FirstEnvelopeDateTest extends BaseHoundTest {

    public static final String ERROR_MESSAGE = "unknown database error";
    public static final String ERROR_REASON = "empty result from database, details:";

    public static final String NOT_EXIST_FID = "6666666666666666666";

    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).before(true).inbox().outbox().allfolders();

    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Test
    @Title("first_envelope_date с пустой папкой inbox")
    public void firstEnvelopeDateWithEmptyInbox() {
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid(folderList.defaultFID())
                .setUid(uid())).get().via(authClient).shouldBe().code(equalTo(
                DB_UNKNOWN_ERROR_1000.code())).message(equalTo(ERROR_MESSAGE))
                .reason(containsString(ERROR_REASON)).withDebugPrint();
    }

    @Test
    @Title("first_envelope_date с папкой inbox")
    public void firstEnvelopeDateWithInbox() throws Exception {
        sendWith(authClient).viaProd().send().waitDeliver().getMid();
        long utcTimestamp = api(MessagesByFolder.class).setHost(props().houndUri()).params(MessagesByFolderObj.empty()
                .setUid(uid()).setFirst("0").setCount("1")
                .setFid(folderList.defaultFID())).get().via(authClient)
                .resp().getEnvelopes().get(0).getReceiveDate();
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid(folderList.defaultFID())
                .setUid(uid())).get().via(authClient).shouldBeDate(utcTimestamp);
    }

    @Test
    @Title("first_envelope_date с пустой пользовательской папкой")
    public void firstEnvelopeDateWithEmptyUserFolder() {
        String fid = Mops.newFolder(authClient, Util.getRandomString());
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid(fid).setUid(uid())).get().via(authClient)
                .code(equalTo(DB_UNKNOWN_ERROR_1000.code())).message(equalTo(ERROR_MESSAGE))
                .reason(containsString(ERROR_REASON));
    }

    @Test
    @Title("first_envelope_date с пользовательской папкой")
    public void firstEnvelopeDateWithUserFolder() throws Exception {
        String fid = Mops.newFolder(authClient, Util.getRandomString());
        Envelope envelope = sendWith(authClient).viaProd().send().waitDeliver().getEnvelope().get();
        Mops.complexMove(authClient, fid, new MidsSource(envelope.getMid()))
                .post(shouldBe(okSync()));
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid(fid).setUid(uid())).get().via(authClient)
                .withDebugPrint().shouldBeDate(envelope.getReceiveDate());
    }

    @Test
    @Title("first_envelope_date с несуществующим fid")
    public void firstEnvelopeDateWithNotExistFidShouldSeeError() {
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid(NOT_EXIST_FID)
                .setUid(uid())).get().via(authClient)
                .code(equalTo(DB_UNKNOWN_ERROR_1000.code())).message(equalTo(ERROR_MESSAGE))
                .reason(equalTo(String.format("in query MailboxGetFirstEnvelopeDate request_op::get_result failed: ERROR:  " +
                        "value \"%s\" is out of range for type integer", NOT_EXIST_FID)));
    }


    @Test
    @Issue("MAILPG-518")
    @Title("first_envelope_date с пустым fid")
    public void firstEnvelopeDateWithEmptyFidShouldSeeError() {
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj().setFid("")
                .setUid(uid())).get().via(authClient)
                .code(equalTo(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001.code()))
                .reason(equalTo("fid is missing or empty"));
    }

    @Test
    @Issue("MAILPG-518")
    @Title("first_envelope_date без fid")
    public void firstEnvelopeDateWithoutFidShouldSeeError() {
        firstEnvelopeDate(GetFirstEnvelopeDateObj.getEmptyObj()
                .setUid(uid())).get().via(authClient)
                .code(equalTo(WmiConsts.WmiErrorCodes.INVALID_ARGUMENT_5001.code()))
                .reason(equalTo("fid is missing or empty"));
    }
}
