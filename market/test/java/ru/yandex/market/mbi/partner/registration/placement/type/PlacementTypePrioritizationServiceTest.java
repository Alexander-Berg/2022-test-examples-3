package ru.yandex.market.mbi.partner.registration.placement.type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.model.PlacementRegionLogisticPointInfo;
import ru.yandex.market.mbi.partner.registration.model.PlacementRegionLogisticPointInfo.NearestLogisticPointInfo;
import ru.yandex.market.mbi.partner.registration.placement.type.PlacementTypePrioritizationService.PlacementPrioritizationData;

import static ru.yandex.mj.generated.server.model.AvailablePartnerPlacementType.DBS;
import static ru.yandex.mj.generated.server.model.AvailablePartnerPlacementType.EXPRESS;
import static ru.yandex.mj.generated.server.model.AvailablePartnerPlacementType.FBS;
import static ru.yandex.mj.generated.server.model.AvailablePartnerPlacementType.FBY;

public class PlacementTypePrioritizationServiceTest extends AbstractFunctionalTest {

    @Autowired
    private PlacementTypePrioritizationService tested;

    @DisplayName("Тест приоритезации моделей размещения")
    @ParameterizedTest(name = "{0}")
    @MethodSource(value = "getPrioritizationParams")
    void testPrioritization(
        String displayName,
        List<PlacementPrioritizationData> after
    ) {
        List<PlacementPrioritizationData> before = new ArrayList<>(after);
        Collections.shuffle(before);

        Assertions.assertThat(tested.prioritize(before))
            .isEqualTo(after);
    }

    public static Stream<Arguments> getPrioritizationParams() {
        return Stream.of(
            Arguments.of(
                "Самый приоритетный FBY cо складом прямо в городе",
                List.of(
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(1)
                        .build()
                    ),
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(1)
                        .build()
                    ),
                    new PlacementPrioritizationData(EXPRESS, true, null),
                    new PlacementPrioritizationData(DBS, true, null)
                )
            ),
            Arguments.of(
                "Самый приоритетный FBY cо складом менее чем 250 км от города",
                List.of(
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(200, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(1)
                        .build()
                    ),
                    new PlacementPrioritizationData(EXPRESS, true, null),
                    new PlacementPrioritizationData(DBS, true, null)
                )
            ),
            Arguments.of(
                "Самый приоритетный FBS, так как FBY недоступен",
                List.of(
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(1)
                        .build()
                    ),
                    new PlacementPrioritizationData(EXPRESS, true, null),
                    new PlacementPrioritizationData(DBS, true, null),
                    new PlacementPrioritizationData(FBY, false, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(200, "", "", null))
                        .build()
                    )
                )
            ),
            Arguments.of(
                "Самый приоритетный FBS, так как FBY вне зоны доступа",
                List.of(
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(1)
                        .build()
                    ),
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(251, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(DBS, true, null),
                    new PlacementPrioritizationData(EXPRESS, false, null)
                )
            ),
            Arguments.of(
                "Самый приоритетный FBS (но ПВЗ в 90 км), так как FBY вне зоны доступа",
                List.of(
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(99, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(251, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(DBS, true, null),
                    new PlacementPrioritizationData(EXPRESS, false, null)
                )
            ),
            Arguments.of(
                "Самый приоритетный EXPRESS",
                List.of(
                    new PlacementPrioritizationData(EXPRESS, true, null),
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(251, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(101, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(DBS, true, null)
                )
            ),
            Arguments.of(
                "Самый приоритетный DBS",
                List.of(
                    new PlacementPrioritizationData(DBS, true, null),
                    new PlacementPrioritizationData(FBY, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(251, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(FBS, true, PlacementRegionLogisticPointInfo.builder()
                        .withNumberOfWarehousesInRegion(0)
                        .withNearestLogisticPoint(new NearestLogisticPointInfo(101, "", "", null))
                        .build()
                    ),
                    new PlacementPrioritizationData(EXPRESS, false, null)
                )
            )
        );
    }

}
