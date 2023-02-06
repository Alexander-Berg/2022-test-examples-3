package ru.yandex.chemodan.app.psbilling.web.services;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.balance.Address;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.balance.PaymentData;
import ru.yandex.chemodan.app.psbilling.core.balance.PhPaymentData;
import ru.yandex.chemodan.app.psbilling.core.balance.UrPaymentData;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.web.config.PsBillingWebServicesConfiguration;
import ru.yandex.chemodan.app.psbilling.web.exceptions.WebActionException;
import ru.yandex.chemodan.app.psbilling.web.model.AddressPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.model.PhPaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.model.UrPaymentDataPojo;
import ru.yandex.chemodan.app.psbilling.web.utils.PaymentDataUtils;
import ru.yandex.chemodan.balanceclient.exception.BalanceException;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientPersonsResponseItem;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;


@ContextConfiguration(classes = {
        PsBillingWebServicesConfiguration.class,
        PaymentDataServiceTest.ConfigurationMocks.class
})
public class PaymentDataServiceTest extends AbstractPsBillingCoreTest {
    private static final long CONTRACT_ID = 7654;
    private static final long PERSON_ID = 1234;
    private static final long CLIENT_ID = 5678;
    private static final PassportUid PASSPORT_UID = new PassportUid(9999);
    private static final String PAYMENT_DATA_ID = PaymentDataUtils.buildPaymentDataId(PERSON_ID, CLIENT_ID,
            PASSPORT_UID);

    private static final String ORGANIZATION_NAME = "My super organization";
    private static final String EMAIL = "disk-devnull@yandex-team.ru";
    private static final String PHONE = "+79999999999";
    private static final UrPaymentData UR_PAYMENT_DATA = new UrPaymentData(
            EMAIL,
            PHONE,
            BalanceService.COUNTRY_RUSSIA.toString(),
            ORGANIZATION_NAME,
            "1234567890",
            "7776665554432",
            new Address("119021", "ул. Льва Толстого, 16"),
            new Address("119021", "ул. Льва Толстого, 16")
    );
    private static final String PH_BALANCE_NAME = "Иван Иванов";
    private static final PhPaymentData PH_PAYMENT_DATA = new PhPaymentData(
            EMAIL,
            PHONE,
            BalanceService.COUNTRY_RUSSIA.toString(),
            "Иванов",
            "Иван",
            "Иванович"
    );

    @Autowired
    private PaymentDataService paymentDataService;
    @Autowired
    private BalanceService balanceService;

    @Test
    public void buildUrBalancePaymentData__emptyParams() {
        Assert.assertThrows(
                () -> paymentDataService.buildBalancePaymentData(PASSPORT_UID, Option.empty(), Option.empty()),
                WebActionException.class
        );

        checkNeverCreateOffer();
        checkNeverFindClient();
        checkNeverGetActiveContract();
        checkNeverFindPaymentData();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentId__withoutClientInBalance() {
        setupFindClient(Option.empty());
        String paymentDataId = PASSPORT_UID.getUid() + "_" + PERSON_ID;

        Assert.assertThrows(
                () -> paymentDataService.buildBalancePaymentData(PassportUid.MAX_VALUE, Option.of(paymentDataId),
                        Option.empty()),
                WebActionException.class
        );

        checkNeverCreateOffer();
        checkNeverGetActiveContract();
        checkNeverFindPaymentData();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentId__withoutPaymentData() {
        setupFindPaymentData(Cf.map());

        Assert.assertThrows(
                () -> paymentDataService.buildBalancePaymentData(PassportUid.MAX_VALUE, Option.of(PAYMENT_DATA_ID),
                        Option.empty()),
                WebActionException.class
        );

        checkNeverCreateOffer();
        checkNeverGetActiveContract();
        checkNeverFindClient();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentId__withoutActiveContract() {
        setupGetActiveContract(false);
        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PassportUid.MAX_VALUE,
                Option.of(PAYMENT_DATA_ID), Option.empty());
        checkUrPaymentInfo(paymentInfo);

        checkNeverFindClient();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildPhBalancePaymentData__withPaymentId__withoutActiveContract() {
        setupPh();
        setupGetActiveContract(false);
        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PassportUid.MAX_VALUE,
                Option.of(PAYMENT_DATA_ID), Option.empty());
        checkPhPaymentInfo(paymentInfo);

        checkNeverFindClient();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentId__withActiveContract() {
        setupGetActiveContract(true);
        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PassportUid.MAX_VALUE,
                Option.of(PAYMENT_DATA_ID), Option.empty());
        checkUrPaymentInfo(paymentInfo);

        checkNeverCreateOffer();
        checkNeverFindClient();
        checkNeverFindOrCreateClient();
        checkNeverCreatePerson();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentData() {
        setupGetActiveContract(false);
        PaymentDataPojo paymentDataPojo = new UrPaymentDataPojo(
                EMAIL,
                PHONE,
                BalanceService.COUNTRY_RUSSIA.toString(),
                ORGANIZATION_NAME,
                "1234567890",
                "7776665554432",
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                false
        );

        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PASSPORT_UID, Option.empty(),
                Option.of(paymentDataPojo));
        checkUrPaymentInfo(paymentInfo);

        checkNeverGetActiveContract();
        checkNeverFindClient();
        checkNeverFindPaymentData();
    }

    @Test
    public void buildUrBalancePaymentData__withPaymentData__withBalanceTimeoutOnCreateOffer() {
        // эмулируем ситуацию, когда первй запрос создания оффера в балансе таймаутится, но сам оффер создает
        // следующие попытки создать оффер будут падать с ошибкой, что оффер для клинета уже есть
        Mockito.when(
                balanceService.createOffer(PASSPORT_UID, CLIENT_ID, PERSON_ID)
        ).thenThrow(BalanceException.class);

        Mockito.when(balanceService.getActiveContract(CLIENT_ID))
                .thenReturn(Option.empty())
                .thenReturn(Option.of(getClientContractsResponseItem()));

        PaymentDataPojo paymentDataPojo = new UrPaymentDataPojo(UR_PAYMENT_DATA, false);
        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PASSPORT_UID, Option.empty(),
                Option.of(paymentDataPojo));
        checkUrPaymentInfo(paymentInfo);

        checkNeverGetActiveContract();
        checkNeverFindClient();
    }

    @Test
    public void buildPhBalancePaymentData__withPaymentData() {
        setupPh();
        setupGetActiveContract(false);
        PaymentDataPojo paymentDataPojo = new PhPaymentDataPojo(PH_PAYMENT_DATA, false);

        BalancePaymentInfo paymentInfo = paymentDataService.buildBalancePaymentData(PASSPORT_UID, Option.empty(),
                Option.of(paymentDataPojo));
        checkPhPaymentInfo(paymentInfo);

        checkNeverGetActiveContract();
        checkNeverFindClient();
        checkNeverFindPaymentData();
    }

    @Test
    public void buildBalancePaymentData__withPaymentData__withoutCountryCode() {
        PaymentDataPojo paymentDataPojo = new UrPaymentDataPojo(
                EMAIL,
                PHONE,
                null,
                ORGANIZATION_NAME,
                "1234567890",
                "7776665554432",
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                new AddressPojo("119021", "ул. Льва Толстого, 16"),
                false
        );

        Assert.assertThrows(() -> paymentDataService.buildBalancePaymentData(PASSPORT_UID, Option.empty(),
                Option.of(paymentDataPojo)), WebActionException.class, e -> e.getHttpStatusCode() == 400);
        checkNeverGetActiveContract();
        checkNeverFindClient();
        checkNeverFindPaymentData();
    }

    private void checkUrPaymentInfo(BalancePaymentInfo paymentInfo) {
        checkPaymentInfo(paymentInfo, GetClientPersonsResponseItem.LEGAL_PERSON_TYPE);
    }

    private void checkPhPaymentInfo(BalancePaymentInfo paymentInfo) {
        checkPaymentInfo(paymentInfo, GetClientPersonsResponseItem.NATURAL_PERSON_TYPE);
    }

    private void checkPaymentInfo(BalancePaymentInfo paymentInfo, String type) {
        Assert.equals(paymentInfo.getPassportUid(), PASSPORT_UID);
        Assert.equals(paymentInfo.getClientId(), CLIENT_ID);
        Assert.equals(paymentInfo.getClientType().value(), type);
    }

    private void setupCreateOffer() {
        Mockito.when(
                balanceService.createOffer(PASSPORT_UID, CLIENT_ID, PERSON_ID)
        ).thenReturn(CONTRACT_ID);
    }

    private void checkNeverCreateOffer() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).createOffer(Mockito.any(), Mockito.anyLong(), Mockito.anyLong());
    }

    private void setupGetActiveContract(boolean contractExist) {
        GetClientContractsResponseItem response = getClientContractsResponseItem();

        Mockito.when(
                balanceService.getActiveContract(CLIENT_ID)
        ).thenReturn(contractExist ? Option.of(response) : Option.empty());
    }

    private GetClientContractsResponseItem getClientContractsResponseItem() {
        GetClientContractsResponseItem response = new GetClientContractsResponseItem();
        response.setPersonId(PERSON_ID);
        response.setId(CONTRACT_ID);
        return response;
    }

    private void checkNeverGetActiveContract() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).getActiveContract(Mockito.any());
    }

    private void setupFindClient(Option<Long> clientId) {
        Mockito.when(
                balanceService.findClient(PASSPORT_UID)
        ).thenReturn(clientId);
    }

    private void checkNeverFindClient() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).findClient(Mockito.any());
    }

    private void setupFindPaymentData(MapF<Long, PaymentData> paymentData) {
        Mockito.when(
                balanceService.findPaymentData(CLIENT_ID)
        ).thenReturn(paymentData);
    }

    private void checkNeverFindPaymentData() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).findPaymentData(Mockito.anyLong());
    }

    private void setupFindOrCreateClient(String clientName) {
        Mockito.when(
                balanceService.findOrCreateClient(PASSPORT_UID, clientName, EMAIL, PHONE)
        ).thenReturn(CLIENT_ID);
    }

    private void checkNeverFindOrCreateClient() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).findOrCreateClient(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void setupCreatePerson(PaymentData paymentData) {
        Mockito.when(
                balanceService.createPerson(PASSPORT_UID, CLIENT_ID, paymentData)
        ).thenReturn(PERSON_ID);
    }

    private void checkNeverCreatePerson() {
        Mockito.verify(
                balanceService, Mockito.never()
        ).createPerson(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Before
    public void setup() {
        setupUr();
    }

    private void setupUr() {
        setup(UR_PAYMENT_DATA, ORGANIZATION_NAME);
    }

    private void setupPh() {
        setup(PH_PAYMENT_DATA, PH_BALANCE_NAME);
    }

    private void setup(PaymentData paymentData, String clientName) {
        setupFindClient(Option.of(CLIENT_ID));
        setupFindPaymentData(Cf.map(PERSON_ID, paymentData));
        setupGetActiveContract(true);
        setupCreateOffer();
        setupFindOrCreateClient(clientName);
        setupCreatePerson(paymentData);
    }

    @Configuration
    public static class ConfigurationMocks {
        @Autowired
        private PsBillingCoreMocksConfig mocksConfig;

        @Primary
        @Bean
        public BalanceService balanceService() {
            return mocksConfig.addMock(BalanceService.class);
        }
    }
}
