package ru.yandex.market.logistics.nesu.enums.orderstatus;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.lom.model.enums.SegmentStatus;
import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.dto.enums.DaasOrderStatus;
import ru.yandex.market.logistics.nesu.utils.status.StatusConstants;

public class StatusConstantsTest extends AbstractTest {
    @Test
    @DisplayName("В DELIVERY-блоках StatusConstants перечислены все DELIVERY-статусы")
    void deliveryUnion() {
        Set<SegmentStatus> allDeliveryStatuses = Arrays.stream(DaasOrderStatus.values())
            .filter(
                orderStatus -> orderStatus.getPartnerTypes() != null
                    && orderStatus.getPartnerTypes().contains(PartnerType.DELIVERY)
            )
            .map(DaasOrderStatus::getSegmentStatus)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        softly.assertThat(StatusConstants.DELIVERY_STATUSES).containsExactlyInAnyOrderElementsOf(allDeliveryStatuses);
    }

    @Test
    @DisplayName("Статусы в DELIVERY-блоках StatusConstants не пересекаются")
    void deliveryIntersection() {
        softly.assertThat(
            StreamEx.of(StatusConstants.DELIVERY_BEFORE_PROCESSING)
                .append(StatusConstants.DELIVERY_PROCESSING_DELIVERY_TO_RECIPIENT)
                .append(StatusConstants.DELIVERY_PROCESSING_DELIVERY_TO_DELIVERY_SERVICE)
                .append(StatusConstants.RETURN_DELIVERY)
                .append(StatusConstants.DELIVERY_CANCEL)
                .append(StatusConstants.DELIVERY_ERROR)
                .append(StatusConstants.DELIVERY_UNKNOWN)
                .toList()
                .size()
        )
            .isEqualTo(StatusConstants.DELIVERY_STATUSES.size());
    }
}
