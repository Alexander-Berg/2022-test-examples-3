package ru.yandex.autotests.innerpochta.hound.v2.positive;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.Change_;
import ru.yandex.autotests.innerpochta.beans.hound.V2ChangesResponse;
import ru.yandex.autotests.innerpochta.beans.yplatform.Folder;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.hound.Hound;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;
import static ru.yandex.autotests.innerpochta.wmicommon.Util.getRandomString;

@Aqua.Test
@Title("[HOUND] Ручка v2/changes")
@Description("Тесты на ручку v2/changes")
@Features(MyFeatures.HOUND)
@Stories(MyStories.CHANGLOG)
@Credentials(loginGroup = "HoundV2Changes")
public class ChangesPositiveTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all().before(true);

    @Test
    @Title("Ответ для последней ревизии должен быть пустым")
    public void shouldReceiveEmptyListOfChangesForLastRevision() {
        String fid = Mops.newFolder(authClient, getRandomString());
        Map<String, Folder> folders = Hound.folders(authClient);

        assertThat("Не смогли получить созданную папку", folders.containsKey(fid));

        Long lastRevision = folders.get(fid).getRevision();

        List<Change_> changes = apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision(String.valueOf(lastRevision))
                .withMaxCount("100")
                .get(shouldBe(ok200()))
                .as(V2ChangesResponse.class)
                .getChanges();
        assertTrue("Ожидался пустой список изменений", changes.isEmpty());
    }

    @Test
    @Title("Store письма должен попадать в ответ и содержать envelope")
    public void shouldReceiveOneStoreChangeWithEnvelopeAfterSaveDraft() {
        String fid = Mops.newFolder(authClient, getRandomString());
        Map<String, Folder> folders = Hound.folders(authClient);

        assertThat("Не смогли получить созданную папку", folders.containsKey(fid));

        Long fromRevision = folders.get(fid).getRevision();

        String mid = sendWith(authClient).viaProd().saveDraft().waitDeliver().getMid();

        List<Change_> changes = apiHoundV2().changes()
                .withUid(authClient.account().uid())
                .withRevision(String.valueOf(fromRevision))
                .withMaxCount("100")
                .get(shouldBe(ok200()))
                .as(V2ChangesResponse.class)
                .getChanges();
        assertEquals("Ожидалось одно изменение в ответе", 1, changes.size());
        assertEquals("Изменение должно быть типа store", "store", changes.get(0).getType());
        assertEquals("Изменение типа store должно содержать Envelope", mid, changes.get(0).getValue().get(0).getMid());
    }
}
