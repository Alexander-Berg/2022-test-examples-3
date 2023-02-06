package ru.yandex.market.abo.mm.gen;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.mm.model.Message;
import ru.yandex.market.abo.mm.model.MessageUser;
import ru.yandex.market.abo.mm.model.Suggestion;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 * @date 11.11.2008
 */
public class ByShopNamesSuggestionGeneratorTest extends EmptyTest {

    @Autowired
    private ByShopNamesSuggestionGenerator byShopNamesSuggestionGenerator;
    @Autowired
    private SuggestionGeneratorManager suggestionGeneratorManager;
    @Autowired
    private StringIndexer stringIndexer;

    @Test
    public void test() {
        SuggestionContext context = suggestionGeneratorManager.loadSuggestionContext();
        LoweredShopInfo shop = new LoweredShopInfo(1);
        shop.addName("ozon.ru");
        context.getShopInfos().add(shop);

        Message message = new Message(1L);
        message.setFrom(new MessageUser("vanek", "vanek@ozon.ru"));
        message.setBody("Магазин ozon.ru поздравляет вас");
        message.setSubject("Заказ");

        List<Suggestion> suggestions = new ArrayList<>();

        byShopNamesSuggestionGenerator.generate(message, context, suggestions, stringIndexer);

        assertFalse(suggestions.isEmpty());
    }
}
