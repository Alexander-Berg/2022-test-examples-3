package ru.yandex.autotests.innerpochta.hound.v2.negative;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.*;

@Aqua.Test
@Title("[HOUND] Ручка v2/first_envelope_date")
@Description("Тесты на ручку v2/first_envelope_date")
@Features(MyFeatures.HOUND)
@Credentials(loginGroup = "HoundV2GetFirstEnvelopeDateNegativeTest")
public class FirstEnvelopeDateNegativeTest extends BaseHoundTest {

    public static final String NOT_EXIST_FID = "12332199";

    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Test
    @Title("first_envelope_date с пустой inbox папкой")
    public void firstEnvelopeDateWithEmptyInboxFid() {
        apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid(folderList.inboxFID()).
                get(shouldBe(ok200()));
    }

    @Test
    @Title("first_envelope_date с пустой пользовательской папкой")
    public void firstEnvelopeDateWithEmptyUserFid() {
        String fid = Mops.newFolder(authClient, Util.getRandomString());
        apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid(fid).
                get(shouldBe(ok200()));
    }

    @Test
    @Title("first_envelope_date с некорректным Fid")
    public void firstEnvelopeDateWithIncorrectFid() {
        apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid(NOT_EXIST_FID).
                get(shouldBe(ok200()));
    }

    @Test
    @Title("first_envelope_date с пустым Fid")
    public void firstEnvelopeDateWithEmptyFid() {
        apiHoundV2().firstEnvelopeDate().withUid(uid()).withFid("").
                get(shouldBe(invalidArgument(CoreMatchers.equalTo("fid is missing or empty"))));
    }

    @Test
    @Title("first_envelope_date без Fid")
    public void firstEnvelopeDateWithoutFid() {
        apiHoundV2().firstEnvelopeDate().withUid(uid()).
                get(shouldBe(invalidArgument(CoreMatchers.equalTo("fid is missing or empty"))));
    }
}
