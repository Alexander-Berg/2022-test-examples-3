package ru.yandex.chemodan.app.djfs.core.legacy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.UserLocale;
import ru.yandex.chemodan.app.djfs.core.web.JsonStringResult;
import ru.yandex.chemodan.util.test.JsonTestUtils;
import ru.yandex.misc.test.Assert;

@RunWith(Parameterized.class)
public class LegacyDefaultFoldersSampleTest extends LegacyActionsTestBase {
    @Parameterized.Parameters(name = "{0}")
    public static Object[][] sample() {
        return new Object[][]{
                new Object[]{
                        "ru",
                        Cf.toMap(Tuple2List.<String, String>fromPairs(
                                "yaslovariarchive", "/attach/yaslovariarchive",
                                "yaruarchive", "/attach/yaruarchive",
                                "archive", "/attach/archive",
                                "google", "/disk/Социальные сети/Google+",
                                "instagram", "/disk/Социальные сети/Instagram",
                                "vkontakte", "/disk/Социальные сети/ВКонтакте",
                                "yabooks", "/disk/Яндекс.Книги",
                                "screenshots", "/disk/Скриншоты/",
                                "downloads", "/disk/Загрузки/",
                                "odnoklassniki", "/disk/Социальные сети/Одноклассники",
                                "applications", "/disk/Приложения",
                                "yalivelettersarchive", "/attach/yalivelettersarchive",
                                "facebook", "/disk/Социальные сети/Facebook",
                                "social", "/disk/Социальные сети/",
                                "mailru", "/disk/Социальные сети/Мой Мир",
                                "fotki", "/disk/Яндекс.Фотки/",
                                "photostream", "/disk/Фотокамера/",
                                "yateamnda", "/disk/Yandex Team (NDA)",
                                "yafotki", "/attach/YaFotki",
                                "attach", "/disk/Почтовые вложения",
                                "scans", "/disk/Сканы"
                        ))
                },
                new Object[]{
                        "en",
                        Cf.toMap(Tuple2List.<String, String>fromPairs(
                                "yaslovariarchive", "/attach/yaslovariarchive",
                                "yaruarchive", "/attach/yaruarchive",
                                "archive", "/attach/archive",
                                "google", "/disk/Social networks/Google+",
                                "instagram", "/disk/Social networks/Instagram",
                                "vkontakte", "/disk/Social networks/VK",
                                "yabooks", "/disk/Yandex.Books",
                                "screenshots", "/disk/Screenshots/",
                                "downloads", "/disk/Downloads/",
                                "odnoklassniki", "/disk/Social networks/Одноклассники",
                                "applications", "/disk/Applications",
                                "yalivelettersarchive", "/attach/yalivelettersarchive",
                                "facebook", "/disk/Social networks/Facebook",
                                "social", "/disk/Social networks/",
                                "mailru", "/disk/Social networks/Мой Мир",
                                "fotki", "/disk/Yandex.Fotki/",
                                "photostream", "/disk/Camera Uploads/",
                                "yateamnda", "/disk/Yandex Team (NDA)",
                                "yafotki", "/attach/YaFotki",
                                "attach", "/disk/Mail attachments",
                                "scans", "/disk/Scans"
                        ))
                },
                new Object[]{
                        "tr",
                        Cf.toMap(Tuple2List.<String, String>fromPairs(
                                "yaslovariarchive", "/attach/yaslovariarchive",
                                "yaruarchive", "/attach/yaruarchive",
                                "archive", "/attach/archive",
                                "google", "/disk/Sosyal ağlar/Google+",
                                "instagram", "/disk/Sosyal ağlar/Instagram",
                                "vkontakte", "/disk/Sosyal ağlar/VK",
                                "yabooks", "/disk/Yandex.Kitaplar",
                                "screenshots", "/disk/Ekran görüntüleri/",
                                "downloads", "/disk/Downloads/",
                                "odnoklassniki", "/disk/Sosyal ağlar/Одноклассники",
                                "applications", "/disk/Uygulamalar",
                                "yalivelettersarchive", "/attach/yalivelettersarchive",
                                "facebook", "/disk/Sosyal ağlar/Facebook",
                                "social", "/disk/Sosyal ağlar/",
                                "mailru", "/disk/Sosyal ağlar/Мой Мир",
                                "fotki", "/disk/Yandex.Foto/",
                                "photostream", "/disk/Kameradan yüklenenler/",
                                "yateamnda", "/disk/Yandex Team (NDA)",
                                "yafotki", "/attach/YaFotki",
                                "attach", "/disk/E-posta ekleri",
                                "scans", "/disk/Tarananlar"
                        ))
                },
                new Object[]{
                        "uk",
                        Cf.toMap(Tuple2List.<String, String>fromPairs(
                                "yaslovariarchive", "/attach/yaslovariarchive",
                                "yaruarchive", "/attach/yaruarchive",
                                "archive", "/attach/archive",
                                "google", "/disk/Соціальні мережі/Google+",
                                "instagram", "/disk/Соціальні мережі/Instagram",
                                "vkontakte", "/disk/Соціальні мережі/ВКонтакте",
                                "yabooks", "/disk/Яндекс.Книжки",
                                "screenshots", "/disk/Скриншоти/",
                                "downloads", "/disk/Завантаження/",
                                "odnoklassniki", "/disk/Соціальні мережі/Одноклассники",
                                "applications", "/disk/Додатки",
                                "yalivelettersarchive", "/attach/yalivelettersarchive",
                                "facebook", "/disk/Соціальні мережі/Facebook",
                                "social", "/disk/Соціальні мережі/",
                                "mailru", "/disk/Соціальні мережі/Мой Мир",
                                "fotki", "/disk/Яндекс.Фотки/",
                                "photostream", "/disk/Фотокамера/",
                                "yateamnda", "/disk/Yandex Team (NDA)",
                                "yafotki", "/attach/YaFotki",
                                "attach", "/disk/Поштові вкладення",
                                "scans", "/disk/Скани"
                        ))
                },
                new Object[]{
                        "ua",
                        //ua не поддерживается, так что выдаётся русская локаль
                        Cf.toMap(Tuple2List.<String, String>fromPairs(
                                "yaslovariarchive", "/attach/yaslovariarchive",
                                "yaruarchive", "/attach/yaruarchive",
                                "archive", "/attach/archive",
                                "google", "/disk/Социальные сети/Google+",
                                "instagram", "/disk/Социальные сети/Instagram",
                                "vkontakte", "/disk/Социальные сети/ВКонтакте",
                                "yabooks", "/disk/Яндекс.Книги",
                                "screenshots", "/disk/Скриншоты/",
                                "downloads", "/disk/Загрузки/",
                                "odnoklassniki", "/disk/Социальные сети/Одноклассники",
                                "applications", "/disk/Приложения",
                                "yalivelettersarchive", "/attach/yalivelettersarchive",
                                "facebook", "/disk/Социальные сети/Facebook",
                                "social", "/disk/Социальные сети/",
                                "mailru", "/disk/Социальные сети/Мой Мир",
                                "fotki", "/disk/Яндекс.Фотки/",
                                "photostream", "/disk/Фотокамера/",
                                "yateamnda", "/disk/Yandex Team (NDA)",
                                "yafotki", "/attach/YaFotki",
                                "attach", "/disk/Почтовые вложения",
                                "scans", "/disk/Сканы"
                        ))
                },
        };
    }

    @Parameterized.Parameter
    public String locale;

    @Parameterized.Parameter(1)
    public MapF<String, String> folders;

    @Test
    public void testDefaultFoldersWithoutFilteringNotExisted() throws JsonProcessingException {
        DjfsUid uid = DjfsUid.cons(123123123L);
        initializePgUser(uid, PG_SHARD_1, b -> b.locale(UserLocale.R_BY_VALUE.valueOfO(locale)));

        JsonStringResult result = legacyFilesystemActions.default_folders(uid.asString(), Option.empty());
        Assert.equals(folders, JsonTestUtils.parseJsonToMap(result.getResult().getBytes()));
    }
}
