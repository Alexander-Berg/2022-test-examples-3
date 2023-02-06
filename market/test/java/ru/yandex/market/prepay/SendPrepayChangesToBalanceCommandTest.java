package ru.yandex.market.prepay;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.common.balance.xmlrpc.model.PersonStructure;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.balance.BalanceService;
import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.balance.model.FullClientInfo;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class SendPrepayChangesToBalanceCommandTest extends FunctionalTest {

    @Autowired
    private SendPrepayChangesToBalanceCommand sendPrepayChangesToBalanceCommand;

    @Autowired
    private BalanceService patientBalanceService;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @AfterEach
    void afterEach() {
        doCallRealMethod().when(checkouterAPI).shops();
    }

    /**
     * Проверяем корректность принудительной отправки данных предоплатных заявок в Баланс.
     * В тесте 6 заявок. По статусу подходят 3:
     * <ol>
     * <li> Без seller_client_id
     * <li> С seller_client_id и существующим клиентом.
     * <li> С seller_client_id, но в балансе клиента нет.
     * </ol>
     * <p>
     * Результат:
     * <ol>
     * <li> Создание клиента, плательщика, оффера.
     * <li> Обновление клиента, плательщика, оффера.
     * <li> Создание клиента, плательщика, оффера.
     * </ol>
     * А так же отправляем все 3 обновления в чекаутер.
     *
     * @throws Exception если что-то пошло не так
     */
    @Test
    @DbUnitDataSet(before = "executeCommand.before.csv")
    void executeCommandForShops() {
        final CheckouterShopApi spyShops = Mockito.spy(checkouterAPI.shops());

        when(checkouterAPI.shops()).thenReturn(spyShops);

        doReturn(null).when(spyShops).updateShopData(anyLong(), any());

        when(patientBalanceService.getClient(eq(5L))).thenReturn(new ClientInfo());
        sendPrepayChangesToBalanceCommand.executeCommand(
                new CommandInvocation(
                        "send-prepay-changes",
                        new String[0],
                        Collections.singletonMap("campaign-type", "SHOP")
                ),
                new Terminal(new ByteArrayInputStream("yes".getBytes()), new ByteArrayOutputStream()) {
                    @Override
                    protected void onStart() {
                    }

                    @Override
                    protected void onClose() {
                    }
                }
        );

        InOrder inOrder = Mockito.inOrder(patientBalanceService);
        // создаем клиента
        inOrder.verify(patientBalanceService).createClient(any(), anyLong(), anyLong());
        // затем плательщика для одного договора
        inOrder.verify(patientBalanceService).createOrUpdatePerson(
                argThat(
                        anyOf(
                                not(personHasKey("IS_PARTNER")),
                                personHasKeyWithValue("IS_PARTNER", "0")
                        )
                ),
                anyLong()
        );
        // затем контракт
        inOrder.verify(patientBalanceService).createOffer(any(), anyLong());
        // затем плательщика для другого договора
        inOrder.verify(patientBalanceService).createOrUpdatePerson(
                argThat(personHasKeyWithValue("IS_PARTNER", "1")),
                anyLong()
        );
        // затем еще один контракт
        inOrder.verify(patientBalanceService).createOffer(any(), anyLong());

        // Проверяем отправку обновлений в чекаутер
        verify(spyShops, times(1)).updateShopData(anyLong(), any());
    }

    @Test
    @DbUnitDataSet(before = "executeCommand.before.csv")
    void executeCommandForSupplier() {
        final CheckouterShopApi spyShops = Mockito.spy(checkouterAPI.shops());

        when(checkouterAPI.shops()).thenReturn(spyShops);

        doReturn(null).when(spyShops).updateShopData(anyLong(), any());

        when(patientBalanceService.getClient(eq(8L))).thenReturn(new ClientInfo());
        sendPrepayChangesToBalanceCommand.executeCommand(
                new CommandInvocation(
                        "send-prepay-changes",
                        new String[0],
                        Collections.singletonMap("campaign-type", "SUPPLIER")
                ),
                new Terminal(new ByteArrayInputStream("yes".getBytes()), new ByteArrayOutputStream()) {
                    @Override
                    protected void onStart() {
                    }

                    @Override
                    protected void onClose() {
                    }
                }
        );

        InOrder inOrder = Mockito.inOrder(patientBalanceService);
        // для заявки 7 создаем клиента
        inOrder.verify(patientBalanceService).createClient(any(), anyLong(), anyLong());
        // затем плательщика для одного договора
        inOrder.verify(patientBalanceService).createOrUpdatePerson(
                argThat(
                        anyOf(
                                not(personHasKey("IS_PARTNER")),
                                personHasKeyWithValue("IS_PARTNER", "0")
                        )
                ),
                anyLong()
        );
        // затем контракт
        inOrder.verify(patientBalanceService).createOffer(any(), anyLong());
        // затем плательщика для другого договора
        inOrder.verify(patientBalanceService).createOrUpdatePerson(
                argThat(personHasKeyWithValue("IS_PARTNER", "1")),
                anyLong()
        );
        // затем еще один контракт
        inOrder.verify(patientBalanceService).createOffer(any(), anyLong());

        // для заявки 8 обновляем клиента
        inOrder.verify(patientBalanceService).createClient(argThat(clientWithId(8L)), anyLong(), anyLong());
        // затем обновляем обоих плательщиков
        inOrder.verify(patientBalanceService, times(2)).createOrUpdatePerson(
                argThat(personHasKey("PERSON_ID")),
                anyLong()
        );

        // Проверяем 2 отправки обновлений в чекаутер
        verify(spyShops, times(2)).updateShopData(anyLong(), any());
    }

    private static Matcher<PersonStructure> personHasKey(String key) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(PersonStructure item) {
                return item.containsKey(key);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has key [" + key + "]");
            }
        };
    }

    private static Matcher<PersonStructure> personHasKeyWithValue(String key, Object value) {
        return personHasKeyWithValueMatching(key, equalTo(value));
    }

    private static Matcher<PersonStructure> personHasKeyWithValueMatching(String key, Matcher<?> value) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(PersonStructure item) {
                return value.matches(item.get(key));
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has key [" + key + "] with value [" + value + "]");
            }
        };
    }

    private static Matcher<FullClientInfo> clientWithId(long clientId) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(FullClientInfo item) {
                return item.getId() == clientId;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has client id [" + clientId + "]");
            }
        };
    }
}
