package ru.yandex.market.logistics.management.service.balance;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.processor.billing.AbstractPartnerBillingRegistrationProcessor;
import ru.yandex.market.logistics.management.service.billing.PartnerBillingRegistrationException;

import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.linkIntegrationToClientStructure;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner_subtypes.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner.xml")
class IntegrationToClientLinkingTest extends AbstractContextualTest {
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", 1L);
    private static final String OPERATOR_UID = "123456";

    @Autowired
    private AbstractPartnerBillingRegistrationProcessor integrationToClientLinkingTaskProcessor;

    @Autowired
    private Balance2 balance2;

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/without_uid.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/partner_without_uid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет passportUid")
    void failedIntegrationToClientLinkingWithoutUid() {
        softly.assertThatThrownBy(() -> integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Невозможно привязать конфигурацию интеграции к клиенту для партнёра id = 1: " +
                    "поле passportUid не заполнено."
            );
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_type_not_ds.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/partner_type_not_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как тип партнёра не СД")
    void failedIntegrationToClientLinkingForSc() {
        integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_subtype_not_pickup_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/partner_subtype_not_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как подтип партнёра не партнёрская ПВЗ")
    void failedIntegrationToClientLinkingForNotPickupPoint() {
        integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/integrationToClientLinking/before/no_billing_client_id.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/no_billing_client_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет billingClientId")
    void failedIntegrationToClientLinkingNoBillingClientId() {
        softly.assertThatThrownBy(() -> integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Невозможно привязать конфигурацию интеграции к клиенту для партнёра id = 1: " +
                    "поле billingClientId не заполнено."
            );
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешная привязка конфигурации интеграции к клиенту")
    void successIntegrationToClientLinking() throws XmlRpcException {
        when(balance2.LinkIntegrationToClient(OPERATOR_UID, linkIntegrationToClientStructure()))
            .thenReturn(new Object[]{0, ""});
        integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/service/balance/integrationToClientLinking/after/balance_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, ошибка на стороне баланса")
    void failIntegrationToClientLinking() throws XmlRpcException {
        when(balance2.LinkIntegrationToClient(OPERATOR_UID, linkIntegrationToClientStructure()))
            .thenReturn(new Object[]{1, "Balance LinkIntegrationToClient error"});
        softly.assertThatThrownBy(() -> integrationToClientLinkingTaskProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage(
                "Привязка конфигурации интеграции к клиенту завершилась с ошибкой: Error returned from Balance: " +
                    "(1) Balance LinkIntegrationToClient error"
            );
    }
}
