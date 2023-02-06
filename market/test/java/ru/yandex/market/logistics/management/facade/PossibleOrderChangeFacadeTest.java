package ru.yandex.market.logistics.management.facade;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.dto.front.possibleOrderChange.PossibleOrderChangeCreateDto;
import ru.yandex.market.logistics.management.domain.dto.front.possibleOrderChange.PossibleOrderChangeUpdateDto;
import ru.yandex.market.logistics.management.domain.entity.type.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.management.exception.BadRequestException;

public class PossibleOrderChangeFacadeTest extends AbstractTest {

    private PossibleOrderChangeFacade possibleOrderChangeFacade = new PossibleOrderChangeFacade(
        null,
        null,
        null,
        null,
        null,
        null
    );

    @Test
    public void shouldCheckCheckpointRangeOnCreate() {
        softly.assertThatThrownBy(() -> {
            possibleOrderChangeFacade.create(
                PossibleOrderChangeCreateDto.newBuilder()
                    .method(PossibleOrderChangeMethod.PARTNER_SITE)
                    .checkpointStatusFrom(49)
                    .checkpointStatusTo(10)
                    .build()
            );
        }).isInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldCheckCheckpointRangeOnUpdate() {
        softly.assertThatThrownBy(() -> {
            possibleOrderChangeFacade.update(
                1L,
                PossibleOrderChangeUpdateDto.newBuilder()
                    .method(PossibleOrderChangeMethod.PARTNER_PHONE)
                    .checkpointStatusFrom(49)
                    .checkpointStatusTo(10)
                    .build()
            );
        }).isInstanceOf(BadRequestException.class);
    }
}
