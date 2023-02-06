package ru.yandex.market.ff4shops.factory;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeDto;
import ru.yandex.market.logistics.management.entity.response.possibleOrderChanges.PossibleOrderChangeGroup;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeMethod;
import ru.yandex.market.logistics.management.entity.type.PossibleOrderChangeType;

@ParametersAreNonnullByDefault
public class LmsFactory {

    private LmsFactory() {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public static PossibleOrderChangeDto.PossibleOrderChangeDtoBuilder possibleOrderChange(
        Boolean canChangeItems,
        Long partnerId
    ) {
        return PossibleOrderChangeDto.builder()
            .id(1L)
            .partnerId(partnerId)
            .type(PossibleOrderChangeType.ORDER_ITEMS)
            .method(PossibleOrderChangeMethod.PARTNER_API)
            .enabled(canChangeItems);
    }

    @Nonnull
    public static PossibleOrderChangeGroup possibleOrderChangeGroup(long partnerId, boolean canChangeItems) {
        return new PossibleOrderChangeGroup(
            partnerId,
            List.of(possibleOrderChange(canChangeItems, partnerId).build())
        );
    }
}
