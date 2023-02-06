package ru.yandex.market.abo.mm.account;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.common.framework.message.MessageService;
import ru.yandex.common.framework.message.MessageTemplate;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.db.DbMailService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.AccountStatus;
import ru.yandex.market.abo.mm.model.AccountType;
import ru.yandex.market.abo.mm.model.Message;
import ru.yandex.market.abo.mm.model.MessageUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.mm.account.AccountManager.CHECK_ACCOUNT_MESSAGE_ID;
import static ru.yandex.market.abo.mm.account.AccountManager.NO_PARAMS;

/**
 * @author artemmz
 * @date 06/12/18.
 */
class AccountManagerLifeCycleTest extends EmptyTest {
    private static final long USER_ID = 1L;
    private static final String EMAIL = "foo@bar";
    private static final long DELAY = 100L;
    private static final long ACC_ID = 123L;

    @Autowired
    @InjectMocks
    private AccountManager accountManager;

    @Autowired
    private DbMailAccountService accountService;
    @Autowired
    private DbMailService dbMailService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Mock
    private MessageService messageService;

    private MessageTemplate template;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        template = MessageTemplate.newPlainText(1, "baz@fuzz", "", "subj", "txt", true);
        when(messageService.createMessageTemplate(CHECK_ACCOUNT_MESSAGE_ID, NO_PARAMS)).thenReturn(template);
    }

    /**
     * includes goodLifeCycle & badLifeCycle as single test so that they do not interfere in terms of db.
     */
    @Test
    void lifeCycle() throws InterruptedException {
        goodLifeCycle();
        clearTables();

        badLifeCycle();
        clearTables();

        badLifeCycleForActive();
        clearTables();
    }

    private void clearTables() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(4 * DELAY);
        Stream.of("mm_account", "mm_message")
                .forEach(t -> pgJdbcTemplate.update("truncate " + t + " cascade"));
    }

    /**
     * NEW---[tstMsg]-->CHECK-->[hasMsg]-->ACTIVE--[hasMsg]-->ACTIVE.
     */
    void goodLifeCycle() throws InterruptedException {
        when(messageService.sendMessageTemplate(template)).then(inv -> {
            sendWithDelay();
            return true;
        });
        storeNewAccount();

        accountManager.processNewAccounts();
        checkAccounts(AccountStatus.CHECK);
        TimeUnit.MILLISECONDS.sleep(4 * DELAY);

        accountManager.processCheckAccounts();
        checkAccounts(AccountStatus.ACTIVE);
        TimeUnit.MILLISECONDS.sleep(4 * DELAY);

        accountManager.processActiveAccounts();
        checkAccounts(AccountStatus.ACTIVE);
    }

    /**
     * NEW--[tstMsg]-->CHECK--[3hours, no msg]-->ERROR
     */
    void badLifeCycle() {
        when(messageService.sendMessageTemplate(template)).thenReturn(true);
        storeNewAccount();

        accountManager.processNewAccounts();
        checkAccounts(AccountStatus.CHECK);

        moreThan3HoursPassed();

        accountManager.processCheckAccounts();
        checkAccounts(AccountStatus.ERROR);
    }

    /**
     * ACTIVE--[tstMsg]-->ACTIVE--[3hours, no msg]-->ERROR
     */
    void badLifeCycleForActive() {
        when(messageService.sendMessageTemplate(template)).thenReturn(true);
        storeNewAccount();

        Account account = accountService.loadAccountsByStatus(AccountStatus.NEW).get(0);
        accountService.setAccountStatus(account.getId(), AccountStatus.ACTIVE);
        checkAccounts(AccountStatus.ACTIVE);

        accountManager.processActiveAccounts();
        checkAccounts(AccountStatus.ACTIVE);

        moreThan3HoursPassed();
        accountManager.processActiveAccounts();
        checkAccounts(AccountStatus.ERROR);
    }

    private void storeNewAccount() {
        accountService.storeAccount(EMAIL, USER_ID, AccountType.REGULAR);
        checkAccounts(AccountStatus.NEW);
    }

    private void moreThan3HoursPassed() {
        int updated = pgJdbcTemplate.update("update mm_account set modification_time = " +
                "modification_time - make_interval(hours := 4) where email = ?", EMAIL);
        assertEquals(1, updated);
    }

    private void checkAccounts(AccountStatus status) {
        List<Account> accounts = accountService.loadAccountsByStatus(status);
        assertEquals(1, accounts.size());
        assertEquals(EMAIL, accounts.get(0).getEmail());
    }

    private void sendWithDelay() {
        new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(DELAY);
                dbMailService.store(Collections.singletonList(createMessage()));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @SuppressWarnings("ConstantConditions")
    private Message createMessage() {
        Message msg = new Message(pgJdbcTemplate.queryForObject("select nextval('s_mm_message')", Long.class));
        msg.setSubject(template.getSubject());
        msg.addToUser(new MessageUser("whatever", template.getTo().get(0)));
        msg.setFrom(new MessageUser("don't care again", template.getFrom()));
        msg.setAccountId(ACC_ID);
        return msg;
    }
}
