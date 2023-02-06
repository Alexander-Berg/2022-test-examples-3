package ru.yandex.market.abo.mm.db;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.mm.model.Suggestion;
import ru.yandex.market.abo.mm.model.SuggestionEntityType;

/**
 * @author Olga Bolshakova (obolshakova@yandex-team.ru)
 *         @date 12.11.2008
 */
public class DefaultApprovingSuggestionServiceTest extends EmptyTest {

    private static final int GEN_ID = -2;
    private static final int TYPE = 3;
    private static final int SHOP_ID_2 = 155;
    private static final int HYP_ID_2 = 1145690;
    private static final long MESSAGE_ID_2 = 2332L;
    private static final int HYP_ID = 1145880;
    private static final int USER_ID = 111;
    private static final int SHOP_ID = 5530;
    private static final long MESSAGE_ID = 2334L;

    @Autowired
    private DefaultApprovingSuggestionService approvingSuggestionService;
    @Autowired
    private DbMailService dbMailService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void init() {
        // TODO использовать, когда уберем сохранение в Oracle
        /*
        User user = new User("name", "email");
        Message msg = new Message(MESSAGE_ID);
        msg.setFrom(user);
        msg.getToList().add(user);
        dbMailService.storeMessage(msg);
        */
        pgJdbcTemplate.update("insert into mm_message (id) values(?)", MESSAGE_ID);
    }

    @Test
    public void testBindShop() {
        final long startTime = System.currentTimeMillis();
        approvingSuggestionService.bindMessageToShop(MESSAGE_ID, SHOP_ID, USER_ID);
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

    @Test
    public void testBindTicket() {
        final long startTime = System.currentTimeMillis();
        approvingSuggestionService.bindMessageToTicket(MESSAGE_ID, HYP_ID, USER_ID);
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

    @Test
    public void testBindType() {
        final long startTime = System.currentTimeMillis();
        approvingSuggestionService.bindMessageToType(MESSAGE_ID, TYPE, USER_ID);
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

    @Test
    public void testSaveSuggestions() {
        final Suggestion suggestion = new Suggestion(MESSAGE_ID, 3605, SuggestionEntityType.SHOP, 1);
        suggestion.setGenId(GEN_ID);
        suggestion.setApproved(1);
        approvingSuggestionService.saveSuggestions(Collections.singletonList(suggestion), new HashMap<>());
    }

    @Test
    public void testUnbindShop() {
        final long startTime = System.currentTimeMillis();
        approvingSuggestionService.unbindMessageFromShop(MESSAGE_ID_2, SHOP_ID_2, USER_ID);
        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

    @Test
    public void testUnbindTicket() {
        final long startTime = System.currentTimeMillis();
        approvingSuggestionService.unbindMessageFromTicket(MESSAGE_ID_2, HYP_ID_2, USER_ID);

        System.out.println(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime));
    }

}
