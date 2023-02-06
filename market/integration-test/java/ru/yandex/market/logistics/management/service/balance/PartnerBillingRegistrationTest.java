package ru.yandex.market.logistics.management.service.balance;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.model.result.PartnerProcessingResult;
import ru.yandex.market.logistics.management.queue.processor.billing.AbstractPartnerBillingRegistrationProcessor;
import ru.yandex.market.logistics.management.queue.producer.PartnerBillingRegistrationTaskProducer;
import ru.yandex.market.logistics.management.service.billing.PartnerBillingRegistrationException;
import ru.yandex.market.logistics.management.service.client.PartnerService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.clientRequest;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.clientResponse;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.searchParamsRequest;

@CleanDatabase
@DatabaseSetup("/data/service/balance/legal_info.xml")
class PartnerBillingRegistrationTest extends AbstractContextualTest {

    private static final long PARTNER_ID = 1L;
    private static final int CLIENT_ID = 1340576061;
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", PARTNER_ID);

    @Autowired
    @Qualifier("partnerBillingClientCreationProcessor")
    private AbstractPartnerBillingRegistrationProcessor<EntityIdPayload, EntityIdPayload> clientCreationProcessor;
    @Autowired
    @Qualifier("partnerBillingClientLinkingProcessor")
    private AbstractPartnerBillingRegistrationProcessor<EntityIdPayload, EntityIdPayload> clientLinkingProcessor;
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private Balance2 balance2;
    @Autowired
    @Qualifier("partnerBillingClientCreationTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> clientCreationTaskProducer;
    @Autowired
    @Qualifier("partnerBillingClientLinkingTaskProducer")
    private PartnerBillingRegistrationTaskProducer<EntityIdPayload> clientLinkingTaskProducer;

    @BeforeEach
    void setup() {
        Mockito.reset(clientCreationTaskProducer, clientLinkingTaskProducer);
        Mockito.doNothing().when(clientCreationTaskProducer).produceTask(any());
        Mockito.doNothing().when(clientLinkingTaskProducer).produceTask(any());
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_without_uid.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/partner_without_uid.xml"
    )
    @DisplayName("Не можем зарегистрировать, так как нет passportUid")
    void failedRegistrationWithoutUid() {
        assertThrows(
            IllegalStateException.class,
            () -> clientCreationProcessor.processPayload(PAYLOAD)
        );

        checkBillingInfo(PARTNER_ID, null);
        Mockito.verifyZeroInteractions(clientLinkingTaskProducer);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_wo_uid_with_billing_client_id.xml")
    @DisplayName("Партнер создан уже с billingClientId, но без passportUid")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/partner_wo_uid_with_billing_client_id.xml"
    )
    void withBillingClientIdWithoutPassportUid() throws Exception {

        clientCreationProcessor.processPayload(PAYLOAD);

        checkTaskProducerWasInvokedForPartner(clientLinkingTaskProducer, PARTNER_ID);

        clientLinkingProcessor.processPayload(PAYLOAD);

        checkBillingInfo(PARTNER_ID, (long) CLIENT_ID);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_billing_client_id.xml")
    @DisplayName("Для партнера уже указан billingClientId")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/billing_client_id_already_set.xml"
    )
    void billingClientIdAlreadySet() {
        clientCreationProcessor.processPayload(PAYLOAD);

        checkBillingInfo(PARTNER_ID, (long) CLIENT_ID);

        checkTaskProducerWasInvokedForPartner(clientLinkingTaskProducer, PARTNER_ID);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_uid.xml")
    @DisplayName("Ошибка поиска существующего клиента")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/billing_client_search_error.xml"
    )
    void failedRegistrationOnBalanceFindClientError() throws Exception {
        Mockito.when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{1, "Balance FindClient error", new Object[]{}});

        assertThrows(
            PartnerBillingRegistrationException.class,
            () -> clientCreationProcessor.processPayload(PAYLOAD)
        );

        checkBillingInfo(PARTNER_ID, null);
        Mockito.verifyZeroInteractions(clientLinkingTaskProducer);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_uid.xml")
    @DisplayName("Нашли уже существующиего клиента")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/existing_billing_client_found.xml"
    )
    void successfulFindClient() throws Exception {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{0, "", new Object[]{clientResponse(CLIENT_ID)}});

        clientCreationProcessor.processPayload(PAYLOAD);

        checkBillingInfo(PARTNER_ID, (long) CLIENT_ID);

        checkTaskProducerWasInvokedForPartner(clientLinkingTaskProducer, PARTNER_ID);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_uid.xml")
    @DisplayName("Нет существующего клиента, а нового создать не смогли")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/billing_client_creation_error.xml"
    )
    void failedRegistrationOnBalanceCreateClientError() throws Exception {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{0, "", new Object[]{}});
        when(balance2.CreateClient("123456", clientRequest()))
            .thenReturn(new Object[]{1, "Balance CreateClient error", CLIENT_ID});

        assertThrows(
            PartnerBillingRegistrationException.class,
            () -> clientCreationProcessor.processPayload(PAYLOAD)
        );

        checkBillingInfo(PARTNER_ID, null);
        Mockito.verifyZeroInteractions(clientLinkingTaskProducer);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_uid.xml")
    @DisplayName("Создали новго клиента, но не смогли привязать")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/billing_client_linking_error.xml"
    )
    void failedRegistrationOnBalanceAssociationError() throws Exception {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{0, "", new Object[]{}});
        when(balance2.CreateClient("123456", clientRequest()))
            .thenReturn(new Object[]{0, "", CLIENT_ID});
        when(balance2.CreateUserClientAssociation("123456", CLIENT_ID, "123456"))
            .thenReturn(new Object[]{1, "Balance CreateUserClientAssociation error"});

        clientCreationProcessor.processPayload(PAYLOAD);

        checkTaskProducerWasInvokedForPartner(clientLinkingTaskProducer, PARTNER_ID);

        assertThrows(
            PartnerBillingRegistrationException.class,
            () -> clientLinkingProcessor.processPayload(PAYLOAD)
        );

        checkBillingInfo(PARTNER_ID, (long) CLIENT_ID);
    }

    @Test
    @DatabaseSetup("/data/service/balance/partner_with_uid.xml")
    @DisplayName("Успешный сценарий создания и привязки клиента")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT,
        value = "/data/service/balance/after/successful_creation_and_linking.xml"
    )
    void successfulRegistration() throws Exception {
        when(balance2.FindClient(searchParamsRequest()))
            .thenReturn(new Object[]{0, "", new Object[]{}});
        when(balance2.CreateClient("123456", clientRequest()))
            .thenReturn(new Object[]{0, "", CLIENT_ID});
        when(balance2.CreateUserClientAssociation("123456", CLIENT_ID, "123456"))
            .thenReturn(new Object[]{0, ""});

        clientCreationProcessor.processPayload(PAYLOAD);

        checkTaskProducerWasInvokedForPartner(clientLinkingTaskProducer, PARTNER_ID);

        clientLinkingProcessor.processPayload(PAYLOAD);

        checkBillingInfo(PARTNER_ID, (long) CLIENT_ID);
    }

    private void checkTaskProducerWasInvokedForPartner(
        PartnerBillingRegistrationTaskProducer taskProducer,
        long partnerId
    ) {
        ArgumentCaptor<PartnerProcessingResult> argumentCaptor = ArgumentCaptor.forClass(PartnerProcessingResult.class);
        Mockito.verify(taskProducer, Mockito.times(1))
            .produceTask(argumentCaptor.capture());
        softly.assertThat(argumentCaptor.getValue())
            .extracting(PartnerProcessingResult::getPartnerId)
            .isEqualTo(partnerId);
    }

    private void checkBillingInfo(long partnerId, Long billingClientId) {
        Partner partner = partnerService.findByIdOrThrow(partnerId);
        softly.assertThat(partner.getBillingClientId()).isEqualTo(billingClientId);
    }
}
