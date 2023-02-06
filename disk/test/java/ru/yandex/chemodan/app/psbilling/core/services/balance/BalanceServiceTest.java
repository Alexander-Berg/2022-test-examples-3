package ru.yandex.chemodan.app.psbilling.core.services.balance;

import java.math.BigDecimal;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.balance.PaymentData;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.balanceclient.model.request.FindClientRequest;
import ru.yandex.chemodan.balanceclient.model.response.FindClientResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientPersonsResponseItem;
import ru.yandex.chemodan.util.test.HttpRecorderRule;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

public class BalanceServiceTest extends AbstractPsBillingCoreTest {

    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BalanceClientStub balanceClientStub;

    public static final Long TEST_UID = 3000185708L;

//    @Rule
//    @Autowired
//    public final HttpRecorderRule recorderRule = null;


    @Test
    @HttpRecorderRule.IgnoreHeaders({"X-Ya-Service-Ticket"})
    @Ignore
    public void testFindPaymentData() {
        final PassportUid uid = PassportUid.cons(TEST_UID);

        MapF<Long, PaymentData> paymentData = balanceService.findPaymentData(uid);
        System.out.println(paymentData);
    }

    @Test
    @Ignore
    public void testGetContractsBalance() {
        final PassportUid uid = PassportUid.cons(TEST_UID);
        long clientId = 111932797L;

        System.out.println(balanceService.findPaymentData(clientId));
        Option<GetClientContractsResponseItem> activeContract = balanceService.getActiveContract(clientId);

        System.out.println(balanceService.getContractsBalance(Cf.list(activeContract.get().getId())));
        long paymentRequest = balanceService.createPaymentRequest(
                uid,
                clientId,
                activeContract.get().getId().toString(),
                new BigDecimal("100000000000")
        );
        System.out.println(paymentRequest);
        long invoice = balanceService.createInvoice(uid, activeContract.get(), paymentRequest);
        System.out.println(invoice);
        System.out.println(balanceService.getInvoiceMdsUrl(invoice));
    }

    @Test
    public void findActiveContractPaymentEmail() {
        long clientId = 127000L;
        long personId = 128000L;

        GetClientContractsResponseItem contract = GetClientContractsResponseItem.buildActiveBalanceContract();
        contract.setId(clientId);
        contract.setPersonId(personId);
        contract.setServices(Cf.set(balanceService.getServiceId()));
        balanceClientStub.createOrReplaceClientContract(clientId, contract);

        balanceClientStub.setClientPersons(clientId, Cf.list(personId - 1, personId).map(id -> {
            GetClientPersonsResponseItem person = new GetClientPersonsResponseItem();
            person.setId(id);
            person.setEmail(id + "@persons.com");
            person.setType(GetClientPersonsResponseItem.LEGAL_PERSON_TYPE);
            return person;
        }));
        Assert.some(
                new Email(personId + "@persons.com"),
                balanceService.findActiveContractPaymentEmail(clientId)
        );
    }

    @Test
    public void testFindPaymentDataForUnknownPersons() {
        final PassportUid uid = PassportUid.cons(TEST_UID);
        balanceClientStub.turnOnMockitoForMethod("findClient");
        Mockito.when(balanceClientStub.getBalanceClientMock().findClient(Mockito.any()))
                .thenReturn(Cf.list(createClient(1)));
        long clientId = balanceClientStub.findClient(new FindClientRequest().withUid(uid.getUid())).get(0).getClientId();
        ListF<GetClientPersonsResponseItem> persons = Cf.list(
                createPersonInfo(clientId, 1, GetClientPersonsResponseItem.LEGAL_PERSON_TYPE),
                createPersonInfo(clientId, 2, "sw_yt"),
                createPersonInfo(clientId, 3, "ytph"));
        balanceClientStub.setClientPersons(clientId, persons);

        MapF<Long, PaymentData> paymentData = balanceService.findPaymentData(uid);
        // должно вернуть только тех плательщиков, с которыми умеем работать
        Assert.assertEquals(paymentData.size(), 1);
    }

    private FindClientResponseItem createClient(long clientId) {
        FindClientResponseItem client = new FindClientResponseItem();
        client.setClientId(clientId);
        return client;
    }

    private GetClientPersonsResponseItem createPersonInfo(long clientId, long personId, String type) {
        GetClientPersonsResponseItem person = new GetClientPersonsResponseItem();
        person.setId(personId);
        person.setClientId(clientId);
        person.setType(type);
        return person;
    }
}
