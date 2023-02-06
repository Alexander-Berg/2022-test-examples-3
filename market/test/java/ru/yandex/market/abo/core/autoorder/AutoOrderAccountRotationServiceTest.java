package ru.yandex.market.abo.core.autoorder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.CoreCounter;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.AccountStatus;
import ru.yandex.market.abo.mm.model.AccountType;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;

/**
 * @author antipov93.
 */
class AutoOrderAccountRotationServiceTest extends EmptyTestWithTransactionTemplate {
    private static final long ACC_ID = 1;
    private static final AccountType ACCOUNT_TYPE = AccountType.AUTO_CORE;

    @InjectMocks
    private AutoOrderAccountRotationService autoOrderAccountRotationService;
    @Mock
    private ConfigurationService aboConfigurationService;
    @Mock
    private DbMailAccountService dbMailAccountService;
    @Mock
    private ConfigurationService coreCounter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(aboConfigurationService.getValueAsInt(CoreConfig.AUTO_ORDER_ACCOUNT_LIFETIME_DAYS.getId())).thenReturn(1);
        when(dbMailAccountService.setAccountStatus(ACC_ID, AccountStatus.DELETED)).thenReturn(true);
    }

    @ParameterizedTest
    @CsvSource({"0, true", "2, false"})
    void testAccountFresh(int minusDays, boolean isActive) {
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(ACC_ID);
        when(dbMailAccountService.loadAccountsByStatus(AccountStatus.ACTIVE))
                .thenReturn(Collections.singletonList(new Account((int) ACC_ID, "foo@bar", new Date(), AccountType.AUTO_CORE)));
        when(coreCounter.getValueAsDate(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_FROM.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(DateUtils.addDays(new Date(), -minusDays));
        assertEquals(isActive, autoOrderAccountRotationService.isActiveAndFresh(ACC_ID ,ACCOUNT_TYPE));
    }

    @Test
    void testNotActive() {
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(ACC_ID);
        when(dbMailAccountService.loadAccountsByStatus(AccountStatus.ACTIVE)).thenReturn(Collections.emptyList());
        assertFalse(autoOrderAccountRotationService.isActiveAndFresh(ACC_ID, ACCOUNT_TYPE));
        verifyNoMoreInteractions(aboConfigurationService);
    }

    @Test
    void loadActiveWithUpdating() {
        long currentAccountId = 1L;
        Long newAccountId = 2L;
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(currentAccountId);
        when(dbMailAccountService.getAccountsForBasketCheck(ACCOUNT_TYPE))
                .thenReturn(Arrays.asList(currentAccountId, newAccountId));

        assertEquals(newAccountId, autoOrderAccountRotationService.rotate(currentAccountId, ACCOUNT_TYPE));
        verify(dbMailAccountService).setAccountStatus(currentAccountId, AccountStatus.DELETED);
        verify(dbMailAccountService, never()).setAccountStatus(newAccountId, AccountStatus.DELETED);
    }

    @Test
    void testAttachAccountToTicket() {
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(ACC_ID);
        autoOrderAccountRotationService.attachAccountToTicket(ACC_ID, ACCOUNT_TYPE);
        verify(dbMailAccountService, atLeastOnce()).storeTicketAccountBinding(1, ACC_ID);
    }

    @Test
    void noCurrentActive() {
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(null);
        when(dbMailAccountService.getAccountsForBasketCheck(ACCOUNT_TYPE)).thenReturn(Collections.singletonList(ACC_ID));
        Long currentActive = autoOrderAccountRotationService.rotateAccount(ACCOUNT_TYPE);
        verify(coreCounter).mergeValue(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name()), ACC_ID);
        verify(coreCounter)
                .mergeValue(eq(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())), anyLong());
        assertEquals((long) currentActive, ACC_ID);
    }

    @Test
    void noAccountsAtAll() {
        when(coreCounter.getValueAsLong(CoreCounter.AUTO_ORDER_ACCOUNT_CURRENT_ACTIVE_ID.asPrefixTo(ACCOUNT_TYPE.name())))
                .thenReturn(null);
        when(dbMailAccountService.getAccountsForBasketCheck(ACCOUNT_TYPE)).thenReturn(Collections.emptyList());
        assertThrows(IllegalStateException.class, () -> autoOrderAccountRotationService.rotateAccount(ACCOUNT_TYPE));
    }
}
