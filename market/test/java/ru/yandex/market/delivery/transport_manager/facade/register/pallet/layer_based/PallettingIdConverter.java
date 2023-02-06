package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PallettingId;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class PallettingIdConverter {

    public static List<PallettingId> toPalletingIds(long... ids) {
        return toPalletingIds(CountType.FIT, ids);
    }

    public static List<PallettingId> toPalletingIds(CountType countType, long... ids) {
        return Arrays
            .stream(ids)
            .mapToObj(id -> new PallettingId(id, 0, countType))
            .collect(Collectors.toList());
    }
}
