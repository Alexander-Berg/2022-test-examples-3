package ru.yandex.market.core.tanker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.bunker.model.route.RouteContent;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.bunker.dao.BunkerDao;
import ru.yandex.market.core.bunker.dao.CachedBunkerDao;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.core.tanker.model.MessageSet;
import ru.yandex.market.core.tanker.model.TankerCode;
import ru.yandex.market.core.tanker.model.TankerKeySets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TankerServiceTest extends FunctionalTest {
    @Autowired
    private TankerService tankerService;
    @Autowired
    @Qualifier("cachedBunkerDao")
    private BunkerDao<RouteContent> cachedBunkerDao;
    @Autowired
    @Qualifier("bunkerRoutesDao")
    private BunkerDao<RouteContent> bunkerRoutesDao;

    @BeforeEach
    void init() {
        ((CachedBunkerDao<RouteContent>) cachedBunkerDao).cleanUpCache();
    }

    @Test
    @DisplayName("Получение keySet")
    @DbUnitDataSet(before = "TankerServiceTest.before.csv")
    void testGetKeySet() {
        Map<String, MessageSet> keySetMap = tankerService.getMessageSets(
                Set.of("keyset1", "keyset2"), Language.RUSSIAN
        );
        assertThat(keySetMap).hasSize(2);
        assertThat(keySetMap.get("keyset1").getMessages()).hasSize(2);
        assertThat(keySetMap.get("keyset1").getMessage("key11")).isEqualTo("value11");
        assertThat(keySetMap.get("keyset1").getMessage("key12")).isEqualTo("value12");
        assertThat(keySetMap.get("keyset2").getMessages()).hasSize(1);
        assertThat(keySetMap.get("keyset2").getMessage("key21")).isEqualTo("value21");
    }

    @Test
    @DisplayName("merge keySet")
    @DbUnitDataSet(before = "TankerServiceTest.before.csv")
    void testMergeKeySet() {
        MessageSet merged = tankerService.getMergedMessageSets(
                List.of("keyset1", "keyset.merge"), Language.RUSSIAN
        );
        assertThat(merged.getMessages()).hasSize(2);
        assertThat(merged.getMessage("key11")).isEqualTo("value11");
        assertThat(merged.getMessage("key12")).isEqualTo("merged_value");

        Map<String, MessageSet> keySetMap =
                tankerService.getMessageSets(Set.of("keyset1", "keyset.merge"), Language.RUSSIAN);
        merged = tankerService.mergeMessageSets(List.of(keySetMap.get("keyset1"), keySetMap.get("keyset.merge")),
                Language.RUSSIAN);
        assertThat(merged.getMessages()).hasSize(2);
        assertThat(merged.getMessage("key11")).isEqualTo("value11");
        assertThat(merged.getMessage("key12")).isEqualTo("merged_value");
    }

    @Test
    @DisplayName("Перевод по танкеру")
    @DbUnitDataSet(before = "TankerServiceTest.before.csv")
    void testTranslation() {
        Map<String, MessageSet> keySet = tankerService.getMessageSets(
                Sets.newHashSet(
                        TankerKeySets.SHARED_INDEXER_ERROR_CODES,
                        TankerKeySets.SHARED_INDEXER_ERROR_CODES_DETAILS
                ),
                Language.RUSSIAN
        );
        var messageSetErrorCodes = keySet.get(TankerKeySets.SHARED_INDEXER_ERROR_CODES);
        assertThat(tankerService.translateCode(messageSetErrorCodes, TankerCode.ofStringMap("401", null, null)))
                .isEqualTo("Некорректная ссылка на прайс-лист");
        assertThat(tankerService.translateCode(messageSetErrorCodes, TankerCode.ofStringMap("45G", null, Map.of("tagName", "vendor"))))
                .isEqualTo("Нет элемента <vendor>");
        assertThat(tankerService.translateCode(messageSetErrorCodes, TankerCode.ofStringMap("45T", null, null)))
                .isEqualTo("Проверьте и заполните поля согласно этой инструкции (/routes/route/1).");
        assertThat(tankerService.translateCode(messageSetErrorCodes, TankerCode.ofStringMap("45T", null, null)))
                .isEqualTo("Проверьте и заполните поля согласно этой инструкции (/routes/route/1).");


        var messageSetErrorCodesDetails = keySet.get(TankerKeySets.SHARED_INDEXER_ERROR_CODES_DETAILS);
        assertThat(tankerService.translateCode(messageSetErrorCodesDetails, TankerCode.ofStringMap("450", null, Map.of("url", "someUrl"))))
                .isEqualTo("Проверьте ссылку на страницу товара с вашего сайта. Ссылка в вашем предложении: someUrl.");
        assertThat(tankerService.translateCode(messageSetErrorCodesDetails, TankerCode.ofStringMap("450", null, null)))
                .isEqualTo("Проверьте ссылку на страницу товара с вашего сайта.");
        assertThat(tankerService.translateCode(messageSetErrorCodesDetails, TankerCode.ofStringMap("396", null, Map.of("field", "expiry"))))
                .isEqualTo("Заполните обязательное поле – «Срок годности»");

        verify(bunkerRoutesDao, times(1)).getDataFromDb("external");
        verify(cachedBunkerDao, times(2)).getDataFromDb("external");
        verifyNoMoreInteractions(bunkerRoutesDao);
    }

    @Test
    void testMissingKeySetCallback() {
        Map<String, MessageSet> emptyKeySet = tankerService.getMessageSets(Set.of(), Language.RUSSIAN);
        Consumer<TankerCode> callback = mock(Consumer.class);
        tankerService.translateCode(emptyKeySet.get(TankerKeySets.SHARED_HIDDEN_OFFERS_DETAILS),
                TankerCode.ofStringMap("someCode", "No text", Map.of()),
                callback, null);
        verify(callback, times(1)).accept(any());
    }


    @Test
    void testMissingTextCallback() {
        Map<String, MessageSet> emptyKeySet = tankerService.getMessageSets(
                Set.of(TankerKeySets.SHARED_HIDDEN_OFFERS_DETAILS), Language.RUSSIAN);
        Consumer<TankerCode> callback = mock(Consumer.class);
        tankerService.translateCode(emptyKeySet.get(TankerKeySets.SHARED_HIDDEN_OFFERS_DETAILS),
                TankerCode.ofStringMap("someCodeThatIsNotPresent", "No text", Map.of()),
                callback, null);
        verify(callback, times(1)).accept(any());
    }
}
