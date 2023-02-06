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
 *         @date 12.11.2008
 */
public class ByEmailDomainSuggestionGeneratorTest extends EmptyTest {

    @Autowired
    private ByEmailDomainSuggestionGenerator byEmailDomainSuggestionGenerator;
    @Autowired
    private SuggestionGeneratorManager suggestionGeneratorManager;
    @Autowired
    private StringIndexer stringIndexer;

    @Test
    public void domainName() {
        SuggestionContext context = suggestionGeneratorManager.loadSuggestionContext();
        LoweredShopInfo shop = new LoweredShopInfo(1);
        shop.addName("misspelleddomainname.ru");
        context.getShopInfos().add(shop);

        Message message = new Message(1L);
        message.setFrom(new MessageUser("vanek", "vanek@misspeleddomainname.ru"));

        List<Suggestion> suggestions = new ArrayList<>();
        byEmailDomainSuggestionGenerator.generate(message, context, suggestions, stringIndexer);
        assertFalse(suggestions.isEmpty());
    }
}
