package ru.yandex.market.abo.mm.gen;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.mm.db.DbMailService;
import ru.yandex.market.abo.mm.model.Message;
import ru.yandex.market.abo.mm.model.Suggestion;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 11.11.2008
 */
public class ByTicketEmailSuggestionGeneratorTest extends EmptyTest {
    @Autowired
    private ByTicketEmailSuggestionGenerator byTicketEmailSuggestionGenerator;
    @Autowired
    private StringIndexer stringIndexer;
    @Autowired
    private DbMailService dbMailService;
    @Autowired
    private SuggestionGeneratorManager suggestionGeneratorManager;

    private static final long MESSAGE_ID = 2060000004894339202L;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void init() {
        // TODO использовать, когда уберем сохранение в Oracle
        /*
        Message msg = new Message(MESSAGE_ID);
        dbMailService.storeMessage(msg);
        */
        pgJdbcTemplate.update("insert into mm_message (id) values(?)", MESSAGE_ID);
    }

    @Test
    public void test() {
        final SuggestionContext context = suggestionGeneratorManager.loadSuggestionContext();
        final Message message = dbMailService.loadMessage(MESSAGE_ID);

        final List<Suggestion> suggestions = new ArrayList<>();

        byTicketEmailSuggestionGenerator.generate(message, context, suggestions, stringIndexer);

        System.out.println(suggestions);
    }
}
