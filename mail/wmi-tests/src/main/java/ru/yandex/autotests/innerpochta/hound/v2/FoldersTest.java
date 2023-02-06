package ru.yandex.autotests.innerpochta.hound.v2;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.seleniumhq.jetty7.http.HttpStatus;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.Folder;
import ru.yandex.autotests.innerpochta.beans.hound.V2FoldersResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.innerpochta.wmi.core.api.CommonApiSettings.shouldBe;
import static ru.yandex.autotests.innerpochta.wmi.core.consts.InvalidArguments.UNEXISTING_UID;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgument;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.invalidArgumentWithCode;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.ok200;
import static ru.yandex.autotests.innerpochta.wmi.core.hound.HoundResponses.unknownUid;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/folders")
@Description("Тесты на ручку v2/folders")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "HoundV2FoldersTest")
public class FoldersTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all().before(true);

    @Test
    @Title("Ручка v2/folders с системной и пользовательской папкой")
    @Description("Создаём папку." +
        "Проверяем, что ручка возвращает созданную папку и хотя бы одну системную.")
    public void shouldReceiveUserAndSystemFolders() {
        String folderName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);

        List<Folder> folders = getFolders();

        assertTrue("Не нашли ни одной системной папки", folders.stream()
            .anyMatch(folder -> folder.getType().equals("system")));
        assertTrue("Не нашли созданную папку", folders.stream()
            .anyMatch(folder -> folder.getName().equals(folderName) && folder.getId().equals(fid)));
    }

    @Test
    @Title("Проверка обновления поля revision пользовательской папки")
    @Description("Создаём папку, в цикле изменяем её содержимое и проверяем, что поле revision изменяется.")
    public void revisionShouldUpdateWhenMailWasSent() {
        Folder initialFolder = GetSentFolder();
        sendWith(authClient).viaProd().send().strict().waitDeliver();
        Folder modifiedFolder = GetSentFolder();

        assertNotNull("Поле 'revision' отсутствует", modifiedFolder.getRevision());

        assertThat("Поле 'revision' не увеличилось",
            modifiedFolder.getRevision(),
            greaterThan(initialFolder.getRevision()));
    }

    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().folders()
            .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().folders()
            .withUid(UNEXISTING_UID)
            .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().folders()
            .withUid("abacaba")
            .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    private Folder GetSentFolder() {
        List<Folder> folders = getFolders();
        Optional<Folder> sentFolder = folders.stream()
            .filter(folder -> folder.getId().equals(folderList.sentFID()))
            .findFirst();
        assertTrue("Не нашли фолдера по sentFid: " + folderList.sentFID(), sentFolder.isPresent());
        return sentFolder.get();
    }

    private List<Folder> getFolders() {
        return apiHoundV2().folders()
            .withUid(uid())
            .get(shouldBe(ok200()))
            .body().as(V2FoldersResponse.class)
            .getFolders();
    }
}
