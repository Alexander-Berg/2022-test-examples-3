package ru.yandex.market.tpl.core.service.sqs;

import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.service.sqs.processor.CancelClientReturnValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class CancelClientReturnValidatorTest {

    @Test
    void testAllFieldsAreValid() {
        var event = LrmReturnAtClientAddressCancelEventGenerateService.generateEvent(
                LrmReturnAtClientAddressCancelEventGenerateService.LrmReturnAtClientAdressCancelEventGenerateParam.builder().build()
        );
        assertDoesNotThrow(() -> CancelClientReturnValidator.validateEventNotNull(event));
    }

    @ParameterizedTest
    @MethodSource("forbiddenStatuses")
    void testInvalidClientReturnStatus(ClientReturnStatus wrongStatus) {
        ClientReturn clientReturn = new ClientReturn();
        clientReturn.setStatus(wrongStatus);

        var exception = assertThrows(TplIllegalStateException.class,
                () -> CancelClientReturnValidator.validateClientReturnEligible(clientReturn));
        assertThat(exception.getMessage()).contains(wrongStatus.name());
    }

    private static Stream<ClientReturnStatus> forbiddenStatuses() {
        return ClientReturnStatus.CANNOT_CANCEL_CLIENT_RETURN.stream();
    }
}
