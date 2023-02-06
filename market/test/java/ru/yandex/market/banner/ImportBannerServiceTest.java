package ru.yandex.market.banner;

import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.bunker.loader.BunkerLoader;

/**
 * Тесты для {@link ImportBannerService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportBannerServiceTest extends FunctionalTest {

    @Autowired
    private BunkerLoader bunkerLoader;

    @Autowired
    private ImportBannerService importBannerService;

    @Test
    @DisplayName("Загрузка нового баннера по sql")
    @DbUnitDataSet(before = "csv/ImportBannerServiceTest.testNewBannerBySql.before.csv", after = "csv/ImportBannerServiceTest.testNewBannerBySql.after.csv")
    void testNewBannerBySql() {
        invoke("testNewBannerBySql.json");
    }

    @Test
    @DisplayName("Загрузка нового баннера по списку")
    @DbUnitDataSet(after = "csv/ImportBannerServiceTest.testNewBannerBySql.after.csv")
    void testNewBannerByWhitelist() {
        invoke("testNewBannerByWhitelist.json");
    }

    @Test
    @DisplayName("Загрузка баннера, который уже был в базе")
    @DbUnitDataSet(before = "csv/ImportBannerServiceTest.testUpdateBanner.before.csv", after = "csv/ImportBannerServiceTest.testUpdateBanner.after.csv")
    void testUpdateBanner() {
        invoke("testUpdateBanner.json");
    }

    @Test
    @DisplayName("Удаление баннера, которого больше нет в бункере")
    @DbUnitDataSet(before = "csv/ImportBannerServiceTest.testDeleteBanner.before.csv", after = "csv/ImportBannerServiceTest.testDeleteBanner.after.csv")
    void testDeleteBanner() {
        invoke("testDeleteBanner.json");
    }

    @Test
    @DisplayName("Убер баннер и обычный баннер. Партнеры пересекаются. Убер должен быть в приоритете")
    @DbUnitDataSet(before = "csv/ImportBannerServiceTest.testUberBanner.before.csv", after = "csv/ImportBannerServiceTest.testUberBanner.after.csv")
    void testUberBanner() {
        invoke("testUberBanner.json");
    }

    private void invoke(final String responseFile) {
        try {
            final InputStream bunkerResponse = ImportBannerServiceTest.class.getResourceAsStream("bunker/" + responseFile);
            Mockito.when(bunkerLoader.getNodeStream(Mockito.anyString(), Mockito.anyString())).thenReturn(bunkerResponse);
            importBannerService.importBanners();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
