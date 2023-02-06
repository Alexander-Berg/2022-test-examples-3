package ru.yandex.market.logistics.lom.admin.converter;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.admin.enums.AdminOrderStatus;
import ru.yandex.market.logistics.lom.admin.enums.AdminSegmentStatus;
import ru.yandex.market.logistics.lom.converter.EnumConverter;
import ru.yandex.market.logistics.lom.entity.enums.OrderStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;

import static ru.yandex.market.logistics.lom.admin.converter.AdminOrderStatusConverter.COMMON_STATUSES;
import static ru.yandex.market.logistics.lom.admin.converter.AdminOrderStatusConverter.DS_STATUSES;
import static ru.yandex.market.logistics.lom.admin.converter.AdminOrderStatusConverter.FF_STATUSES;

public class AdminOrderStatusConverterTest extends AbstractTest {
    private final EnumConverter enumConverter = new EnumConverter();
    private final AdminOrderStatusConverter converter = new AdminOrderStatusConverter(enumConverter);

    @DisplayName("Для всякого статуса в админке существует маппинг во внутренний статус")
    @ParameterizedTest
    @EnumSource(AdminOrderStatus.class)
    void allAdminStatusesHaveMappingToInnerStatuses(AdminOrderStatus status) {
        OrderStatus orderStatus = converter.toOrderStatus(status);
        softly.assertThat(orderStatus).isNotNull();
        softly.assertThat(orderStatus.name()).isEqualTo(status.name());
    }

    @DisplayName("Для всякого статуса сегмента в админке существует маппинг во внутренний статус")
    @ParameterizedTest
    @EnumSource(AdminSegmentStatus.class)
    void allAdminSegmentStatusesHaveMappingToInnerStatuses(AdminSegmentStatus status) {
        Pair<Set<SegmentType>, SegmentStatus> orderStatus = converter.toSegmentStatus(status);
        softly.assertThat(orderStatus).isNotNull();
    }

    @DisplayName("Среди DS_STATUSES, FF_STATUSES и COMMON_STATUSES перечислены все SegmentStatus'ы")
    @Test
    void allTestsStatusesListed() {
        Set<SegmentStatus> testStatuses = Stream.of(DS_STATUSES, FF_STATUSES, COMMON_STATUSES)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
        softly.assertThat(testStatuses).containsExactlyInAnyOrder(SegmentStatus.values());
    }

    @DisplayName("Для всякого статуса сегмента существует маппинг в админский статус")
    @ParameterizedTest
    @EnumSource(SegmentStatus.class)
    void allSegmentStatusesHaveMappingToAdminStatuses(SegmentStatus status) {
        if (DS_STATUSES.contains(status)) {
            checkDsSegmentStatus(status);
        } else if (FF_STATUSES.contains(status)) {
            checkFfSegmentStatus(status);
        } else {
            checkDsSegmentStatus(status);
            checkFfSegmentStatus(status);
        }
    }

    private void checkDsSegmentStatus(SegmentStatus status) {
        softly.assertThat(converter.toDisplay(SegmentType.MOVEMENT, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.COURIER, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.PICKUP, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.POST, status)).isNotNull();
    }

    private void checkFfSegmentStatus(SegmentStatus status) {
        softly.assertThat(converter.toDisplay(SegmentType.SORTING_CENTER, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.FULFILLMENT, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.SUPPLIER, status)).isNotNull();
        softly.assertThat(converter.toDisplay(SegmentType.NO_OPERATION, status)).isNotNull();
    }

    @DisplayName("Для всякого статуса существует маппинг в админский статус")
    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "PROCESSING", mode = EnumSource.Mode.EXCLUDE)
    void allOrderStatusesHaveMappingToAdminStatuses(OrderStatus status) {
        AdminOrderStatus adminStatus = converter.toDisplay(status);
        softly.assertThat(Optional.ofNullable(adminStatus))
            .hasValueSatisfying(s -> softly.assertThat(s.name()).isEqualTo(status.name()));
    }
}
