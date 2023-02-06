package ru.yandex.market.logistics.nesu.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.logistics.management.entity.request.partner.CreatePartnerDto;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;

import static ru.yandex.market.logistics.nesu.model.LmsFactory.createDropshipPartnerDto;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createPartner;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createSettingsMethodDtos;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class PartnerTestUtils {

    public static final String DROPSHIP_SHOP_ID = "1";
    public static final long SUPPLIER_SHOP_ID = 3;
    public static final long DROPSHIP_PARTNER_ID = 1;
    public static final long SUPPLIER_PARTNER_ID = 123;
    public static final long SUPPLIER_BUSINESS_ID = 12;
    public static final long WAREHOUSE_ID = 29L;
    public static final int WAREHOUSE_LOCATION_ID = 2;
    public static final String GET_STOCKS = "getStocks";
    public static final String GET_ITEMS = "getReferenceItems";
    public static final List<Pair<String, String>> DROPSHIP_METHODS = List.of(
        Pair.of(GET_STOCKS, "reference/1/getStocks"),
        Pair.of(GET_ITEMS, "reference/1/getReferenceItems"),
        Pair.of("createOrder", "orders/createOrder"),
        Pair.of("getOrdersStatus", "orders/getOrdersStatus"),
        Pair.of("getOrderHistory", "orders/getOrderHistory"),
        Pair.of("getOrder", "orders/getOrder"),
        Pair.of("cancelOrder", "orders/cancelOrder"),
        Pair.of("updateCourier", "orders/updateCourier"),
        Pair.of("putOutbound", "outbounds/putOutbound"),
        Pair.of("getOutboundStatus", "outbounds/getOutboundStatus"),
        Pair.of("getOutboundStatusHistory", "outbounds/getOutboundStatusHistory"),
        Pair.of("getOutbound", "outbounds/getOutbound")
    );

    public static final Set<DeliveryServiceType> DROPSHIP = Set.of(DeliveryServiceType.DROPSHIP);
    public static final Set<DeliveryServiceType> CROSSDOCK = Set.of(DeliveryServiceType.CROSSDOCK);

    public static final PartnerResponse DROPSHIP_PARTNER_WITH_PLATFORM_CLIENT = createPartner(
        DROPSHIP_PARTNER_ID,
        1,
        "DropShip_Partner",
        "DropShip_Partner_Readable",
        PartnerType.DROPSHIP,
        true
    );

    public static final PartnerResponse SUPPLIER_PARTNER_WITH_PLATFORM_CLIENT = createPartner(
        SUPPLIER_PARTNER_ID,
        123,
        "CrossDock_Partner",
        "CrossDock_Partner_Readable",
        PartnerType.SUPPLIER,
        true
    );

    public static final PartnerResponse DROPSHIP_PARTNER_WITHOUT_PLATFORM_CLIENT = createPartner(
        DROPSHIP_PARTNER_ID,
        1,
        "DropShip_Partner",
        "DropShip_Partner_Readable",
        PartnerType.DROPSHIP,
        false
    );

    public static final CreatePartnerDto DROPSHIP_PARTNER_REQUEST = createDropshipPartnerDto(
        1,
        "DropShip_Partner",
        "DropShip_Partner_Readable",
        41L
    );

    @Nonnull
    public static List<SettingsMethodDto> createDropshipMethodsExcept(String... methods) {
        Set<String> toRemove = Set.of(methods);
        List<String> resultMethods = DROPSHIP_METHODS.stream()
            .map(Pair::getKey)
            .filter(method -> !toRemove.contains(method))
            .collect(Collectors.toList());
        return createSettingsMethodDtos(resultMethods);
    }

    @Nonnull
    public static LogisticsPointResponse createWarehouseResponse() {
        return LogisticsPointResponse.newBuilder()
            .id(WAREHOUSE_ID)
            .address(
                Address.newBuilder()
                    .locationId(WAREHOUSE_LOCATION_ID)
                    .build()
            )
            .build();
    }

    @Nonnull
    public static PartnerSettingDto createPartnerSettingsDto() {
        return PartnerSettingDto.newBuilder()
            .locationId(WAREHOUSE_LOCATION_ID)
            .stockSyncEnabled(false)
            .autoSwitchStockSyncEnabled(true)
            .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .korobyteSyncEnabled(false)
            .build();
    }

    @Nonnull
    public static PartnerSettingDto createDropshipPartnerSettingsDto() {
        return PartnerSettingDto.newBuilder()
            .locationId(WAREHOUSE_LOCATION_ID)
            .stockSyncEnabled(false)
            .autoSwitchStockSyncEnabled(true)
            .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .korobyteSyncEnabled(false)
            .autoItemRemovingEnabled(true)
            .updateCourierNeeded(true)
            .build();
    }
}
