package ru.yandex.autotests.innerpochta.hound.v2;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.innerpochta.beans.hound.FoldersTree;
import ru.yandex.autotests.innerpochta.beans.hound.V2FoldersTreeResponse;
import ru.yandex.autotests.innerpochta.hound.BaseHoundTest;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.Credentials;
import ru.yandex.autotests.innerpochta.wmi.core.base.anno.IgnoreForPg;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyFeatures;
import ru.yandex.autotests.innerpochta.wmi.core.consts.MyStories;
import ru.yandex.autotests.innerpochta.wmi.core.hound.v2.folderstree.ApiFoldersTree;
import ru.yandex.autotests.innerpochta.wmi.core.oper.mops.Mops;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.CleanMessagesMopsRule;
import ru.yandex.autotests.innerpochta.wmi.core.rules.mops.DeleteFoldersRule;
import ru.yandex.autotests.innerpochta.wmicommon.Util;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import static ru.yandex.autotests.innerpochta.wmi.core.mops.MopsResponses.okEmptyJson;
import static ru.yandex.autotests.innerpochta.wmi.core.utils.SendbernarUtils.sendWith;

@Aqua.Test
@Title("[HOUND] Ручка v2/folders_tree")
@Description("Тесты на ручку v2/folders_tree")
@Features(MyFeatures.HOUND)
@Stories(MyStories.FOLDERS)
@Credentials(loginGroup = "HoundV2FoldersTreeTest")
public class FoldersTreeTest extends BaseHoundTest {
    @Rule
    public CleanMessagesMopsRule clean = CleanMessagesMopsRule.with(authClient).allfolders();

    @Rule
    public DeleteFoldersRule clearFolders = DeleteFoldersRule.with(authClient).all().before(true);

    @Test
    @Title("Ручка v2/folders_tree с системной и пользовательской папкой")
    @Description("Создаём папку." +
        "Проверяем, что ручка возвращает созданную папку и хотя бы одну системную.")
    public void shouldReceiveUserAndSystemFolders() {
        String folderName = Util.getRandomString();
        String fid = Mops.newFolder(authClient, folderName);

        List<FoldersTree> folders = getFolders();

        assertTrue("Не нашли ни одной системной папки", folders.stream()
            .anyMatch(folder -> folder.getType().equals("system")));
        assertTrue("Не нашли созданную папку", folders.stream()
            .anyMatch(folder -> folder.getName().equals(folderName) && folder.getId().equals(fid)));
    }

    @Test
    @Title("Проверка обновления поля revision пользовательской папки")
    @Description("Создаём папку, в цикле изменяем её содержимое и проверяем, что поле revision изменяется.")
    public void revisionShouldUpdateWhenMailWasSent() {
        FoldersTree initialFolder = GetSentFolder();
        sendWith(authClient).viaProd().send().strict().waitDeliver();
        FoldersTree modifiedFolder = GetSentFolder();

        assertNotNull("Поле 'revision' отсутствует", modifiedFolder.getRevision());

        assertThat("Поле 'revision' не увеличилось",
            modifiedFolder.getRevision(),
            greaterThan(initialFolder.getRevision()));
    }


    @Test
    @Title("Проверка вызова без uid'а")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400WithoutUid() {
        apiHoundV2().foldersTree()
            .withSort("by_fid")
            .get(shouldBe(invalidArgument(equalTo("uid parameter is required"))));
    }

    @Test
    @Title("Проверка вызова с неизвестным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForUnknownUid() {
        apiHoundV2().foldersTree()
            .withUid(UNEXISTING_UID)
            .withSort("position")
            .get(shouldBe(unknownUid()));
    }

    @Test
    @Title("Проверка вызова с некорректным uid'ом")
    @IgnoreForPg("MAILPG-2767")
    public void shouldReceive400ForIncorrectUid() {
        apiHoundV2().foldersTree()
            .withUid("abacaba")
            .withSort("position")
            .get(shouldBe(invalidArgumentWithCode(equalTo(5001))));
    }

    @Test
    @Title("Проверка вызова с некорректным типом сортировки")
    public void shouldReceive400ForUnknownSortingType() {
        apiHoundV2().foldersTree()
            .withUid(uid())
            .withSort("by_fid")
            .get(shouldBe(invalidArgument(equalTo("unknown sorting type by_fid"))));
    }

    @Test
    @Title("Проверка вызова без указания типа сортировки")
    public void shouldReceive400WithoutSortingType() {
        apiHoundV2().foldersTree()
            .withUid(uid())
            .get(shouldBe(invalidArgument(equalTo("sorting type is empty"))));
    }

    @Test
    @Title("Проверка вызова с некорректным языком")
    public void shouldReceive400ForUnknownLang() {
        apiHoundV2().foldersTree()
            .withUid(uid())
            .withSort(ApiFoldersTree.SortParam.LANG)
            .withLang("co")
            .get(shouldBe(invalidArgument(equalTo("unsupported lang"))));
    }

    @Test
    @Title("Проверка вызова с сортировкой по алфавиту без указания языка")
    public void shouldReceive400WithoutLang() {
        apiHoundV2().foldersTree()
            .withUid(uid())
            .withSort(ApiFoldersTree.SortParam.LANG)
            .get(shouldBe(invalidArgument(equalTo("argument lang must be passed for sorting type lang"))));
    }

    @Test
    @Title("Проверка сортировки по позиции")
    @Description("Создаём две папки - должны быть отсортированы по алфавиту в сишной локали." +
        "Двигаем эти папки - порядок должен поменяться.")
    public void updateFolderPositionsShouldChangeFoldersOrder() {
        String firstFid = Mops.newFolder(authClient, "aa");
        String secondFid = Mops.newFolder(authClient, "zz");

        expectedFolderOrder("Неподраганные папки не отсортировались по алфавиту", firstFid, secondFid);

        Mops.updateFolderPosition(authClient, firstFid)
            .withPrevFid(secondFid)
            .post(shouldBe(okEmptyJson()));

        expectedFolderOrder("Подраганные папки не отсортировались по позиции", secondFid, firstFid);
    }

    private void expectedFolderOrder(String desc, String... fids) {
        List<String> expectedOrder = Arrays.asList(fids);
        List<FoldersTree> folders = getFolders();
        List<String> allFids = folders.stream()
            .map(FoldersTree::getId)
            .filter(expectedOrder::contains)
            .collect(Collectors.toList());
        assertThat(desc, allFids, equalTo(expectedOrder));
    }

    private FoldersTree GetSentFolder() {
        List<FoldersTree> folders = getFolders();
        Optional<FoldersTree> sentFolder = folders.stream()
            .filter(folder -> folder.getId().equals(folderList.sentFID()))
            .findFirst();
        assertTrue("Не нашли фолдера по sentFid: " + folderList.sentFID(), sentFolder.isPresent());
        return sentFolder.get();
    }

    private List<FoldersTree> getFolders() {
        return apiHoundV2().foldersTree()
            .withUid(uid())
            .withSort(ApiFoldersTree.SortParam.POSITION)
            .get(shouldBe(ok200()))
            .body().as(V2FoldersTreeResponse.class)
            .getFoldersTree();
    }
}
