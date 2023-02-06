package ru.yandex.market.deepmind.common.services.tanker;

import java.util.Map;

import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.tanker.client.TankerClient;
import ru.yandex.market.tanker.client.TranslationClient;
import ru.yandex.market.tanker.client.model.KeySet;
import ru.yandex.market.tanker.client.model.KeySetTranslation;
import ru.yandex.market.tanker.client.model.Language;
import ru.yandex.market.tanker.client.request.TranslationRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author kravchenko-aa
 * @date 03.03.2020
 */
public class TankerServiceTest {
    private static final String KEY_SET_1 = "shared.hidden-offers.subreasons";
    private static final String KEY_SET_2 = "shared.indexer.error.codes";
    private TankerService tankerService;
    private TranslationClient translationClient;

    @Before
    public void setUp() {
        TankerClient client = Mockito.mock(TankerClient.class);
        translationClient = Mockito.mock(TranslationClient.class);
        when(client.translations()).thenReturn(translationClient);
        tankerService = new TankerServiceImpl(client);
    }

    @Test
    public void testUnfoldLinks() {
        KeySet keySet1 = new KeySet(Map.of(
            "ABO_FAULTY", "Брак",
            "ABO_LEGAL", "Товар скрыт по запросу правообладателя",
            "FEED_ERROR_25g", "{{#_.externalKey}}shared.indexer.error.codes:25g{{/_.externalKey}}",
            "FEED_ERROR_26a", "{{#_.externalKey}}shared.indexer.error.codes:26a{{/_.externalKey}}"
        ));
        KeySet keySet2 = new KeySet(Map.of(
            "25g", "Вы скрыли товар в прайс-листе",
            "26a", "Не указана цена товара",
            "311", "На Маркете возникла проблема",
            "312", "На Маркете возникла проблема"
        ));

        // не очень классно, но не придумал как лучше замокать
        when(translationClient.keySet(any()))
            .thenAnswer(invocation -> {
                String query = invocation.<TranslationRequestBuilder>getArgument(0).toString();
                if (query.contains(KEY_SET_1)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet1));
                }
                if (query.contains(KEY_SET_2)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet2));
                }
                return null;
            });

        assertThat(tankerService.getKeys(KEY_SET_1))
            .containsExactly(
                Map.entry("FEED_ERROR_26a", "Не указана цена товара"),
                Map.entry("FEED_ERROR_25g", "Вы скрыли товар в прайс-листе"),
                Map.entry("ABO_LEGAL", "Товар скрыт по запросу правообладателя"),
                Map.entry("ABO_FAULTY", "Брак")
            );
    }

    @Test
    public void testUnfoldLinksOnCurrentKey() {
        KeySet keySet1 = new KeySet(Map.of(
            "ABO_FAULTY", "Брак",
            "ABO_LEGAL", "Товар скрыт по запросу правообладателя",
            "FEED_ERROR_25g", "{{#_.externalKey}}shared.indexer.error.codes:25g{{/_.externalKey}}",
            "FEED_ERROR_26a", "{{#_.externalKey}}shared.indexer.error.codes:26a{{/_.externalKey}}"
        ));
        KeySet keySet2 = new KeySet(Map.of(
            "25g", "Вы скрыли товар в прайс-листе",
            "26a", "Не указана цена товара",
            "311", "{{#_.externalKey}}shared.indexer.error.codes:312{{/_.externalKey}}",
            "312", "На Маркете возникла проблема"
        ));

        // не очень классно, но не придумал как лучше замокать
        when(translationClient.keySet(any()))
            .thenAnswer(invocation -> {
                String query = invocation.<TranslationRequestBuilder>getArgument(0).toString();
                if (query.contains(KEY_SET_1)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet1));
                }
                if (query.contains(KEY_SET_2)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet2));
                }
                return null;
            });

        assertThat(tankerService.getKeys(KEY_SET_1))
            .containsExactly(
                Map.entry("FEED_ERROR_26a", "Не указана цена товара"),
                Map.entry("FEED_ERROR_25g", "Вы скрыли товар в прайс-листе"),
                Map.entry("ABO_LEGAL", "Товар скрыт по запросу правообладателя"),
                Map.entry("ABO_FAULTY", "Брак")
            );
    }

    @Test
    public void testUnfoldLinksWithSeveralIterations() {
        KeySet keySet1 = new KeySet(Map.of(
            "ABO_FAULTY", "Брак",
            "ABO_LEGAL", "Товар скрыт по запросу правообладателя",
            "FEED_ERROR_25g", "{{#_.externalKey}}shared.indexer.error.codes:25g{{/_.externalKey}}",
            "FEED_ERROR_26a", "{{#_.externalKey}}shared.indexer.error.codes:26a{{/_.externalKey}}"
        ));
        KeySet keySet2 = new KeySet(Map.of(
            "25g", "Вы скрыли товар в прайс-листе",
            "26a", "Не указана цена товара",
            "311", "{{#_.externalKey}}shared.hidden-offers.subreasons:FEED_ERROR_25g{{/_.externalKey}}",
            "312", "На Маркете возникла проблема"
        ));

        // не очень классно, но не придумал как лучше замокать
        when(translationClient.keySet(any()))
            .thenAnswer(invocation -> {
                String query = invocation.<TranslationRequestBuilder>getArgument(0).toString();
                if (query.contains(KEY_SET_1)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet1));
                }
                if (query.contains(KEY_SET_2)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet2));
                }
                return null;
            });

        assertThat(tankerService.getKeys(KEY_SET_1))
            .containsExactly(
                Map.entry("FEED_ERROR_26a", "Не указана цена товара"),
                Map.entry("FEED_ERROR_25g", "Вы скрыли товар в прайс-листе"),
                Map.entry("ABO_LEGAL", "Товар скрыт по запросу правообладателя"),
                Map.entry("ABO_FAULTY", "Брак")
            );
    }

    @Test
    public void testReplacePlatformSpecificKeys() {
        KeySet keySet1 = new KeySet(Map.ofEntries(
            MapEntry.entry("ABO_FAULTY", "Брак"),
            MapEntry.entry("FEED_ERROR_42", "Этот ключ выкидываем так как есть специализированный синий"),
            MapEntry.entry("FEED_ERROR_42@@SUPPLIER", "Берем @@SUPPLIER он приоритенее ключа без типа"),
            MapEntry.entry("FEED_ERROR_42@@SUPPLIER@@DROPSHIP", "Есть более общий ключ"),
            MapEntry.entry("FEED_ERROR_49@@SUPPLIER", "Берем @@SUPPLIER, только он и есть"),
            MapEntry.entry("FEED_ERROR_49@@SUPPLIER@@DROPSHIP", "Есть более общий ключ"),
            MapEntry.entry("FEED_ERROR_142@@SUPPLIER",
                "Берем @@SUPPLIER, он приоритенее частного ключа по типу интеграции"),
            MapEntry.entry("FEED_ERROR_2342@@SUPPLIER@@DROPSHIP",
                "Берем первый увиденный перевод в случае, когда есть много частных"),
            MapEntry.entry("FEED_ERROR_2342@@SUPPLIER@@CROSSDOCK", "Уже взяли перевод для dropship"),
            MapEntry.entry("TEST@@SHOP", "Выкидываем"),
            MapEntry.entry("TEST@@SHOP@@SMB", "Выкидываем"),
            MapEntry.entry("TEST@@FMCG", "Выкидываем"),
            MapEntry.entry("TEST@@CROSSBORDER", "Выкидываем")
        ));

        when(translationClient.keySet(any()))
            .thenAnswer(invocation -> {
                String query = invocation.<TranslationRequestBuilder>getArgument(0).toString();
                if (query.contains(KEY_SET_1)) {
                    return new KeySetTranslation(Map.of(Language.RU, keySet1));
                }
                return null;
            });

        assertThat(tankerService.getKeys(KEY_SET_1))
            .containsAllEntriesOf(
                Map.ofEntries(
                    Map.entry("FEED_ERROR_42", "Берем @@SUPPLIER он приоритенее ключа без типа"),
                    Map.entry("FEED_ERROR_49", "Берем @@SUPPLIER, только он и есть"),
                    Map.entry("FEED_ERROR_142", "Берем @@SUPPLIER, он приоритенее частного ключа по типу интеграции"),
                    Map.entry("FEED_ERROR_2342", "Берем первый увиденный перевод в случае, когда есть много частных"),
                    Map.entry("ABO_FAULTY", "Брак")
                ));
    }
}
