package ru.yandex.market.checkout.checkouter.balance.service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.checkouter.balance.model.BalanceContractResult;
import ru.yandex.market.checkout.checkouter.balance.model.BalancePerson;
import ru.yandex.market.checkout.checkouter.pay.BankDetails;
import ru.yandex.market.checkout.checkouter.pay.ClientCreditInfo;
import ru.yandex.market.checkout.checkouter.pay.JurBankDetails;
import ru.yandex.market.checkout.providers.ReturnProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.util.balance.ResponseVariable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_EMAIL_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_FULL_NAME_ID;
import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.USE_PERSONAL_PHONE_ID;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.FOR_RETURN;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceResponseVariant.WITH_PURPOSE;
import static ru.yandex.market.checkout.util.balance.BalanceMockHelper.BalanceXMLRPCMethod.GetPerson;

/**
 * @author : poluektov
 * date: 24.01.18.
 */

//TODO: Допили меня.
public class ExternalBalance2ServiceTest extends AbstractServicesTestBase {

    @Autowired
    private ExternalBalance2Service externalBalanceService;

    @BeforeEach
    public void init() {
        trustMockConfigurer.balanceHelper().mockWholeBalance();
        checkouterFeatureWriter.writeValue(USE_PERSONAL_PHONE_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_EMAIL_ID, true);
        checkouterFeatureWriter.writeValue(USE_PERSONAL_FULL_NAME_ID, true);
    }

    @AfterEach
    public void tearDown() {
        trustMockConfigurer.balanceHelper().resetAll();
    }

    @Test
    public void findClientTest() {
        externalBalanceService.findClient(123).get(0);
    }

    @Test
    public void updatePaymentTest() {
        externalBalanceService.updatePayment("5a788843795be254d5dacbcd", Date.from(Instant.now()));
    }

    @Test
    public void createUserClientAssociationTest() {
        externalBalanceService.createUserClientAssociation(1, 2, 3);
    }

    @Test
    public void createOfferTest() {
        externalBalanceService.createOffer(80809389, 6306941, 114787485, 111222333);
    }

    @Test
    public void getClientContractsTest() {
        List<BalanceContractResult> result =
                externalBalanceService.getClientContracts(80809389, 1718L, Date.from(Instant.now()), "SPENDABLE");
        assertNotNull(result, "Got balance response");
        assertFalse(result.isEmpty(), "Balance response is not empty");
    }

    @Test
    public void createClientTest() {
        personalMockConfigurer.mockV1MultiTypesRetrieve();
        externalBalanceService.createClient(BuyerProvider.getBuyer());
    }

    @Test
    public void createPersonTest() {
        personalMockConfigurer.mockV1MultiTypesRetrieve();
        final BankDetails bankDetails = ReturnProvider.getDefaultBankDetails();
        //just in case if somebody will set payment purpose for default bank details
        bankDetails.setPaymentPurpose(null);
        externalBalanceService.createPerson(BuyerProvider.getBuyer(), bankDetails, 80809389, null, null, null);
    }

    @Test
    public void createPersonWithPaymentPurpose() {
        personalMockConfigurer.mockV1MultiTypesRetrieve();
        final BankDetails bankDetails = ReturnProvider.getDefaultBankDetails();
        bankDetails.setPaymentPurpose("test payment purpose");
        externalBalanceService.createPerson(BuyerProvider.getBuyer(), bankDetails, 80809389, null, null, null);
    }

    @Test
    public void createPersonWithUserMail() {
        personalMockConfigurer.mockV1MultiTypesRetrieve();
        final BankDetails bankDetails = ReturnProvider.getDefaultBankDetails();
        externalBalanceService.createPerson(BuyerProvider.getBuyer(), bankDetails, 80809389, null,
                "test@ya.ru", "9e92bc743c624f958b8876c7841a653b");
    }

    @Test
    public void createJurPersonTest() {
        ClientCreditInfo clientCreditInfo = new ClientCreditInfo("0512345678", "Том", null, "Ям");
        String paymentPurpose = clientCreditInfo.toPaymentPurpose();
        assertEquals("Возврат денежных средств по договору 0512345678, Ям Том. НДС не облагается.", paymentPurpose,
                "Должны корректно построить назначение, даже с null в фио");
        final JurBankDetails jurBankDetails = JurBankDetails.tinkoffBankDetails(paymentPurpose);

        externalBalanceService.createJurPerson(BuyerProvider.getBuyer(), jurBankDetails, 80809389, null);
    }

    @Test
    public void getPerson() {
        final Long clientId = 1234L;
        final Long personId = 9876L;
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, FOR_RETURN,
                makeParamsForBalanceResponse(clientId, personId));

        final List<BalancePerson> personList = externalBalanceService.getPerson(personId.intValue());
        assertEquals(1, personList.size());
        BalancePerson person = personList.get(0);
        assertEquals(String.valueOf(personId), person.getId());
        assertEquals(String.valueOf(clientId), person.getClientId());
        assertEquals("00000000000000000007", person.getAccount());
        assertEquals("000000007", person.getBik());
        assertEquals("54321", person.getCorraccount());
        assertEquals("Иван", person.getFirstName());
        assertEquals("Васильевич", person.getMiddleName());
        assertEquals("Рюриков", person.getLastName());
        assertEquals("89217654321", person.getPhone());
        assertEquals("Банк России", person.getBank());
        assertEquals("Москва", person.getBankCity());
        assertEquals("2018-03-12 11:48:41", person.getDt());
        assertEquals("ph", person.getType());
        assertEquals("just.tsar@yandex.ru", person.getEmail());
        assertEquals("6449013711", person.getInn());
        assertNull(person.getPaymentPurpose());
    }

    @Test
    public void getPersonWithPaymentPurpose() {
        final Long clientId = 8523L;
        final Long personId = 1024L;
        trustMockConfigurer.balanceHelper().mockBalanceXMLRPCMethod(GetPerson, WITH_PURPOSE,
                makeParamsForBalanceResponse(clientId, personId));

        final List<BalancePerson> personList = externalBalanceService.getPerson(personId.intValue());
        assertEquals(1, personList.size());
        final BalancePerson person = personList.get(0);
        assertEquals(String.valueOf(personId), person.getId());
        assertEquals(String.valueOf(clientId), person.getClientId());
        assertEquals("00000000000000000008", person.getAccount());
        assertEquals("000000008", person.getBik());
        assertEquals("54321", person.getCorraccount());
        assertEquals("Иван", person.getFirstName());
        assertEquals("Васильевич", person.getMiddleName());
        assertEquals("Рюриков", person.getLastName());
        assertEquals("89217654321", person.getPhone());
        assertEquals("Банк России", person.getBank());
        assertEquals("Москва", person.getBankCity());
        assertEquals("2018-03-12 11:48:41", person.getDt());
        assertEquals("ph", person.getType());
        assertEquals("just.tsar@yandex.ru", person.getEmail());
        assertEquals("6449013711", person.getInn());
        assertEquals("Хочу денег", person.getPaymentPurpose());
    }

    @Nonnull
    private Map<ResponseVariable, Object> makeParamsForBalanceResponse(Long clientId, Long
            personId) {
        Map<ResponseVariable, Object> res = new HashMap<>();
        res.put(ResponseVariable.CLIENT_ID, clientId);
        res.put(ResponseVariable.PERSON_ID, personId);
        return res;
    }
}
