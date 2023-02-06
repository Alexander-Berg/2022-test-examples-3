package ru.yandex.market.logistics.nesu.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.nesu.service.offer.OfferService;

class OfferServiceTest extends AbstractContextualTest {
    private static final long SHOP_ID = 1L;
    private static final long NON_EXISTING_SHOP_ID = 2L;

    @Autowired
    private OfferService service;

    @Test
    @DisplayName("Полнотекстовый поиск офферов")
    @DatabaseSetup("/service/offer/before/offers-db.xml")
    void fullTextSearch() {
        softly.assertThat(service.search(SHOP_ID, "fox").size())
            .as("Названия обоих офферов содержат слова, начинающиеся на 'fox'")
            .isEqualTo(2);

        softly.assertThat(service.search(SHOP_ID, "fo").size())
            .as("Названия обоих офферов содержат слова, начинающиеся на 'fo'")
            .isEqualTo(2);

        softly.assertThat(service.search(SHOP_ID, "Ameri").size())
            .as("Название одного оффера содержит слово, начинающееся на 'ameri'")
            .isEqualTo(1);

        softly.assertThat(service.search(SHOP_ID, "foX DOg").size())
            .as("Название одного оффера содержит слова, начинающиеся на 'fox' и 'dog'")
            .isEqualTo(1);

        softly.assertThat(service.search(SHOP_ID, "лиса").size())
            .as("Ни один оффер не содержит слов, начинающихся на 'лиса'")
            .isEqualTo(0);

        softly.assertThat(service.search(SHOP_ID, "'f!o$x").size())
            .as("Названия обоих офферов содержат слова, начинающиеся на 'fox'. Символы ', !, $ фильтруются")
            .isEqualTo(2);

        softly.assertThatThrownBy(() -> service.search(NON_EXISTING_SHOP_ID, "fox"))
            .as("Ошибка при поиске офферов для несуществующего сендера")
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Failed to find [SENDER] with ids [2]");
    }
}
