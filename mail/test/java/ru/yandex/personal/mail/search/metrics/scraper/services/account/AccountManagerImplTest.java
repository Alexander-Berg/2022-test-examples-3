package ru.yandex.personal.mail.search.metrics.scraper.services.account;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.personal.mail.search.metrics.scraper.metrics.MetricsSystemLoaderRegistry;
import ru.yandex.personal.mail.search.metrics.scraper.mocks.InMemoryAccountRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountManagerImplTest {
    private static final String SYS = "sys";
    private static final String ACC = "acc";
    private static final String PART = "credentials";
    private static final AccountProperties PROPS = new AccountProperties();

    @Test
    void initiateAccountCreation() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(false);

        AccountManager manager = new AccountManagerImpl(null, repository);
        manager.initiateCreation(SYS, ACC, PROPS);

        verify(repository).hasAccount(SYS, ACC);
    }

    @Test
    void initiateDoubleAccountCreationException() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(false);

        AccountManager manager = new AccountManagerImpl(null, repository);
        manager.initiateCreation(SYS, ACC, PROPS);

        assertThrows(AccountException.class, () -> manager.initiateCreation(SYS, ACC, PROPS));
        verify(repository).hasAccount(SYS, ACC);
    }

    @Test
    void initiateExistingAccountCreationException() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(true);

        AccountManager manager = new AccountManagerImpl(null, repository);

        assertThrows(AccountException.class, () -> manager.initiateCreation(SYS, ACC, PROPS));
        verify(repository).hasAccount(anyString(), anyString());
    }

    @Test
    void addAccountPart() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(false);

        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getLoader(SYS).validateCredentialName(PART)).thenReturn(true);

        AccountManager manager = new AccountManagerImpl(registry, repository);
        manager.initiateCreation(SYS, ACC, PROPS);
        manager.addCredential(SYS, ACC, PART, new byte[100]);

        verify(registry.getLoader(SYS)).validateCredentialName(PART);
    }

    @Test
    void accAccountPartWrongAccName() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(false);

        AccountManager manager = new AccountManagerImpl(null, repository);

        assertThrows(AccountException.class, () -> manager.addCredential(SYS, ACC, PART, new byte[100]));
    }

    @Test
    void accAccountPartWrongPartName() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenReturn(false);

        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getLoader(SYS).validateCredentialName(PART)).thenReturn(true);

        AccountManager manager = new AccountManagerImpl(registry, repository);
        manager.initiateCreation(SYS, ACC, PROPS);

        assertThrows(AccountException.class, () -> manager.addCredential(SYS, ACC, "", new byte[100]));
    }

    @Test
    void finishAccountInitialization() {
        AccountRepository repository = mock(AccountRepository.class);
        when(repository.hasAccount(SYS, ACC)).thenAnswer((Answer<Boolean>) invocation ->
                Mockito.mockingDetails(invocation.getMock()).getInvocations().size() > 1
        );

        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getLoader(SYS).validateCredentialName(PART)).thenReturn(true);

        AccountManager manager = new AccountManagerImpl(registry, repository);
        createAccount(manager);

        assertTrue(manager.has(SYS, ACC));
    }

    @Test
    void finishWrongAccountInitialization() {
        AccountManager manager = new AccountManagerImpl(null, null);
        assertThrows(AccountException.class, () -> manager.finishCreation(SYS, ACC));
    }

    @Test
    void deleteAccount() {
        AccountRepository accountRepository = new InMemoryAccountRepository();

        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getLoader(SYS).validateCredentialName(PART)).thenReturn(true);

        AccountManager manager = new AccountManagerImpl(registry, accountRepository);
        createAccount(manager);

        manager.delete(SYS, ACC);
        assertFalse(manager.has(SYS, ACC));
    }

    @Test
    void getAccount() {
        AccountConfiguration expected = AccountConfiguration.fromInfo(new AccountInfo(SYS, ACC), null);

        MetricsSystemLoaderRegistry registry = mock(MetricsSystemLoaderRegistry.class, Mockito.RETURNS_DEEP_STUBS);
        when(registry.getLoader(SYS).validateCredentialName(PART)).thenReturn(true);
        AccountRepository repository = new InMemoryAccountRepository();

        AccountManager manager = new AccountManagerImpl(registry, repository);
        createAccount(manager);

        AccountConfiguration result = manager.getConfiguration(SYS, ACC);
        assertEquals(expected, result);
    }

    @Test
    void getNoSuchAccount() {
        AccountRepository repository = new InMemoryAccountRepository();

        AccountManager manager = new AccountManagerImpl(null, repository);
        assertThrows(AccountException.class, () -> manager.getConfiguration(SYS, ACC));
    }

    private void createAccount(AccountManager manager) {
        manager.initiateCreation(SYS, ACC, PROPS);

        manager.addCredential(SYS, ACC, PART, new byte[100]);
        manager.finishCreation(SYS, ACC);
    }
}
