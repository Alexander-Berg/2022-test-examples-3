package ru.yandex.market.core.language.dao;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.language.model.Language;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PreferableLanguageDaoTest extends FunctionalTest {

    @Autowired
    private MemCachedPreferrableLanguageDao preferableLanguageDao;

    @BeforeEach
    public void cleanCaches() {
        preferableLanguageDao.clean();
    }

    /**
     * Проверка того, что все данные успешно считываются из БД.
     */
    @DbUnitDataSet(before = "PreferableLanguageDaoTest.csv")
    @Test
    void testFindAll_allInformationFound() {
        Map<Long, Language> actualResult = preferableLanguageDao.findPreferableLanguagesMap();

        assertNotNull(actualResult);
        assertThat(actualResult.size(), equalTo(3));
        assertThat(actualResult.get(1l), equalTo(Language.RUSSIAN));
        assertThat(actualResult.get(4l), equalTo(Language.GERMAN));
        assertThat(actualResult.get(6l), equalTo(Language.CHINESE_TRADITIONAL));
        assertThat(actualResult.get(2l), nullValue());
    }
}
