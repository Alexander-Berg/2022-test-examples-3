package ru.yandex.market.core.balance;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.balance.impl.JdbcOverdraftPaymentsRepository;

/**
 * @author zoom
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultOverdraftServiceTest {

    @InjectMocks
    DefaultOverdraftService service;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    OverdraftRepository repository;

    @Mock
    JdbcOverdraftPaymentsRepository paymentsRepository;

    @Test
    public void shouldReturnExistingOverdraft() {
        Optional<ClientOverdraft> result =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        Mockito.doReturn(result)
                .when(repository).get(Mockito.eq(1L));
        Assert.assertEquals(result, service.get(1));
    }

    @Test
    public void shouldCreateNewRecordWhenAbsent() {
        Optional<ClientOverdraft> emptyOverdraft = Optional.empty();
        Optional<ClientOverdraft> result =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        Mockito.doReturn(emptyOverdraft)
                .when(repository).get(Mockito.eq(1L));
        Mockito.doNothing().when(repository).create(Mockito.any());
        service.store(result.get());
        Mockito.verify(repository).create(Mockito.eq(result.get()));
    }

    @Test
    public void shouldUpdateRecordWhenExists() {
        Optional<ClientOverdraft> existingOverdraft =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        ClientOverdraft newOverdraft =
                new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 33);
        Mockito.doReturn(existingOverdraft)
                .when(repository).get(Mockito.eq(1L));
        Mockito.doNothing().when(repository).update(Mockito.any());
        service.store(newOverdraft);
        Mockito.verify(repository).update(Mockito.eq(newOverdraft));
    }

    @Test
    public void shouldNotUpdateExistingOverdraftWhenUpdateIdIsEqualThanExisting() {
        Optional<ClientOverdraft> result =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        Mockito.doReturn(result)
                .when(repository).get(Mockito.eq(1L));
        service.store(result.get());
    }

    @Test
    public void shouldNotUpdateExistingOverdraftWhenUpdateIdIsLessThanExisting() {
        Optional<ClientOverdraft> existing =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        ClientOverdraft beingStored =
                new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 3);
        Mockito.doReturn(existing)
                .when(repository).get(Mockito.eq(1L));
        service.store(beingStored);
    }

    // Тесты для проверки логики добавления платежа клиента.
    // Платеж добавляется, когда старое значение spent для clientId существует и больше нового.

    @Test
    public void shouldInsertNewOverdraftPayment() {
        Optional<ClientOverdraft> existingOverdraft =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        ClientOverdraft newOverdraft =
                new ClientOverdraft(1, 10, 5, LocalDate.of(2013, 2, 4), 33);
        Mockito.doReturn(existingOverdraft)
                .when(repository).get(Mockito.eq(1L));
        Mockito.doNothing().when(repository).update(Mockito.any());
        service.store(newOverdraft);
        Mockito.verify(paymentsRepository).create(1, 4);
    }

    @Test
    public void shouldNotInsertNewOverdraftPaymentWhenThereIsNoExistingSpentValue() {
        Optional<ClientOverdraft> existingOverdraft =
                Optional.empty();
        ClientOverdraft newOverdraft =
                new ClientOverdraft(1, 10, 5, LocalDate.of(2013, 2, 4), 33);
        Mockito.doReturn(existingOverdraft)
                .when(repository).get(Mockito.eq(1L));
        service.store(newOverdraft);
        Mockito.verify(paymentsRepository, Mockito.never()).create(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void shouldNotInsertNewOverdraftPaymentWhenSpentValueIncreased() {
        Optional<ClientOverdraft> existingOverdraft =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        ClientOverdraft newOverdraft =
                new ClientOverdraft(1, 10, 10, LocalDate.of(2013, 2, 4), 33);
        Mockito.doReturn(existingOverdraft)
                .when(repository).get(Mockito.eq(1L));
        Mockito.doNothing().when(repository).update(Mockito.any());
        service.store(newOverdraft);
        Mockito.verify(paymentsRepository, Mockito.never()).create(Mockito.anyLong(), Mockito.anyLong());
    }

    @Test
    public void shouldNotInsertNewOverdraftPaymentWhenSpentValueIsTheSame() {
        Optional<ClientOverdraft> existingOverdraft =
                Optional.of(new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 32));
        ClientOverdraft newOverdraft =
                new ClientOverdraft(1, 10, 9, LocalDate.of(2013, 2, 4), 33);
        Mockito.doReturn(existingOverdraft)
                .when(repository).get(Mockito.eq(1L));
        Mockito.doNothing().when(repository).update(Mockito.any());
        service.store(newOverdraft);
        Mockito.verify(paymentsRepository, Mockito.never()).create(Mockito.anyLong(), Mockito.anyLong());
    }
}
