package ru.yandex.market.logistics.lrm.converter;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.model.entity.ReturnBoxEntity;
import ru.yandex.market.logistics.lrm.model.entity.ReturnEntity;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnBoxStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;

@DisplayName("Сопоставление статусов для сущностей возвратов")
class ReturnEntitiesStatusesConverterTest extends LrmTest {
    private final ReturnEntitiesStatusesConverter statusesConverter = new ReturnEntitiesStatusesConverter();

    @Test
    @DisplayName("Получение статуса возврата по статусу грузоместа")
    void getReturnStatusFromReturnBoxStatus() {
        //все статусы возврата есть в маппинге
        List<ReturnStatus> mappedStatuses = Arrays.stream(ReturnBoxStatus.values())
            .map(status -> statusesConverter.getReturnStatusFromReturnBoxStatus(new ReturnEntity(), status))
            .collect(Collectors.toList());
        softly.assertThat(mappedStatuses)
            .containsExactlyInAnyOrder(ReturnStatus.values());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получение статуса возврата по статусу грузоместа для многокоробочных статусов")
    void getReturnStatusFromReturnBoxStatusForFulfilmentStatus(
        ReturnBoxStatus returnBoxStatus,
        ReturnStatus returnStatus
    ) {
        List<ReturnBoxEntity> allBoxesReceivedStatus = List.of(new ReturnBoxEntity(), new ReturnBoxEntity());
        allBoxesReceivedStatus.forEach(
            box -> box.setStatus(returnBoxStatus, Clock.systemUTC())
        );

        ReturnEntity returnEntity = new ReturnEntity();
        returnEntity.setStatus(ReturnStatus.IN_TRANSIT, Clock.systemUTC());

        softly.assertThat(statusesConverter.getReturnStatusFromReturnBoxStatus(
                returnEntity.setBoxes(allBoxesReceivedStatus),
                returnBoxStatus
            ))
            .isEqualTo(returnStatus);

        softly.assertThat(statusesConverter.getReturnStatusFromReturnBoxStatus(
                returnEntity.setBoxes(ListUtils.union(
                    allBoxesReceivedStatus,
                    List.of(new ReturnBoxEntity())
                )),
                returnBoxStatus
            ))
            .isEqualTo(ReturnStatus.IN_TRANSIT);
    }

    @Nonnull
    private static Stream<Arguments> getReturnStatusFromReturnBoxStatusForFulfilmentStatus() {
        return Stream.of(
            Arguments.of(
                ReturnBoxStatus.FULFILMENT_RECEIVED,
                ReturnStatus.FULFILMENT_RECEIVED
            ),
            Arguments.of(
                ReturnBoxStatus.READY_FOR_RETURN,
                ReturnStatus.READY_FOR_IM
            ),
            Arguments.of(
                ReturnBoxStatus.DELIVERED,
                ReturnStatus.DELIVERED
            ),
            Arguments.of(
                ReturnBoxStatus.READY_FOR_UTILIZATION,
                ReturnStatus.READY_FOR_UTILIZATION
            ),
            Arguments.of(
                ReturnBoxStatus.UTILIZED,
                ReturnStatus.UTILIZED
            )
        );
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @DisplayName("Получение статуса грузоместа по статусу сегмента")
    void getReturnBoxStatusFromSegmentStatus(
        String displayName,
        Set<ReturnSegmentStatus> unmappedSegmentStatuses,
        Set<ReturnBoxStatus> unmappedBoxStatuses,
        Function<ReturnSegmentStatus, ReturnBoxStatus> getMappedStatusFunction
    ) {
        for (ReturnSegmentStatus segmentStatus : ReturnSegmentStatus.values()) {
            ReturnBoxStatus mappedBoxStatus = getMappedStatusFunction.apply(segmentStatus);

            softly.assertThat(unmappedBoxStatuses).doesNotContain(mappedBoxStatus);
            if (unmappedSegmentStatuses.contains(segmentStatus)) {
                softly.assertThat(mappedBoxStatus).isNull();
            } else {
                softly.assertThat(mappedBoxStatus).isNotNull();
            }
        }

        softly.assertThat(getMappedStatusFunction.apply(null)).isNull();
    }

    @Nonnull
    private static Stream<Arguments> getReturnBoxStatusFromSegmentStatus() {
        ReturnEntitiesStatusesConverter statusesConverter = new ReturnEntitiesStatusesConverter();
        return Stream.of(
            Arguments.of(
                "По статусу первого сегмента: средняя миля",
                Set.of(
                    ReturnSegmentStatus.TRANSIT_PREPARED,
                    ReturnSegmentStatus.CANCELLED
                ),
                Set.of(
                    ReturnBoxStatus.DESTINATION_POINT_RECEIVED,
                    ReturnBoxStatus.READY_FOR_RETURN,
                    ReturnBoxStatus.DELIVERED
                ),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) segmentStatus ->
                    statusesConverter.getReturnBoxStatusFromFirstSegmentStatus(segmentStatus, false)
            ),
            Arguments.of(
                "По статусу первого сегмента: последняя миля",
                Set.of(
                    ReturnSegmentStatus.EXPIRED,
                    ReturnSegmentStatus.CANCELLED
                ),
                Set.of(
                    ReturnBoxStatus.RECEIVED,
                    ReturnBoxStatus.IN_TRANSIT,
                    ReturnBoxStatus.EXPIRED
                ),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) segmentStatus ->
                    statusesConverter.getReturnBoxStatusFromFirstSegmentStatus(segmentStatus, true)
            ),
            Arguments.of(
                "По статусу сегмента СЦ: средняя миля",
                Set.of(
                    ReturnSegmentStatus.EXPIRED,
                    ReturnSegmentStatus.CANCELLED
                ),
                Set.of(
                    ReturnBoxStatus.RECEIVED,
                    ReturnBoxStatus.EXPIRED
                ),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) segmentStatus ->
                    statusesConverter.getReturnBoxStatusFromScSegmentStatus(segmentStatus, false, false)
            ),
            Arguments.of(
                "По статусу сегмента СЦ: последняя миля",
                Set.of(
                    ReturnSegmentStatus.EXPIRED,
                    ReturnSegmentStatus.CANCELLED
                ),
                Set.of(
                    ReturnBoxStatus.RECEIVED,
                    ReturnBoxStatus.IN_TRANSIT,
                    ReturnBoxStatus.EXPIRED
                ),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) segmentStatus ->
                    statusesConverter.getReturnBoxStatusFromScSegmentStatus(segmentStatus, true, false)
            ),
            Arguments.of(
                "По статусу сегмента СЦ: последняя миля в утилизацию",
                Set.of(
                    ReturnSegmentStatus.EXPIRED,
                    ReturnSegmentStatus.CANCELLED
                ),
                Arrays.stream(ReturnBoxStatus.values())
                    .filter(
                        status -> !Set.of(
                                ReturnBoxStatus.CREATED,
                                ReturnBoxStatus.DESTINATION_POINT_RECEIVED,
                                ReturnBoxStatus.READY_FOR_UTILIZATION,
                                ReturnBoxStatus.UTILIZED
                            )
                            .contains(status)
                    )
                    .collect(Collectors.toSet()),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) segmentStatus ->
                    statusesConverter.getReturnBoxStatusFromScSegmentStatus(segmentStatus, false, true)
            ),
            Arguments.of(
                "По статусу сегмента ФФ",
                Arrays.stream(ReturnSegmentStatus.values())
                    .filter(status -> status != ReturnSegmentStatus.IN)
                    .collect(Collectors.toSet()),
                Arrays.stream(ReturnBoxStatus.values())
                    .filter(status -> status != ReturnBoxStatus.FULFILMENT_RECEIVED)
                    .collect(Collectors.toSet()),
                (Function<ReturnSegmentStatus, ReturnBoxStatus>) statusesConverter::getReturnBoxStatusForFulfillment
            )
        );
    }
}
