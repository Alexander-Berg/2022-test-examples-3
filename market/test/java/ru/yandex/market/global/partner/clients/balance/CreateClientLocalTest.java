package ru.yandex.market.global.partner.clients.balance;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.common.balance.BalanceService;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.ClientStructure;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.OfferResponse;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.OfferStructure;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.PersonStructure;
import ru.yandex.market.global.partner.BaseLocalTest;
import ru.yandex.market.global.partner.domain.contracts.ContractsCreationService;

import static ru.yandex.market.global.partner.util.FileUtil.readAllLinesFromClassPath;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Disabled
public class CreateClientLocalTest extends BaseLocalTest {

    // ssh -L 8004:balance-xmlrpc-tvm-ts.paysys.yandex.net:8004 root@global-market-partner_box.are3j4jqvvond3tg.sas.yp-c.yandex.net

    private final BalanceService balanceService;

    @Test
    public void testCreateClient() throws XmlRpcException {

        ClientStructure clientStructure = new ClientStructure()
                .setCity("Tel Aviv")
                .setCurrency("ILS")
                .setName("ClientName")
                .setPhone("+79991234567")
                .setRegionId(181);

        long client = balanceService.createClient(clientStructure, 1120000000145872L);
        System.out.println(client);

    }

    @Test
    public void testCreatePerson() throws XmlRpcException {

        PersonStructure person = new PersonStructure()
                .setClientId(1358021735)
                .setName("PersonName")
                .setEmail("gm-support@yandex-team.ru")
                .setPhone("79991234567")
                .setName("ClientName")
                .setPhone("+79991234567")
                .setPostAddress("test test test test")
                .setPostCode("12345678")
                .setRegion(181)
                .setCity("Tel Aviv")
                .setIlId("1234567890")
                .setInn("1234567890")
                .setLegalAddress("test test test test")
                .setType("il_ur")
                .setBenBank("benbank")
                .setIban("IL000000000000000000000")
                .setSwift("TICSRUMM")
                .setIsPartner(false);

        long client = balanceService.createOrUpdatePerson(person, 1120000000145872L);
        System.out.println(client);

        //21233827

    }

    @Test
    public void testCreatePerson2() throws XmlRpcException {

        PersonStructure person = new PersonStructure()
                .setClientId(1358021735)
                .setName("PersonName")
                .setEmail("gm-support@yandex-team.ru")
                .setPhone("79991234567")
                .setName("ClientName")
                .setPhone("+79991234567")
                .setPostAddress("test test test test")
                .setPostCode("12345678")
                .setRegion(181)
                .setCity("Tel Aviv")
                .setIlId("1234567890")
                .setInn("1234567890")
                .setLegalAddress("test test test test")
                .setType("il_ur")
                .setBenBank("benbank")
                .setIban("IL000000000000000000000")
                .setSwift("TICSRUMM")
                .setIsPartner(true);

        long client = balanceService.createOrUpdatePerson(person, 1120000000145872L);
        System.out.println(client);

        //21233818

    }

    @Test
    public void testCreateOffer() throws XmlRpcException {

        OfferStructure offerStructure = new OfferStructure()
                .setClientId(1358021735)
                .setPersonId(21233827)
                .setCurrency("ILS")
                .setNetting(1)
                .setCountry(181)
                .setSigned(1)
                .setFirmId(1097)
                .setStartDt("2022-04-25")
                .setIsraelTaxPct(0.0)
                .setPaymentType(3)
                .setPaymentTerm(15)
                .setServices(List.of(610, 612))
                .setOfferConfirmationType("no")
                .setPersonalAccount(true)
                .setManagerUid(98700241L);

        OfferResponse offer = balanceService.createOffer(offerStructure, 98700241L);
        System.out.println(offer.getId());
        System.out.println(offer.getExternalId());

    }

    @Test
    public void testCreateOffer2() throws XmlRpcException {

        OfferStructure offerStructure = new OfferStructure()
                .setClientId(1358021735)
                .setPersonId(21233818)
                .setCurrency("ILS")
                .setNetting(1)
                .setCountry(181)
                .setSigned(1)
                .setFirmId(1097)
                .setStartDt("2022-04-25")
                .setIsraelTaxPct(0.0)
                .setServices(List.of(609))
                .setManagerUid(98700241L)
                .setNds(17);

        OfferResponse offer = balanceService.createOffer(offerStructure, 98700241L);
        System.out.println(offer.getId());
        System.out.println(offer.getExternalId());

    }

}
