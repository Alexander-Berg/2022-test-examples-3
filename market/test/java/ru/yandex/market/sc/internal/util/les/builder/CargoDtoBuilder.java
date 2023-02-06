package ru.yandex.market.sc.internal.util.les.builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierDto;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.InboundShipmentDto;
import ru.yandex.market.logistics.les.dto.KorobyteDto;
import ru.yandex.market.logistics.les.dto.OutboundShipmentDto;
import ru.yandex.market.logistics.les.dto.PointDto;

/**
 * @author: dbryndin
 * @date: 3/21/22
 */
public class CargoDtoBuilder {


    @Builder
    @Setter
    @Getter
    public static class CargoUnitWithGroupsDtoParams {
        private CargoUnitDtoParams cargoUnitDtoParams;
        private CargoUnitGroupDtoParams cargoUnitGroupDtoParams;
    }

    /**
     * @see ru.yandex.market.logistics.les.dto.CargoUnitDto
     */
    @Builder
    @Getter
    @Setter
    public static class CargoUnitDtoParams {
        private String id;

        @Builder.Default
        private Long timestamp = System.currentTimeMillis();

        @Builder.Default
        private String segmentUuid = UUID.randomUUID().toString();

        private String parentId;

        private String cargoUnitGroupId;

        private List<CodeDto> codesCodeDto;

        @Builder.Default
        private KorobyteDto korobyte = new KorobyteDto(1, 1, 3, 1, null, null);

        @Builder.Default
        private Boolean isDamaged = false;

        @Builder.Default
        private BigDecimal assessedCost = BigDecimal.valueOf(10000L);

        private InboundShipmentDto inboundShipment;

        private OutboundShipmentDto outboundShipment;
    }

    /**
     * @see ru.yandex.market.logistics.les.dto.CargoUnitGroupDto
     */
    @Builder
    @Getter
    public static class CargoUnitGroupDtoParams {

        @Builder.Default
        private String id = null;

        @Builder.Default
        private String checkoutId = null;

        @Builder.Default
        private CodeDto checkoutOrderId = null;

        @Builder.Default
        private BigDecimal assessedCost = BigDecimal.valueOf(10000L);

        @Builder.Default
        private Integer cargoUnitCount = 1;

        @Builder.Default
        private Boolean isComplete = true;

        @Builder.Default
        private CargoUnitGroupType type = CargoUnitGroupType.ORDER;
    }


    /**
     * @see ru.yandex.market.logistics.les.dto.InboundShipmentDto
     */
    @Builder
    @Getter
    public static class InboundShipmentDtoParams {
        private Instant dateTime;
        private CarrierDto sender;
        private PointDto source;
    }

    /**
     * @see ru.yandex.market.logistics.les.dto.OutboundShipmentDto
     */
    @Builder
    @Getter
    public static class OutboundShipmentDtoParams {
        @Builder.Default
        private Instant dateTime = Instant.now();
        private CarrierDto recipient;
        private PointDto destination;
    }

}
