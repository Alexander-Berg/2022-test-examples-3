package ru.yandex.market.tpl.core.domain.idempotence;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.domain.base.IdempotencyManager;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class IdempotencyManagerImplTest extends TplAbstractTest {

    private final IdempotencyManager idempotencyManager;

    @Test
    void isAlreadyCalled() {

        UserShiftCommand.PayOrder command = new UserShiftCommand.PayOrder(
                123L,
                456L,
                789L,
                OrderPaymentType.CARD,
                UUID.randomUUID()
        );

        assertThat(
                idempotencyManager.isAlreadyCalled(
                        command,
                        command.getIdempotencyKey()
                )
        ).isFalse();

        assertThat(
                idempotencyManager.isAlreadyCalled(
                        command,
                        command.getIdempotencyKey()
                )
        ).isTrue();

    }
}
