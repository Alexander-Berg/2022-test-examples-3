package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.Envelope;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.mops.mark.ApiMark;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesUnreadByFolder;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesUnreadByFolderObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка /messages_unread_by_folder")
@Features(MyFeatures.HOUND)
@Stories(MyStories.OTHER)
@Credentials(loginGroup = "MessagesUnreadByFolderTest")
public class MessagesUnreadByFolderTest extends BaseHoundTest {
    @Rule
    public DeleteFoldersRule folderDelete = DeleteFoldersRule.with(authClient).all();

    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).before(true).allfolders();

    @Test
    @Title("Проверяем непрочитанные письма в пользовательской папке")
    @Description("Посылаем два письма. Складываем в пользовательскую папку." +
            " Читаем одно. Дёргаем ручку. Ожидаем в ответе одно письмо")
    public void shouldSeeOnlyUnreadInUserFolder() throws Exception {
        String mid1 = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String mid2 = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        String folderName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);

        Mops.complexMove(authClient, fid, new MidsSource(mid1, mid2))
                .post(shouldBe(okSync()));

        Mops.mark(authClient, new MidsSource(mid2), ApiMark.StatusParam.READ)
                .post(shouldBe(okSync()));

        List<Envelope> envelopes = api(MessagesUnreadByFolder.class)
                .setHost(props().houndUri())
                .params(empty().setUid(uid())
                .setFid(fid)
                .setCount("30")
                .setFirst("0"))
                .get()
                .via(authClient).withDebugPrint()
                .resp().getEnvelopes();

        assertThat("Ожидали ровно одно письмо", envelopes.size(), equalTo(1));
        assertThat("Ожидали другое письмо", envelopes.get(0).getMid(), equalTo(mid1));
    }
}