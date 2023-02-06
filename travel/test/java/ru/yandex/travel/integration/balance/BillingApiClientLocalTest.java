package ru.yandex.travel.integration.balance;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import io.opentracing.mock.MockTracer;
import lombok.extern.slf4j.Slf4j;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.integration.balance.model.BillingClient;
import ru.yandex.travel.integration.balance.model.BillingCommissionType;
import ru.yandex.travel.integration.balance.model.BillingContract;
import ru.yandex.travel.integration.balance.model.BillingOfferConfirmationType;
import ru.yandex.travel.integration.balance.model.BillingPaymentType;
import ru.yandex.travel.integration.balance.model.BillingPerson;
import ru.yandex.travel.integration.balance.model.BillingUrPerson;
import ru.yandex.travel.integration.balance.responses.BillingCreateContractResponse;
import ru.yandex.travel.tvm.TvmHelper;
import ru.yandex.travel.tvm.TvmProperties;
import ru.yandex.travel.tvm.TvmWrapper;

/**
 * Command Line test:
 * curl -d "@UpdatePaymentReq.xml" -H "X-Ya-Service-Ticket: ${TVM_TICKET}" 'http://greed-tm.paysys.yandex
 * .ru:8002/xmlrpctvm'
 * <p/>
 * Environments:
 * <il>
 * <li>Prod: https://admin.balance.yandex-team.ru/passports.xml?tcl_id=71687062</li>
 * <li>Testing: https://admin-balance.greed-tm.paysys.yandex-team.ru/passports.xml?tcl_id=132907448</li>
 * </il>
 */
@Ignore
@Slf4j
public class BillingApiClientLocalTest {
    private static final String BILLING_ADMIN_URL = "https://admin-balance.greed-tm.paysys.yandex-team.ru";
    //private static final String BILLING_ADMIN_URL = "https://admin.balance.yandex-team.ru";

    private TvmWrapper tvm;
    private AsyncHttpClient billingAhc;
    private BillingApiClient billingApi;

    @Before
    public void init() {
        billingAhc = Dsl.asyncHttpClient(Dsl.config().setThreadPoolName("localBillingApiTestAhc"));

        BillingApiProperties apiProperties = new BillingApiProperties();
        // ssh -L 8002:greed-tm.paysys.yandex.ru:8002 ${USER}.sas.yp-c.yandex.net
        // ssh -L 8002:greed-tm.paysys.yandex.ru:8002 __ANY_TESTING_HOST__
        // (hosts: https://nanny.yandex-team.ru/ui/#/services/catalog/travel_orders_app_testing/)
        apiProperties.setBaseUrl("http://localhost:8002/xmlrpctvm");
        // PROD access:
        // ssh -L 8004:balance-xmlrpc-tvm.paysys.yandex.net:8004 __PROD_PROD_HOST__
        // (hosts: https://nanny.yandex-team.ru/ui/#/services/catalog/travel_orders_app_prod/)
        // Then add "127.0.0.1       balance-xmlrpc-tvm.paysys.yandex.net" to your /etc/hosts
        //apiProperties.setBaseUrl("https://balance-xmlrpc-tvm.paysys.yandex.net:8004/xmlrpctvm");
        apiProperties.setHttpRequestTimeout(Duration.ofSeconds(10));
        apiProperties.setHttpReadTimeout(Duration.ofSeconds(10));
        apiProperties.setTvmEnabled(true);
        apiProperties.setTvmDestinationAlias("balance_api");

        TvmProperties tvmProperties = new TvmProperties();
        tvmProperties.setClientId(2002740);     // orders_app               @ testing
        //tvmProperties.setClientId(2002742);   // orders_app               @ prod
        //tvmProperties.setClientId(2018390);   // hotel_administrator      @ testing
        // see TvmConfigurationLocalTest.createManualTestsTvmClient (should be moved out into a lib and re-used)
        tvmProperties.setClientSecret("CHANGE_ME");
        //tvmProperties.setClientSecret(LocalTestUtils.readLocalToken(".tvm/orders_app_testing.secret"));
        //tvmProperties.setClientSecret(LocalTestUtils.readLocalToken(".tvm/orders_app_prod.secret"));
        tvmProperties.setServiceAliasIdMapping("balance_api=2000601");
        //tvmProperties.setServiceAliasIdMapping("balance_api=2000599"); // prod
        tvmProperties.setDstServiceAliases("balance_api");
        tvm = TvmHelper.getTvmWrapper(tvmProperties);

        AsyncHttpClientWrapper ahcWrapper = new AsyncHttpClientWrapper(billingAhc,
                LoggerFactory.getLogger("HttpLogger"), "default", new MockTracer(), null);
        billingApi = new BillingApiClient(ahcWrapper, apiProperties, tvm);
    }

    @After
    public void tearDown() throws Exception {
        billingAhc.close();
        tvm.close();
    }

    @Test
    public void testPartnerPayouts() {
        billingApi.updatePayment(641, 211, Instant.now());
    }

    @Test
    public void testGetClientContract() {
        for (BillingClientContract clientContract : billingApi.getClientContracts(71687062)) {
            log.info("Contract: {}\n", clientContract);
        }
    }

    @Test
    public void testGetClientPersons() {
        for (BillingPerson billingPerson : billingApi.getClientPersons(1337971706L)) {
            log.info("Billing Person: {}\n", billingPerson);
        }
    }

    @Test
    public void testGetPerson() {
        log.info("Billing Person: {}\n", billingApi.getPerson(11996179L));
    }

    @Test
    public void testGetClient() {
        log.info("Billing Client: {}\n", billingApi.getClient(1337971706L));
    }

    @Test
    public void testNewHotelPartnerRegistration() {
        long operatorId = 1120000000070627L;    // robot-travel-test
        long managerCode = 30891;               // abardina

        Long clientId = billingApi.createClient(operatorId, BillingClient.builder()
                .name("Гостиница «Орбита»")
                .build());
        log.info("New Client ID: {}\n{}/passports.xml?tcl_id={}\n",
                clientId, BILLING_ADMIN_URL, clientId);
        // example: https://admin-balance.greed-tm.paysys.yandex-team.ru/passports.xml?tcl_id=132910168

        long personId = billingApi.createPerson(operatorId, BillingUrPerson.builder()
                .clientId(clientId)
                .name("Гостиница «Орбита» - филиал АО «Гостиничный комплекс «Славянка»")
                .longName("Гостиница «Орбита» - филиал акционерного общества «Гостиничный комплекс «Славянка»")
                .inn("7702706914")
                .kpp("780443001")
                .phone("8(495) 999-99-99")
                .email("some-email@example.com")
                .postCode("194021")
                .postAddress("г. Санкт - Петербург, проспект Непокоренных, дом 4")
                .legalAddress("г. Москва, Суворовская площадь, д.2 стр.3")
                .bik("044030704")
                .account("40702810439040002377")
                .build());
        log.info("New Person ID: {}\n{}/subpersons.xml?tcl_id={}\n",
                personId, BILLING_ADMIN_URL, clientId);
        // example: https://admin-balance.greed-tm.paysys.yandex-team.ru/subpersons.xml?tcl_id=132910168

        BillingCreateContractResponse offerRes = billingApi.createContract(operatorId, BillingContract.builder()
                .clientId(clientId)
                .personId(personId)
                .currency("RUB")
                .firmId(1L)
                .services(List.of(641))
                .paymentType(BillingPaymentType.POSTPAID)
                .managerCode(managerCode)
                //.contractType(BillingContractType.SPENDABLE)
                .offerConfirmationType(BillingOfferConfirmationType.NO)
                .commission(BillingCommissionType.OFFER)
                .paymentTerm(15)
                .startDate(LocalDate.now()) // billing TZ is assumed
                .build());
        log.info("New Offer ID: {}\n{}/contract.xml?contract_id={}\n",
                offerRes.getContractId(), BILLING_ADMIN_URL, offerRes.getContractId());
        // example: https://admin-balance.greed-tm.paysys.yandex-team.ru/contract.xml?contract_id=1668435
    }

    @Test
    public void testExistingHotelPartnerUpdate() {
        long operatorId = 4021320124L;
        long clientId = 132992060;
        long personId = 32442541;

        billingApi.updateClient(operatorId, BillingClient.builder()
                .clientId(clientId)
                .name("Гостиница «Орбита» - UPD")
                .phone("7911-111-1111")
                .email("new-phone@exmaple.com")
                .build());
        log.info("Updated Client:\n{}/passports.xml?tcl_id={}\n", BILLING_ADMIN_URL, clientId);
        // example: https://admin-balance.greed-tm.paysys.yandex-team.ru/passports.xml?tcl_id=132992060

        billingApi.updatePerson(operatorId, BillingUrPerson.builder()
                .personId(personId)
                .clientId(clientId)
                .name("Гостиница «Орбита» - short - UPD")
                .longName("Гостиница «Орбита» - long - UPD")
                // prohibited changes, be aware!
                //.inn("7704340310")
                .kpp("770401001")
                .phone("+7 (495) 999-99-99")
                .email("some-email@example.com; more-emails@example.com")
                .postCode("999999")
                .postAddress("Post Address 2")
                .legalAddress("Россия, город Москва, улица Льва Толстого, дом 16.")
                .bik("044525222")
                .account("40702810800001005378")
                .build());
        log.info("Updated Person:\n{}/subpersons.xml?tcl_id={}\n", BILLING_ADMIN_URL, clientId);
        // example: https://admin-balance.greed-tm.paysys.yandex-team.ru/subpersons.xml?tcl_id=132992060
    }
}
