package ru.yandex.market.survey;

import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.bunker.loader.BunkerLoader;

/**
 * Тесты для {@link ImportSurveyService}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class ImportSurveyServiceTest extends FunctionalTest {

    @Autowired
    private BunkerLoader bunkerLoader;

    @Autowired
    private ImportSurveyService importSurveyService;

    @Test
    @DisplayName("Загрузка нового опроса по sql")
    @DbUnitDataSet(before = "csv/ImportSurveyServiceTest.testNewBannerBySql.before.csv", after = "csv/ImportSurveyServiceTest.testNewBannerBySql.after.csv")
    void testNewSurveyBySql() {
        invoke("testNewSurveyBySql.json");
    }

    @Test
    @DisplayName("Загрузка нового опроса по списку")
    @DbUnitDataSet(before = "csv/ImportSurveyServiceTest.testNewBannerByWhitelist.before.csv", after = "csv/ImportSurveyServiceTest.testNewBannerByWhitelist.after.csv")
    void testNewSurveyByWhitelist() {
        invoke("testNewSurveyByWhitelist.json");
    }

    @Test
    @DisplayName("Загрузка опроса, который уже был в базе с историей")
    @DbUnitDataSet(before = "csv/ImportSurveyServiceTest.testUpdateSurvey.before.csv", after = "csv/ImportSurveyServiceTest.testUpdateSurvey.after.csv")
    void testUpdateSurvey() {
        invoke("testUpdateSurvey.json");
    }

    @Test
    @DisplayName("Загрузка нескольких опросов, удаление старого")
    @DbUnitDataSet(before = "csv/ImportSurveyServiceTest.testMultipleSurvey.before.csv", after = "csv/ImportSurveyServiceTest.testMultipleSurvey.after.csv")
    void testMultipleSurvey() {
        invoke("testMultipleSurvey.json");
    }

    @Test
    @DisplayName("Загрузка нескольких опросов, один из опросов битый")
    @DbUnitDataSet(before = "csv/ImportSurveyServiceTest.testInvalidSurvey.before.csv", after = "csv/ImportSurveyServiceTest.testInvalidSurvey.after.csv")
    void testInvalidSurvey() {
        Assertions.assertThrows(
                Exception.class,
                () -> invoke("testInvalidSurvey.json")
        );
    }

    private void invoke(final String responseFile) {
        try {
            final InputStream bunkerResponse = ImportSurveyServiceTest.class.getResourceAsStream("bunker/" + responseFile);
            Mockito.when(bunkerLoader.getNodeStream(Mockito.anyString(), Mockito.anyString())).thenReturn(bunkerResponse);
            importSurveyService.importSurveys();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
