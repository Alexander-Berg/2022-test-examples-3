package ru.yandex.market.crm.triggers.services.logbroker.consumers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.core.services.bpm.ProcessVariablesNames;
import ru.yandex.market.crm.core.services.logbroker.LogTypesResolver;
import ru.yandex.market.crm.core.services.trigger.MessageTypes;
import ru.yandex.market.crm.lb.LBInstallation;
import ru.yandex.market.crm.lb.LogIdentifier;
import ru.yandex.market.crm.platform.ConfigRepository;
import ru.yandex.market.crm.platform.FactHolder;
import ru.yandex.market.crm.triggers.services.platform.PlatformUtils;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.config.FactInfo;
import ru.yandex.market.crm.platform.models.ExternalCertificate;
import ru.yandex.market.crm.triggers.TestConfiguration;
import ru.yandex.market.crm.triggers.services.bpm.BpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.TriggerService;
import ru.yandex.market.crm.triggers.services.bpm.UidBpmMessage;
import ru.yandex.market.crm.triggers.services.bpm.correlation.MessageSender;
import ru.yandex.market.crm.triggers.services.bpm.platform.FactStrategyRegistry;
import ru.yandex.market.crm.triggers.services.bpm.variables.NewReceiptOnCertificate;
import ru.yandex.market.mcrm.tx.TxService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class PlatformConsumerTest {
    private PlatformConsumer platformConsumer;

    private final TriggerService triggerService = mock(TriggerService.class);
    private final MessageSender messageSender = mock(MessageSender.class);

    @Inject
    private PlatformUtils platformUtils;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private FactStrategyRegistry strategyRegistry;

    private FactInfo factConfig;

    @Before
    public void before() {
        LogTypesResolver logTypes = mock(LogTypesResolver.class);
        when(logTypes.getLogIdentifier("platform"))
                .thenReturn(new LogIdentifier(null, null, LBInstallation.LOGBROKER));

        TxService txService = mock(TxService.class);
        platformConsumer = new PlatformConsumer(platformUtils, triggerService, messageSender, logTypes,
                strategyRegistry, txService);
        factConfig = configRepository.getFact("ExternalCertificatePaid");
    }

    @Test
    @Ignore("В рамках https://st.yandex-team.ru/LILUCRM-2381")
    public void checkBpmnMessageForExternalCertificatePrinted() {
        ExternalCertificate certificate = mockExternalCertificatePrinted();

        FactHolder factHolder = new FactHolder(factConfig, certificate);

        List<BpmMessage> messages = platformConsumer.asBpmMessages(factHolder);
        Assert.assertEquals(1, messages.size());

        BpmMessage message = messages.get(0);
        Assert.assertTrue(message instanceof UidBpmMessage);

        UidBpmMessage actualMessage = (UidBpmMessage) message;

        NewReceiptOnCertificate.CertificateInfo certificateInfo = new NewReceiptOnCertificate.CertificateInfo();
        certificateInfo.setExpiredAt(1234567891L);
        certificateInfo.setCreatedAt(1234567890L);
        certificateInfo.setSku(12345);
        certificateInfo.setPrice(100);
        certificateInfo.setId(1);
        certificateInfo.setSupplierId(1010);
        certificateInfo.setBuyer(
                new NewReceiptOnCertificate.BuyerInfo("Test 1", "test@mail.ru", "+7929292929")
        );

        NewReceiptOnCertificate receiptOnCertificate = new NewReceiptOnCertificate();
        receiptOnCertificate.setId(123);
        receiptOnCertificate.setCertificateInfo(certificateInfo);

        Assert.assertEquals(MessageTypes.RECEIPT_PRINTED, actualMessage.getType());
        Assert.assertEquals(ru.yandex.market.crm.mapreduce.domain.user.Uid.asEmail("test@mail.ru"),
                actualMessage.getUid());

        Map<String, Object> actualVariables = actualMessage.getVariables();
        Assert.assertEquals(2, actualVariables.size());
        Assert.assertTrue(equals(receiptOnCertificate,
                (NewReceiptOnCertificate) actualVariables.get(ProcessVariablesNames.Event.RECEIPT_PRINTED_ON_CERTIFICATE)));
    }

    @Test
    @Ignore("В рамках https://st.yandex-team.ru/LILUCRM-2381")
    public void checkTriggerServiceInvokedIfReceiptPrinted() {
        ExternalCertificate certificate = mockExternalCertificatePrinted();

        FactHolder factHolder = new FactHolder(factConfig, certificate);

        // вызов системы
        platformConsumer.accept(Collections.singletonList(factHolder));

        // проверка утверждений (достаточно, что не был вызыван)
        verify(triggerService, times(1)).sendMessage(any(BpmMessage.class));
    }

    @Test
    public void checkTriggerServiceInvokedIfSomeExternalCertificateExceptReceiptPrinted() {
        ExternalCertificate certificate = mockExternalCertificate();

        FactHolder factHolder = new FactHolder(factConfig, certificate);

        // вызов системы
        platformConsumer.accept(Collections.singletonList(factHolder));

        // проверка утверждений (достаточно, что был вызыван один раз)
        verify(triggerService, times(0)).sendMessage(any(BpmMessage.class));
    }

    private boolean equals(NewReceiptOnCertificate receipt1, NewReceiptOnCertificate receipt2) {
        return receipt1.getId() == receipt2.getId() &&
                equals(receipt1.getCertificateInfo(), receipt2.getCertificateInfo());
    }

    private boolean equals(NewReceiptOnCertificate.CertificateInfo certificate1,
                           NewReceiptOnCertificate.CertificateInfo certificate2) {
        return certificate1.getId() == certificate2.getId() &&
                certificate1.getCreatedAt() == certificate2.getCreatedAt() &&
                certificate1.getExpiredAt() == certificate2.getExpiredAt() &&
                certificate1.getPrice() == certificate2.getPrice() &&
                certificate1.getSku() == certificate2.getSku() &&
                certificate1.getBuyer().getName().equals(certificate2.getBuyer().getName()) &&
                certificate1.getBuyer().getEmail().equals(certificate2.getBuyer().getEmail()) &&
                certificate1.getBuyer().getPhone().equals(certificate2.getBuyer().getPhone()) &&
                certificate1.getSupplierId() == certificate2.getSupplierId();
    }

    private ExternalCertificate mockExternalCertificate() {
        return ExternalCertificate.newBuilder()
                .setId(1)
                .setEventType(ExternalCertificate.EventType.CERTIFICATE_STATUS_UPDATED)
                .build();
    }

    private ExternalCertificate mockExternalCertificatePrinted() {
        return ExternalCertificate.newBuilder()
                .setReceiptId(123)
                .setId(1)
                .setCreatedAt(1234567890L)
                .setExpiryAt(1234567891L)
                .setPrice(100)
                .setMsku(12345)
                .setBuyer(ExternalCertificate.User
                        .newBuilder()
                        .setName("Test 1")
                        .setEmail("test@mail.ru")
                        .setPhone("+7929292929")
                        .build()
                )
                .setSupplierId(1010)
                .setUid(Uid.newBuilder().setType(UidType.EMAIL).setStringValue("test@mail.ru").build())
                .setEventType(ExternalCertificate.EventType.RECEIPT_PRINTED)
                .build();
    }
}
