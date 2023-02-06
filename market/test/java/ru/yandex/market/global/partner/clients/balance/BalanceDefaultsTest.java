package ru.yandex.market.global.partner.clients.balance;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.common.balance.impl.xmlrpc.model.ClientStructure;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.OfferStructure;
import ru.yandex.market.global.common.balance.impl.xmlrpc.model.PersonStructure;
import ru.yandex.market.global.partner.domain.contracts.ContractsCreationService;

import static ru.yandex.market.global.partner.util.FileUtil.readAllLinesFromClassPath;

public class BalanceDefaultsTest {

    private static final String OFFER_OUTCOME_DEFAULTS = readAllLinesFromClassPath(ContractsCreationService.class,
            "/templates/defaults/contracts/offer-outcome-defaults.json");
    private static final String OFFER_INCOME_DEFAULTS = readAllLinesFromClassPath(ContractsCreationService.class,
            "/templates/defaults/contracts/offer-income-defaults.json");

    private static final String CLIENT_DEFAULTS = readAllLinesFromClassPath(ContractsCreationService.class,
            "/templates/defaults/contracts/client-defaults.json");

    private static final String PERSONS_DEFAULTS = readAllLinesFromClassPath(ContractsCreationService.class,
            "/templates/defaults/contracts/person-defaults.json");

    private final ObjectMapper jsonMapper = new ObjectMapper();

    @SneakyThrows
    private <T> T getStructureFromDefaults(String defaults, Class<T> tClass) {
        return jsonMapper.readValue(defaults, tClass);
    }

    @Test
    public void testCreateClient() {

        ClientStructure clientStructure = new ClientStructure()
                .setCity("Tel Aviv")
                .setCurrency("ILS")
                .setName("ClientName")
                .setPhone("+79991234567")
                .setRegionId(181);

        ClientStructure clientStructure2 = getStructureFromDefaults(CLIENT_DEFAULTS, ClientStructure.class)
                .setName("ClientName")
                .setPhone("+79991234567")
                .setRegionId(181);

        Assertions.assertThat(clientStructure2).usingRecursiveComparison()
                .isEqualTo(clientStructure);

    }

    @Test
    public void testCreatePerson() {

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

        PersonStructure person2 = getStructureFromDefaults(PERSONS_DEFAULTS, PersonStructure.class)
                .setClientId(1358021735)
                .setName("PersonName")
                .setEmail("gm-support@yandex-team.ru")
                .setPhone("79991234567")
                .setName("ClientName")
                .setPhone("+79991234567")
                .setPostAddress("test test test test")
                .setPostCode("12345678")
                .setRegion(181)
                .setIlId("1234567890")
                .setInn("1234567890")
                .setLegalAddress("test test test test")
                .setBenBank("benbank")
                .setIban("IL000000000000000000000")
                .setSwift("TICSRUMM")
                .setIsPartner(false);

        Assertions.assertThat(person2).usingRecursiveComparison()
                .isEqualTo(person);

    }

    @Test
    public void testOfferIncome() {

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

        OfferStructure offerStructure2 = getStructureFromDefaults(OFFER_INCOME_DEFAULTS, OfferStructure.class)
                .setClientId(1358021735)
                .setPersonId(21233827)
                .setCountry(181)
                .setStartDt("2022-04-25")
                .setIsraelTaxPct(0.0)
                .setManagerUid(98700241L);

        Assertions.assertThat(offerStructure2).usingRecursiveComparison()
                .isEqualTo(offerStructure);

    }

    @Test
    public void testOfferOutcome() {

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
                .setManagerUid(1120000000145872L)
                .setNds(17);

        OfferStructure offerStructure2 = getStructureFromDefaults(OFFER_OUTCOME_DEFAULTS, OfferStructure.class)
                .setClientId(1358021735)
                .setPersonId(21233818)
                .setNetting(1)
                .setCountry(181)
                .setStartDt("2022-04-25")
                .setIsraelTaxPct(0.0)
                .setManagerUid(1120000000145872L);

        Assertions.assertThat(offerStructure2).usingRecursiveComparison()
                .isEqualTo(offerStructure);

    }



}
