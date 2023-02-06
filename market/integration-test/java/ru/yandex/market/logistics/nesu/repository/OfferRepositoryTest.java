package ru.yandex.market.logistics.nesu.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

class OfferRepositoryTest extends AbstractContextualTest {
    private static final long SHOP_ID = 1L;
    private static final int LIMIT = 10;

    @Autowired
    private OfferPartitionRepository repository;

    @Test
    @DisplayName("Полнотекстовый поиск офферов")
    @DatabaseSetup("/service/offer/before/offers-db.xml")
    void fullTextSearch() {
        softly.assertThat(repository.search(SHOP_ID, "fox", LIMIT).size())
            .as("Слово содержится в двух офферах")
            .isEqualTo(2);
        softly.assertThat(repository.search(SHOP_ID, "fox & !dog", LIMIT).size())
            .as("Один оффер со словом 'fox' и без слова 'dog'")
            .isEqualTo(1);
        softly.assertThat(repository.search(SHOP_ID, "fOX & dOg", LIMIT).size())
            .as("Один оффер со словами 'fox' и 'dog'")
            .isEqualTo(1);
        softly.assertThat(repository.search(SHOP_ID, "ameri", LIMIT).size())
            .as("Нет офферов, которые содержат слово 'ameri'")
            .isEqualTo(0);
        softly.assertThat(repository.search(SHOP_ID, "лиса", LIMIT).size())
            .as("Нет офферов, содержащих слово 'лиса'")
            .isEqualTo(0);
        softly.assertThatThrownBy(() -> repository.search(SHOP_ID, "'fox", LIMIT))
            .as("Попытка sql-инъекции приводит к ошибке")
            .isInstanceOf(DataAccessException.class);
    }
}
