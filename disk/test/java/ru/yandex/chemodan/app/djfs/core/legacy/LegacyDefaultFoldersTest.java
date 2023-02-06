package ru.yandex.chemodan.app.djfs.core.legacy;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.user.UserNotInitializedException;
import ru.yandex.chemodan.util.test.JsonTestUtils;
import ru.yandex.misc.test.Assert;

public class LegacyDefaultFoldersTest extends LegacyActionsTestBase {
    @Test
    public void test_mail_store_for_user_under_experiment() {

    }

    @Test
    public void testExistsFlagMustFilterFolders() throws JsonProcessingException {
        Map<String, Map<String, Object>> result = JsonTestUtils.parseJsonToMap(
                legacyFilesystemActions.default_folders(UID_1.asString(), Option.of("1")).getResult().getBytes()
        );

        Assert.equals("/disk/Загрузки", result.get("downloads").get("path"));
        Assert.equals(0, result.get("downloads").get("exist"));

        Assert.equals("/disk/Скриншоты", result.get("screenshots").get("path"));
        Assert.equals(0, result.get("screenshots").get("exist"));

        Assert.equals("/disk/Яндекс.Книги", result.get("yabooks").get("path"));
        Assert.equals(0, result.get("yabooks").get("exist"));

        filesystem.createFolder(DjfsPrincipal.cons(UID_1), DjfsResourcePath.cons(UID_1, "/disk/Загрузки/"));
        filesystem.createFolder(DjfsPrincipal.cons(UID_1), DjfsResourcePath.cons(UID_1, "/disk/Скриншоты/"));
        filesystem.createFolder(DjfsPrincipal.cons(UID_1), DjfsResourcePath.cons(UID_1, "/disk/Яндекс.Книги"));

        result = JsonTestUtils.parseJsonToMap(
                legacyFilesystemActions.default_folders(UID_1.asString(), Option.of("1")).getResult().getBytes()
        );

        Assert.equals("/disk/Загрузки", result.get("downloads").get("path"));
        Assert.equals(1, result.get("downloads").get("exist"));

        Assert.equals("/disk/Скриншоты", result.get("screenshots").get("path"));
        Assert.equals(1, result.get("screenshots").get("exist"));

        Assert.equals("/disk/Яндекс.Книги", result.get("yabooks").get("path"));
        Assert.equals(1, result.get("yabooks").get("exist"));
    }

    @Test
    public void testUserNotInitialized() {
        String unknownUser = "1312312313";
        Assert.assertThrows(
                () -> legacyFilesystemActions.default_folders(unknownUser, Option.of("0")),
                UserNotInitializedException.class
        );
    }
}
