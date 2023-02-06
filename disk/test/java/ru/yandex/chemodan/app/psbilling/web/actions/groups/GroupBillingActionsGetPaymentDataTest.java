package ru.yandex.chemodan.app.psbilling.web.actions.groups;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.balance.Address;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.balance.PaymentData;
import ru.yandex.chemodan.app.psbilling.core.balance.UrPaymentData;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.web.BaseWebTest;
import ru.yandex.chemodan.app.psbilling.web.model.PaymentDataListPojo;
import ru.yandex.chemodan.app.psbilling.web.utils.PaymentDataUtils;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.test.Assert;

@ContextConfiguration(classes = {
        GroupBillingActionsGetPaymentDataTest.ConfigurationMocks.class,
})
public class GroupBillingActionsGetPaymentDataTest extends BaseWebTest {
    @Autowired
    private GroupBillingActions actions;
    @Autowired
    private BalanceService balanceService;

    private static final long CLIENT_ID = 123L;
    private static final long PERSON_ID = 891L;
    private static final PassportUidOrZero PASSPORT_UID = new PassportUidOrZero(567L);
    private static final String PAYMENT_DATA_KEY = PaymentDataUtils.buildPaymentDataId(PERSON_ID, CLIENT_ID, PASSPORT_UID.toUid());
    private static final PaymentData PAYMENT_DATA = new UrPaymentData(
            "disk-devnull@yandex-team.ru",
            "+79999999999",
            BalanceService.COUNTRY_RUSSIA.toString(),
            "My super organization",
            "1234567890",
            "7776665554432",
            new Address("119021", "ул. Льва Толстого, 16"),
            new Address("119021", "ул. Льва Толстого, 16")
    );

    @Test
    public void getOrganizationPaymentData_foundByGroup() {
        setupBalanceMock(Option.of(PAYMENT_DATA), Option.of(CLIENT_ID));

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(CLIENT_ID, PASSPORT_UID)));

        assertPaymentDataPojo(actions.getOrganizationPaymentDataV1(PASSPORT_UID, Option.of(group.getExternalId())));
        assertPaymentDataPojo(actions.getOrganizationPaymentDataV2(PASSPORT_UID, Option.of(group.getExternalId())));
    }

    @Test
    public void getOrganizationPaymentData_foundByUid() {
        setupBalanceMock(Option.of(PAYMENT_DATA), Option.of(CLIENT_ID));

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        assertPaymentDataPojo(actions.getOrganizationPaymentDataV1(PASSPORT_UID, Option.of(group.getExternalId())));
        assertPaymentDataPojoEmpty(actions.getOrganizationPaymentDataV2(PASSPORT_UID, Option.of(group.getExternalId())));
    }

    @Test
    public void getOrganizationPaymentData_notFoundAll() {
        setupBalanceMock(Option.empty(), Option.empty());

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        assertPaymentDataPojoEmpty(actions.getOrganizationPaymentDataV1(PASSPORT_UID, Option.of(group.getExternalId())));
        assertPaymentDataPojoEmpty(actions.getOrganizationPaymentDataV2(PASSPORT_UID, Option.of(group.getExternalId())));
    }

    @Test
    public void getOrganizationPaymentData_emptyGroupExternalId() {
        setupBalanceMock(Option.of(PAYMENT_DATA), Option.of(CLIENT_ID));

        psBillingGroupsFactory.createGroup(x -> x.paymentInfo(null));

        assertPaymentDataPojo(actions.getOrganizationPaymentDataV1(PASSPORT_UID, Option.empty()));
        assertPaymentDataPojo(actions.getOrganizationPaymentDataV2(PASSPORT_UID, Option.empty()));
    }

    private void assertPaymentDataPojo(PaymentDataListPojo pojo) {
        Assert.sizeIs(1, pojo.getValues());
        Assert.isTrue(pojo.getValues().containsKey(PAYMENT_DATA_KEY));
        Assert.equals(pojo.getValues().get(PAYMENT_DATA_KEY).toCorePaymentData(), PAYMENT_DATA);
    }

    private void assertPaymentDataPojoEmpty(PaymentDataListPojo pojo) {
        Assert.sizeIs(0, pojo.getValues());
    }

    public void setupBalanceMock(Option<PaymentData> paymentData, Option<Long> clientId) {
        Mockito.when(balanceService.findPaymentData(CLIENT_ID))
                .thenReturn(paymentData.map(pd -> Cf.map(PERSON_ID, pd)).orElse(Cf.map()));
        Mockito.when(balanceService.findClient(PASSPORT_UID.toUid()))
                .thenReturn(clientId);
        Mockito.when(balanceService.getActiveContract(CLIENT_ID))
                .thenReturn(Option.of(GetClientContractsResponseItem.buildActiveBalanceContract()));
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
