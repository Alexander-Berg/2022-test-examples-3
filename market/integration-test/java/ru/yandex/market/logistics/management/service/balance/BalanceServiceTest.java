package ru.yandex.market.logistics.management.service.balance;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.balance.ClientInfo;
import ru.yandex.market.logistics.management.domain.balance.OfferInfo;
import ru.yandex.market.logistics.management.domain.balance.PersonInfo;
import ru.yandex.market.logistics.management.exception.BalanceException;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.CLIENT_ID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.OPERATOR_UID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.PERSON_ID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.USER_UID;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.client;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.clientRequest;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.clientResponse;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.clientSearchParams;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createOfferInfo;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createOfferResponse;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createOfferStructure;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createPersonRequest;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createdPerson;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.linkIntegrationToClientInfo;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.linkIntegrationToClientStructure;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.offerInfo;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.person;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.personResponse;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.searchParamsRequest;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.updatePersonRequest;

class BalanceServiceTest extends AbstractContextualTest {
    @Autowired
    private BalanceService balanceService;

    @Autowired
    private Balance2 balance2;

    @Test
    @DisplayName("Успешное создание клиента")
    void callCreateClient() throws XmlRpcException {
        when(balance2.CreateClient(OPERATOR_UID.toString(), clientRequest()))
            .thenReturn(new Object[]{0, "", CLIENT_ID});
        int id = balanceService.createClient(OPERATOR_UID, client(null));
        softly.assertThat(id).isEqualTo(CLIENT_ID);
    }

    @Test
    @DisplayName("Ошибка создания клиента")
    void callCreateClientError() throws XmlRpcException {
        when(balance2.CreateClient(OPERATOR_UID.toString(), clientRequest()))
            .thenReturn(new Object[]{1, "Balance CreateClient error", CLIENT_ID});
        softly.assertThatThrownBy(
            () -> balanceService.createClient(OPERATOR_UID, client(null))
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("Error returned from Balance: (1) Balance CreateClient error");
    }

    @Test
    @DisplayName("Успешный поиск клиента")
    void callSearchClient() throws XmlRpcException {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{0, "", new Object[]{clientResponse()}});
        List<ClientInfo> result = balanceService.searchClients(clientSearchParams());
        softly.assertThat(result).isEqualTo(List.of(client(CLIENT_ID)));
    }

    @Test
    @DisplayName("Ошибка поиска клиента")
    void callSearchClientError() throws XmlRpcException {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{1, "Balance FindClient error", new Object[]{}});
        softly.assertThatThrownBy(
            () -> balanceService.searchClients(clientSearchParams())
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("Error returned from Balance: (1) Balance FindClient error");

    }

    @Test
    @DisplayName("Успешное создание плательщика")
    void callCreatePerson() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID.toString(), createPersonRequest())).thenReturn(PERSON_ID);
        int id = balanceService.createOrUpdatePerson(OPERATOR_UID, createdPerson());
        softly.assertThat(id).isEqualTo(PERSON_ID);
    }

    @Test
    @DisplayName("Успешное обновление плательщика")
    void callUpdatePerson() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID.toString(), updatePersonRequest())).thenReturn(PERSON_ID);
        int id = balanceService.createOrUpdatePerson(OPERATOR_UID, person());
        softly.assertThat(id).isEqualTo(PERSON_ID);
    }

    @Test
    @DisplayName("Ошибка создания плательщика")
    void callCreatePersonError() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID.toString(), createPersonRequest()))
            .thenThrow(new XmlRpcException("Bank with BIK=044525225 not found in DB"));
        softly.assertThatThrownBy(
            () -> balanceService.createOrUpdatePerson(OPERATOR_UID, createdPerson())
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("org.apache.xmlrpc.XmlRpcException: Bank with BIK=044525225 not found in DB");
    }

    @Test
    @DisplayName("Успешная привязка пользователя к клиенту")
    void callLinkUserToClient() throws XmlRpcException {
        when(balance2.CreateUserClientAssociation(OPERATOR_UID.toString(), CLIENT_ID, USER_UID.toString()))
            .thenReturn(new Object[]{0, ""});
        balanceService.linkUserToClient(OPERATOR_UID, CLIENT_ID, USER_UID);
    }

    @Test
    @DisplayName("Ошибка привязки пользователя к клиенту")
    void callLinkUserToClientError() throws XmlRpcException {
        when(balance2.CreateUserClientAssociation(OPERATOR_UID.toString(), CLIENT_ID, USER_UID.toString()))
            .thenReturn(new Object[]{1, "Balance CreateUserClientAssociation error"});
        softly.assertThatThrownBy(
            () -> balanceService.linkUserToClient(OPERATOR_UID, CLIENT_ID, USER_UID)
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("Error returned from Balance: (1) Balance CreateUserClientAssociation error");
    }

    @Test
    @DisplayName("Успешный поиск плательщиков клиента")
    void callFindClientPersons() throws XmlRpcException {
        when(balance2.GetClientPersons(CLIENT_ID))
            .thenReturn(new Object[]{personResponse()});
        List<PersonInfo> result = balanceService.findClientPersons(CLIENT_ID);
        softly.assertThat(result).isEqualTo(List.of(person()));
    }

    @Test
    @DisplayName("Ошибка поиска плательщиков клиента")
    void callFindClientPersonsError() throws XmlRpcException {
        when(balance2.GetClientPersons(CLIENT_ID))
            .thenThrow(new XmlRpcException("Invalid client"));
        softly.assertThatThrownBy(
            () -> balanceService.findClientPersons(CLIENT_ID)
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("org.apache.xmlrpc.XmlRpcException: Invalid client");
    }

    @Test
    @DisplayName("Успешная привязка конфигурации интеграции к клиенту")
    void callLinkIntegrationToClient() throws XmlRpcException {
        when(balance2.LinkIntegrationToClient(OPERATOR_UID.toString(), linkIntegrationToClientStructure()))
            .thenReturn(new Object[]{0, ""});
        balanceService.linkIntegrationToClient(OPERATOR_UID, linkIntegrationToClientInfo());
    }

    @Test
    @DisplayName("Ошибка привязки конфигурации интеграции к клиенту")
    void callLinkIntegrationToClientLink() throws XmlRpcException {
        when(balance2.LinkIntegrationToClient(OPERATOR_UID.toString(), linkIntegrationToClientStructure()))
            .thenReturn(new Object[]{1, "Balance LinkIntegrationToClient error"});

        softly.assertThatThrownBy(
            () -> balanceService.linkIntegrationToClient(OPERATOR_UID, linkIntegrationToClientInfo())
        )
            .isInstanceOf(BalanceException.class)
            .hasMessage("Error returned from Balance: (1) Balance LinkIntegrationToClient error");
    }

    @Test
    @DisplayName("Успешное создание договора")
    void callCreateOffer() throws XmlRpcException {
        when(balance2.CreateOffer(OPERATOR_UID.toString(), createOfferStructure()))
            .thenReturn(createOfferResponse());
        OfferInfo result = balanceService.createOffer(OPERATOR_UID, createOfferInfo());
        softly.assertThat(result).isEqualTo(offerInfo());
    }

    @Test
    @DisplayName("Успешное создание договора")
    void callCreateOfferError() throws XmlRpcException {
        when(balance2.CreateOffer(OPERATOR_UID.toString(), createOfferStructure()))
            .thenThrow(new XmlRpcException("Balance CreateOffer error"));
        softly.assertThatThrownBy(() -> balanceService.createOffer(OPERATOR_UID, createOfferInfo()))
            .isInstanceOf(BalanceException.class)
            .hasMessage("org.apache.xmlrpc.XmlRpcException: Balance CreateOffer error");
    }
}
