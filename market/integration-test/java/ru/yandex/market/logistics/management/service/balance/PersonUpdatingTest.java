package ru.yandex.market.logistics.management.service.balance;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.exception.BalanceException;
import ru.yandex.market.logistics.management.queue.model.EntityIdPayload;
import ru.yandex.market.logistics.management.queue.processor.billing.BalancePersonUpdateProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.updatePersonRequest;

@DatabaseSetup("/data/service/balance/createperson/before/legal_info.xml")
@DatabaseSetup("/data/service/balance/updateperson/before/before.xml")
public class PersonUpdatingTest extends AbstractContextualTest {
    private static final String OPERATOR_UID = "123456";
    private static final EntityIdPayload PAYLOAD = new EntityIdPayload("", 1L);

    @Autowired
    private BalancePersonUpdateProcessor balancePersonUpdateProcessor;

    @Autowired
    private Balance2 balance2;

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/updateperson/before/without_person.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить плательщика, так как плательщик не создан")
    void failedPersonUpdatingWithoutPerson() {
        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Плательщик для партнёра id=1 не создан");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/without_uid.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить плательщика, так как нет passportUid")
    void failedPersonUpdatingWithoutUid() {
        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно обновить плательщика для партнера id=1: поле passportUid не заполнено");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_legal_info.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить плательщика, так как нет legalInfo")
    void failedPersonUpdatingWithoutLegalInfo() {
        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Для партнера id=1 не найден LegalInfo");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_legal_form.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить плательщика, так как нет legalForm")
    void failedPersonUpdatingWithoutLegalForm() {
        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Для партнера id=1 не найден LegalForm");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/no_required_fields.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить плательщика, так как legal info не содержит всех необходимых полей")
    void failedPersonUpdatingLegalInfoNotFull() {
        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage(
                "Не удалось создать плательщика, так как следующие поля не заполнены: " +
                    "[name, longName, phone, email, postCode, inn, bik, account, legalAddress, postAddress]"
            );
    }

    @Test
    @DisplayName("Успешное обновление плательщика")
    void successPersonUpdating() throws XmlRpcException {
        balancePersonUpdateProcessor.processPayload(PAYLOAD);
        verify(balance2).CreatePerson(OPERATOR_UID, updatePersonRequest());
    }

    @Test
    @DisplayName("Обновление плательщика - ошибка на стороне Баланса")
    void personUpdatingBalanceError() throws XmlRpcException {
        when(balance2.CreatePerson(OPERATOR_UID, updatePersonRequest()))
            .thenThrow(new XmlRpcException("Invalid client"));

        softly.assertThatThrownBy(() -> balancePersonUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(BalanceException.class)
            .hasRootCauseInstanceOf(XmlRpcException.class)
            .hasRootCauseMessage("Invalid client");
    }
}
