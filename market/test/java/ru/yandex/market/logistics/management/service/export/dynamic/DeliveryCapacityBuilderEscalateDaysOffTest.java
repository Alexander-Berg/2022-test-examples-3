package ru.yandex.market.logistics.management.service.export.dynamic;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.util.UnitTestUtil;

class DeliveryCapacityBuilderEscalateDaysOffTest extends AbstractTest {
    public static final LocalDate D1 = LocalDate.of(2020, 12, 22);
    public static final LocalDate D2 = D1.plusDays(1);
    public static final LocalDate D3 = D1.plusDays(2);

    private final RegionService regionService = UnitTestUtil.getRegionTree();
    private final DeliveryCapacityBuilderImpl builder = new DeliveryCapacityBuilderImpl(
        null,
        LocalDate.of(2020, 1, 1),
        regionService,
        null,
        null
    );

    @Test
    public void empty() {
        Map<Integer, Map<DeliveryCapacityTypeDto, List<LocalDate>>> input = new HashMap<>();
        Map<Integer, Map<DeliveryCapacityTypeDto, Collection<LocalDate>>> result =
            builder.escalateDayOffsToChildren(input);
        softly.assertThat(result).isEmpty();
    }

    @Test
    public void nothingToEscalate() {
        Map<Integer, Map<DeliveryCapacityTypeDto, List<LocalDate>>> input = Map.of(
            3, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D1)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2))
        );
        Map<Integer, Map<DeliveryCapacityTypeDto, Collection<LocalDate>>> result =
            builder.escalateDayOffsToChildren(input);
        softly.assertThat(result).isEqualTo(Map.of(
            3, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D2))
        ));
    }

    @Test
    public void escalateSimple() {
        Map<Integer, Map<DeliveryCapacityTypeDto, List<LocalDate>>> input = Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D1)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2))
        );
        Map<Integer, Map<DeliveryCapacityTypeDto, Collection<LocalDate>>> result =
            builder.escalateDayOffsToChildren(input);
        softly.assertThat(result).isEqualTo(Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2))
        ));
    }

    @Test
    public void escalateMultiple() {
        Map<Integer, Map<DeliveryCapacityTypeDto, List<LocalDate>>> input = Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D1)),
            3, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2)),
            20279, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D3))
        );
        Map<Integer, Map<DeliveryCapacityTypeDto, Collection<LocalDate>>> result =
            builder.escalateDayOffsToChildren(input);
        softly.assertThat(result).isEqualTo(Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1)),
            3, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2)),
            20279, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2, D3))
        ));
    }

    @Test
    public void escalateDifferentTypes() {
        Map<Integer, Map<DeliveryCapacityTypeDto, List<LocalDate>>> input = Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D1)),
            3, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, List.of(D2)),
            20279, Map.of(DeliveryCapacityTypeDto.COURIER, List.of(D3))
        );
        Map<Integer, Map<DeliveryCapacityTypeDto, Collection<LocalDate>>> result =
            builder.escalateDayOffsToChildren(input);
        softly.assertThat(result).isEqualTo(Map.of(
            225, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1)),
            3, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2)),
            17, Map.of(DeliveryCapacityTypeDto.ALL, Set.of(D1, D2)),
            20279, Map.of(DeliveryCapacityTypeDto.COURIER, Set.of(D3), DeliveryCapacityTypeDto.ALL, Set.of(D1, D2))
        ));
    }
}
