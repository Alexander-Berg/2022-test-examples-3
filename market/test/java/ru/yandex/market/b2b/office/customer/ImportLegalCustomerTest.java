package ru.yandex.market.b2b.office.customer;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.fps.balance.BalanceService;
import ru.yandex.market.fps.balance.model.LegalRussiaPerson;
import ru.yandex.market.fps.balance.model.PersonRelationType;
import ru.yandex.market.jmf.bcp.BcpService;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.queue.retry.internal.FastRetryTasksQueue;
import ru.yandex.market.jmf.queue.retry.internal.RetryTaskProcessor;
import ru.yandex.market.jmf.tx.TxService;
import ru.yandex.market.jmf.utils.Maps;

@SpringJUnitConfig(ModuleCustomerTestConfiguration.class)
@Transactional
public class ImportLegalCustomerTest {

    @Inject
    private LegalCustomerService legalCustomerService;

    @Inject
    private BcpService bcpService;
    @Inject
    private TxService txService;
    @Inject
    private BalanceService balanceService;
    @Inject
    private EntityStorageService entityStorageService;
    @Inject
    private RetryTaskProcessor retryTaskProcessor;
    @Inject
    private FastRetryTasksQueue fastRetryTasksQueue;

    @AfterEach
    public void tearDown() {
        Mockito.reset(balanceService);
    }

    @Test
    public void fireEvent_exists() throws Exception {
        long balanceId = 1237L;

        LegalRussiaPerson person = getPersonStructure();
        Mockito.when(balanceService.getClientPersons(balanceId, PersonRelationType.GENERAL))
                .thenReturn(List.of(person));

        // настройка системы
        String title = Randoms.string();
        String inn = Randoms.stringNumber();
        String kpp = Randoms.stringNumber();
        txService.runInNewTx(() -> bcpService.create(LegalCustomer.FQN, Maps.of(
                LegalCustomer.ID, balanceId,
                LegalCustomer.TITLE, title,
                LegalCustomer.BALANCE_ID, balanceId,
                LegalCustomer.INN, inn,
                LegalCustomer.KPP, kpp
        )));

        // вызов системы
        legalCustomerService.importCustomer(balanceId);
        processTasks();

        // проверка утверждений
        Query q = Query.of(LegalCustomer.FQN).withFilters(Filters.eq(LegalCustomer.BALANCE_ID, balanceId));
        List<LegalCustomer> result = txService.doInNewTx(() -> entityStorageService.list(q));

        Assertions.assertEquals(1, result.size());
        LegalCustomer customer = result.get(0);

        // значения атрибутов измениться не должны т.к. к моменту импорта юр. лицо уже существовало
        Assertions.assertEquals(balanceId, customer.getBalanceId());
        Assertions.assertEquals(inn, customer.getInn());
        Assertions.assertEquals(kpp, customer.getKpp());
        Assertions.assertEquals(title, customer.getTitle());
    }

    @Test
    public void fireEvent_notExists() throws Exception {
        long balanceId = 123L;

        LegalRussiaPerson person = getPersonStructure();
        Mockito.when(balanceService.getClientPersons(balanceId, PersonRelationType.GENERAL))
                .thenReturn(List.of(person));

        // вызов системы
        legalCustomerService.importCustomer(balanceId);

        processTasks();

        // проверка утверждений
        Query q = Query.of(LegalCustomer.FQN).withFilters(Filters.eq(LegalCustomer.BALANCE_ID, balanceId));
        List<LegalCustomer> result = txService.doInNewTx(() -> entityStorageService.list(q));

        Assertions.assertEquals(1, result.size());
        LegalCustomer customer = result.get(0);
        Assertions.assertEquals(balanceId, customer.getBalanceId());
        Assertions.assertEquals(person.getInn(), customer.getInn());
        Assertions.assertEquals(person.getKpp(), customer.getKpp());
        Assertions.assertEquals(person.getLongName(), customer.getTitle());
    }

    @NotNull
    private LegalRussiaPerson getPersonStructure() {
        return new LegalRussiaPerson(
                -1, -1, null, Randoms.string(), Phone.empty(), null, null, null, Randoms.stringNumber(),
                Randoms.stringNumber(), null, null, null, null);
    }

    private void processTasks() throws InterruptedException {
        Thread.sleep(1000L);
        retryTaskProcessor.processPendingTasksWithReset(fastRetryTasksQueue);
    }
}
