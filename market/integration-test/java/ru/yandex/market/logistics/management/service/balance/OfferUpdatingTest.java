package ru.yandex.market.logistics.management.service.balance;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.type.TaxationSystem;
import ru.yandex.market.logistics.management.exception.BalanceException;
import ru.yandex.market.logistics.management.queue.model.BalanceOfferUpdatePayload;
import ru.yandex.market.logistics.management.queue.processor.billing.BalanceOfferUpdateProcessor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.management.service.balance.utills.BalanceEntityUtils.updateOfferStructure;

@DatabaseSetup("/data/service/balance/updateoffer/before/before.xml")
public class OfferUpdatingTest extends AbstractContextualTest {
    private static final String OPERATOR_UID = "123456";
    private static final String CONTRACT_ID = "133";
    private static final BalanceOfferUpdatePayload PAYLOAD = new BalanceOfferUpdatePayload(
        "",
        1L,
        TaxationSystem.PATENT
    );

    @Autowired
    private BalanceOfferUpdateProcessor balanceOfferUpdateProcessor;

    @Autowired
    private Balance2 balance2;

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/createperson/before/without_uid.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить договор, так как нет passportUid")
    void failedOfferUpdatingWithoutUid() {
        softly.assertThatThrownBy(() -> balanceOfferUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно обновить договор для партнера id=1: поле passportUid не заполнено");
    }

    @Test
    @DatabaseSetup(
        value = "/data/service/balance/updateoffer/before/without_offer.xml",
        type = DatabaseOperation.REFRESH
    )
    @DisplayName("Не можем обновить договор, так как договора нет")
    void failedOfferUpdatingWithoutOffer() {
        softly.assertThatThrownBy(() -> balanceOfferUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно обновить договор для партнера id=1: контракт не создан");
    }

    @Test
    @DisplayName("Не можем обновить договор, так как нет изменений")
    void failedOfferUpdatingInvalidPayload() {
        softly.assertThatThrownBy(() -> balanceOfferUpdateProcessor.processPayload(
            new BalanceOfferUpdatePayload(
                "",
                1L,
                null
            )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Невозможно обновить договор для партнера id=1: поля не изменены");
    }

    @Test
    @DisplayName("Успешное обновление договора")
    void successOfferUpdating() throws XmlRpcException {
        balanceOfferUpdateProcessor.processPayload(PAYLOAD);
        verify(balance2).UpdateContract(OPERATOR_UID, CONTRACT_ID, updateOfferStructure());
    }

    @Test
    @DisplayName("Обновление договора - ошибка баланса")
    void balanceErrorOfferUpdating() throws XmlRpcException {
        when(balance2.UpdateContract(OPERATOR_UID, CONTRACT_ID, updateOfferStructure()))
            .thenThrow(new XmlRpcException("Invalid client"));

        softly.assertThatThrownBy(() -> balanceOfferUpdateProcessor.processPayload(PAYLOAD))
            .isInstanceOf(BalanceException.class)
            .hasRootCauseInstanceOf(XmlRpcException.class)
            .hasRootCauseMessage("Invalid client");
    }
}
