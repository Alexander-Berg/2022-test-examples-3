package ru.yandex.autotests.innerpochta.hound;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.MessagesByFolderWithoutLabel;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.source.MidsSource;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteLabelsMopsRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.base.Exec.api;
import static ru.yandex.autotests.innerpochta.wmi.core.base.props.WmiCoreProperties.props;
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okSync;
import static ru.yandex.autotests.innerpochta.wmi.core.obj.hound.MessagesByFolderWithoutLabelObj.empty;
import static ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound.LabelSymbol.PINNED;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка messages_by_folder_without_label")
@Description("Тесты на ручку messages_by_folder_without_label")
@Features(MyFeatures.HOUND)
@Stories(MyStories.MESSAGES_LIST)
@Credentials(loginGroup = "MessagesByFolderWithoutLabelTest")
public class MessagesByFolderWithoutLabelTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = new CleanMessagesMopsRule(authClient).allfolders();

    @Rule
    public DeleteLabelsMopsRule deleteLabels = new DeleteLabelsMopsRule(authClient);

    @Test
    @Title("messages_by_folder_without_label with pinned label in inbox")
    public void testMessagesByFolderWithoutLabelWithAttachedFakeLabel() throws Exception {
        String pinnedMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String lid = Hound.getLidBySymbolTitle(authClient, PINNED);
        assertThat("Не нашли метку с символом " + PINNED, lid, not(equalTo("")));

        Mops.label(authClient, new MidsSource(pinnedMid), Collections.singletonList(lid))
                .post(shouldBe(okSync()));

        String notPinnedMid = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        MessagesByFolderWithoutLabel msgs = getMessagesByFolderWithoutLabel(folderList.defaultFID(), lid);

        assertTrue("Нашли письма по метке pinned", msgs.resp().getEnvelopes().stream()
                .noneMatch((envelope) -> envelope.getMid().equals(pinnedMid) && envelope.getLabels().contains(lid)));

        assertTrue("Не нашли письма без метки pinned", msgs.resp().getEnvelopes().stream()
                .anyMatch((envelope) -> envelope.getMid().equals(notPinnedMid)));
    }

    @Test
    @Title("messages_by_folder_without_label with user's label in inbox")
    public void testMessagesByFolderWithoutLabelWithUsersLabel() throws Exception {
        String midWithLabel = sendWith(authClient).viaProd().send().waitDeliver().getMid();
        String lid = Mops.newLabelByName(authClient, Util.getRandomString());

        Mops.label(authClient, new MidsSource(midWithLabel), Collections.singletonList(lid))
                .post(shouldBe(okSync()));

        String midWithoutLabel = sendWith(authClient).viaProd().send().waitDeliver().getMid();

        MessagesByFolderWithoutLabel msgs = getMessagesByFolderWithoutLabel(folderList.defaultFID(), lid);

        assertTrue("Нашли письма по пользовательской метке", msgs.resp().getEnvelopes().stream()
                .noneMatch((envelope) -> envelope.getMid().equals(midWithLabel) && envelope.getLabels().contains(lid)));

        assertTrue("Не нашли письма без пользовательской метки", msgs.resp().getEnvelopes().stream()
                .anyMatch((envelope) -> envelope.getMid().equals(midWithoutLabel)));
    }

    private MessagesByFolderWithoutLabel getMessagesByFolderWithoutLabel(String fid, String lid) {
        return api(MessagesByFolderWithoutLabel.class)
                .setHost(props().houndUri())
                .params(empty()
                        .setLid(lid)
                        .setFid(fid)
                        .setFirst("0")
                        .setCount("10")
                        .setUid(uid())
                )
                .get()
                .via(authClient).withDebugPrint();
    }
}
