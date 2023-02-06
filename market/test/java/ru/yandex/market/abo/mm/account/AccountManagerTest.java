package ru.yandex.market.abo.mm.account;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.framework.message.MessageService;
import ru.yandex.common.framework.message.MessageTemplate;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.db.DbMailService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.AccountStatus;
import ru.yandex.market.abo.mm.model.AccountType;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 06/12/18.
 */
class AccountManagerTest {
    private static final int ACC_ID = 1;

    @InjectMocks
    AccountManager accountManager;

    @Mock
    DbMailService dbMailService;
    @Mock
    DbMailAccountService accountService;
    @Mock
    MessageService messageService;

    @Mock
    MessageTemplate template;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(accountService.loadAccountsByStatus(any())).thenReturn(
                singletonList(new Account(ACC_ID, "foo@bar", new Date(), AccountType.REGULAR)));
        when(messageService.createMessageTemplate(anyInt(), any(Object[].class))).thenReturn(template);
    }

    @Test
    void processAccounts_hasMsg() {
        processAccounts(true);
    }

    @Test
    void processAccounts_noMsg() {
        processAccounts(false);
    }

    private void processAccounts(boolean hasMsg) {
        AccountStatus status = AccountStatus.CHECK;
        when(dbMailService.hasTestMessageForAccount(any(), any(), any(), any())).thenReturn(hasMsg);

        accountManager.processAccounts(status);

        verify(accountService, times(hasMsg ? 1 : 0)).setAccountStatus(ACC_ID, AccountStatus.ACTIVE);
        verify(messageService, times(hasMsg ? 1 : 0)).sendMessageTemplate(any());
        verify(accountService).processCheckAccountsWithError(status);
    }
}
