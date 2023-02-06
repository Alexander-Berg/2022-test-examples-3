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
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.createPersonRequest;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner_subtypes.xml")
@DatabaseSetup("/data/service/balance/createperson/before/partner.xml")
class PersonCreationTest extends AbstractContextualTest {
    private static final long PARTNER_ID = 1L;
    private static final String OPERATOR_UID = "123456";
    private static final int PERSON_ID = 2;
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", PARTNER_ID);

    @Autowired
    private AbstractPartnerBillingRegistrationProcessor<EntityIdPayload, EntityIdPayload> personCreationProcessor;

    @Autowired
    private Balance2 balance2;

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/person_already_exists.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/person_already_exists.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как плательщик уже существует")
    void failedPersonCreationPersonExists() {
        personCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/without_uid.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/partner_without_uid.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет passportUid")
    void failedPersonCreationWithoutUid() {
        softly.assertThatThrownBy(() -> personCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно создать плательщика для партнера id=1: поле passportUid не заполнено");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_type_not_ds.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/partner_type_not_ds.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет тип партнёра не СД")
    void failedPersonCreationForSc() {
        personCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/partner_subtype_not_pickup_point.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/partner_subtype_not_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет подтип партнёра не партнёрская ПВЗ")
    void failedPersonCreationForNotPickupPoint() {
        personCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_required_fields.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/no_required_fields.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как legal info не содержит всех необходимых полей")
    void failedPersonCreationLegalInfoNotFull() {
        softly.assertThatThrownBy(() -> personCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Не удалось создать плательщика, так как следующие поля не заполнены: " +
                    "[name, longName, phone, email, postCode, inn, bik, account, legalAddress, postAddress]"
            );
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_legal_info.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/no_legal_info.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет legal info")
    void failedPersonCreationNoLegalInfo() {
        softly.assertThatThrownBy(() -> personCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Для партнера id=1 не найден LegalInfo");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_legal_form.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/no_legal_form.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Не можем создать плательщика, так как нет legal form")
    void failedPersonCreationNoLegalForm() {
        softly.assertThatThrownBy(() -> personCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Для партнера id=1 не найден LegalForm");
    }

    @Test
    @DisplayName("Успешное создание плательщика")
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successPersonCreation() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID, createPersonRequest())).thenReturn(PERSON_ID);
        personCreationProcessor.processPayload(PAYLOAD);
    }

    @Test
    @DisplayName("Создание плательщика — ошибка при создании в Балансе")
    @ExpectedDatabase(
        value = "/data/service/balance/createperson/after/balance_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void failedPersonCreationBalanceError() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID, createPersonRequest()))
            .thenThrow(new XmlRpcException("Invalid client"));
        softly.assertThatThrownBy(() -> personCreationProcessor.processPayload(PAYLOAD))
            .isInstanceOf(PartnerBillingRegistrationException.class)
            .hasMessage("Не удалось создать плательщика: org.apache.xmlrpc.XmlRpcException: Invalid client");
    }
}
