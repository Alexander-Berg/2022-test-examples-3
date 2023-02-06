package ru.yandex.market.tpl.core.domain.clientreturn;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class ClientReturnSmokeTest extends TplAbstractTest {

    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;

    @Test
    void successfulCreateClientReturn() {
        var pvzClientReturn = clientReturnGenerator.generate();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        assertThat(clientReturn).isNotNull();
        assertThat(pvzClientReturn).isNotNull();
        assertThat(clientReturn.getItems()).isNotEmpty();
        assertThat(clientReturn.getItems().get(0).getName()).isNotNull();
        assertThat(clientReturn.getItems().get(0).getDimensions()).isNotNull();
        assertThat(clientReturn.getItems().get(0).getBuyerPrice()).isNotNull();
        assertThat(clientReturn.getExternalReturnId()).isNotNull();
        assertThat(clientReturn.getCreatedAt()).isNotNull();
        assertThat(clientReturn.getUpdatedAt()).isNotNull();
        assertThat(clientReturn.getArriveIntervalFrom()).isNotNull();
        assertThat(clientReturn.getArriveIntervalTo()).isNotNull();
        assertThat(clientReturn.getLogisticRequestPointFrom()).isNotNull();
        assertThat(clientReturn.getLogisticRequestPointFrom().getId()).isNotNull();
        assertThat(clientReturn.getExternalOrderId()).isNotNull();
        assertThat(clientReturn.getCheckouterReturnId()).isNotNull();
    }

    @Test
    void successfulCreateMultipleReturns() {
        clientReturnGenerator.generate();
        clientReturnGenerator.generate();
        clientReturnGenerator.generateReturnFromClient();
        clientReturnGenerator.generateReturnFromClient();

        var returns = clientReturnRepository.findAll();
        assertThat(returns).hasSize(4);
    }
}
