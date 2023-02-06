package ru.yandex.market.logistics.lom.service.validation;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.validation.OrderExternalValidationAndEnrichingService;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.TariffDto;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPayload;

public abstract class AbstractOrderExternalValidationAndEnrichingServiceTest extends AbstractContextualTest {
    protected static final Instant FIXED_TIME = Instant.parse("2021-01-01T00:00:00.00Z");

    protected static final Long ORDER_ID = 1L;
    protected static final Long PICKUP_POINT_ID = 10L;
    protected static final Long PICKUP_POINT_PARTNER_ID = 123L;
    protected static final Long POINT_ID_WITHOUT_PARTNER_ID = 400L;
    protected static final Long WAREHOUSE_ID = 11L;
    protected static final Long MARKET_ID_FROM = 111L;
    protected static final Long PICKUP_POINT_MARKET_ID = 100L;
    protected static final Long PARTNER_MARKET_ID = 3333L;
    protected static final Long RETURN_PARTNER_MARKET_ID = 6666L;
    protected static final Long SORTING_CENTER_MARKET_ID = 499L;
    protected static final Long NON_EXISTENT_RETURN_WAREHOUSE = 683L;
    protected static final Long PARTNER_ID = 48L;
    protected static final Long RETURN_PARTNER = 1111L;
    protected static final Long RETURN_PARTNER_FF = 2222L;
    protected static final Long RETURN_PARTNER_FF_MARKET_ID = 700L;
    protected static final Long RETURN_WAREHOUSE = 6831L;
    protected static final Long DROPSHIP_EXPRESS_PARTNER = 4444L;
    protected static final Long GO_COURIER_PARTNER = 4445L;
    protected static final Long UPDATE_INSTANCES_ENABLED_PARTNER = 7777L;
    protected static final Long CAN_UPDATE_SHIPMENT_DATE_ENABLED_PARTNER = 8888L;
    protected static final Long DROPOFF_PARTNER = 7778L;
    protected static final Long SORTING_CENTER_PARTNER = 49L;
    protected static final Long SORTING_CENTER_POINT = 4900L;
    protected static final OrderIdPayload VALIDATION_PAYLOAD = createOrderIdPayload(ORDER_ID, "1");

    @Autowired
    protected OrderExternalValidationAndEnrichingService orderExternalValidationAndEnrichingService;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected MarketIdService marketIdService;

    @Autowired
    protected TarifficatorClient tarifficatorClient;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @BeforeEach
    void setupClock() {
        clock.setFixed(FIXED_TIME, ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, tarifficatorClient, marketIdService, mbiApiClient);
    }

    protected void mockTarifficator() {
        when(tarifficatorClient.getOptionalTariff(100038L))
            .thenReturn(Optional.of(TariffDto.builder().code("tariff_code").id(100038L).build()));
        when(tarifficatorClient.getOptionalTariff(100005L))
            .thenReturn(Optional.of(TariffDto.builder().code("tariff_code").id(100005L).build()));
    }

    protected void mockLmsClientFully() {
        when(lmsClient.getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(Set.of(WAREHOUSE_ID)).build()))
            .thenReturn(List.of(getWarehousePoint()));
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(Set.of(NON_EXISTENT_RETURN_WAREHOUSE))))
            .thenReturn(List.of());
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER))))
            .thenReturn(List.of(getReturnWarehousePoint()));
        when(lmsClient.getLogisticsPoints(logisticsPointFilter(Set.of(RETURN_PARTNER_FF))))
            .thenReturn(List.of(getReturnWarehouseFFPoint()));
        when(
            lmsClient.getLogisticsPoints(
                LogisticsPointFilter.newBuilder()
                    .ids(Set.of(POINT_ID_WITHOUT_PARTNER_ID))
                    .build()
            )
        )
            .thenReturn(List.of(getPointWithoutBothPartnerIdAndMarketId()));

        when(lmsClient.getLogisticsPoint(PICKUP_POINT_ID)).thenReturn(Optional.of(getPickupPoint()));
        when(lmsClient.getLogisticsPoint(RETURN_WAREHOUSE)).thenReturn(Optional.of(getReturnWarehousePoint()));
        when(lmsClient.getLogisticsPoint(SORTING_CENTER_POINT)).thenReturn(Optional.of(getSortingCenterPoint()));

        mockPartners();
    }

    protected void mockPartners() {
        PartnerResponse partner = LmsFactory.createPartnerResponse(PARTNER_ID, PARTNER_MARKET_ID);
        PartnerResponse returnPartner = LmsFactory.createPartnerResponse(
            RETURN_PARTNER,
            "SC",
            PARTNER_MARKET_ID
        );
        PartnerResponse returnFfPartner = LmsFactory.createPartnerResponse(
            RETURN_PARTNER_FF,
            "FF",
            RETURN_PARTNER_FF_MARKET_ID
        );

        List<PartnerResponse> partnerResponseList = List.of(partner, returnPartner);
        List<PartnerResponse> partnerResponseReturnFfList = List.of(partner, returnFfPartner);
        List<PartnerResponse> partnerResponseNonExistentReturnWhList = List.of(partner);

        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, NON_EXISTENT_RETURN_WAREHOUSE))))
            .thenReturn(List.of(partner));
        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER))))
            .thenReturn(partnerResponseList);
        when(lmsClient.searchPartners(partnerFilter(
            Set.of(RETURN_PARTNER, DROPSHIP_EXPRESS_PARTNER, GO_COURIER_PARTNER)
        )))
            .thenReturn(List.of(
                returnPartner,
                LmsFactory.createDropshipExpressPartnerResponse(
                    DROPSHIP_EXPRESS_PARTNER,
                    PARTNER_MARKET_ID,
                    PartnerType.DELIVERY
                ),
                LmsFactory.createPartnerResponse(GO_COURIER_PARTNER, PartnerType.DELIVERY)
                    .marketId(PARTNER_MARKET_ID)
                    .build()
            ));
        when(lmsClient.searchPartners(partnerFilter(Set.of(RETURN_PARTNER, DROPOFF_PARTNER))))
            .thenReturn(List.of(
                returnPartner,
                LmsFactory.createDropoffPartnerResponse(DROPOFF_PARTNER, PARTNER_MARKET_ID, PartnerType.DELIVERY)
            ));
        when(lmsClient.searchPartners(partnerFilter(Set.of(RETURN_PARTNER, UPDATE_INSTANCES_ENABLED_PARTNER))))
            .thenReturn(List.of(
                returnPartner,
                LmsFactory.createUpdateInstancesEnabledPartnerResponse(
                    UPDATE_INSTANCES_ENABLED_PARTNER, PARTNER_MARKET_ID, PartnerType.DELIVERY
                )
            ));
        when(lmsClient.searchPartners(partnerFilter(Set.of(RETURN_PARTNER, CAN_UPDATE_SHIPMENT_DATE_ENABLED_PARTNER))))
            .thenReturn(List.of(
                LmsFactory.createPartnerResponse(
                    RETURN_PARTNER,
                    PARTNER_MARKET_ID,
                    PartnerType.SORTING_CENTER,
                    new PartnerExternalParam(PartnerExternalParamType.CAN_UPDATE_SHIPMENT_DATE.name(), null, "0")
                ),
                LmsFactory.createChangeShipmentDateEnabledPartnerResponse(
                    CAN_UPDATE_SHIPMENT_DATE_ENABLED_PARTNER, PARTNER_MARKET_ID, PartnerType.DELIVERY
                )
            ));
        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, PICKUP_POINT_PARTNER_ID, RETURN_PARTNER))))
            .thenReturn(List.of(
                partner,
                LmsFactory.createPartnerResponse(RETURN_PARTNER, RETURN_PARTNER_MARKET_ID),
                LmsFactory.createPartnerResponse(PICKUP_POINT_PARTNER_ID, PICKUP_POINT_MARKET_ID)
            ));
        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, RETURN_PARTNER_FF))))
            .thenReturn(partnerResponseReturnFfList);

        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, PICKUP_POINT_PARTNER_ID, RETURN_PARTNER_FF))))
            .thenReturn(List.of(
                partner,
                returnFfPartner,
                LmsFactory.createPartnerResponse(PICKUP_POINT_PARTNER_ID, "FF", PICKUP_POINT_MARKET_ID)
            ));

        when(lmsClient.searchPartners(partnerFilter(Set.of(PARTNER_ID, NON_EXISTENT_RETURN_WAREHOUSE))))
            .thenReturn(partnerResponseNonExistentReturnWhList);

        Stream.of(partnerResponseList, partnerResponseReturnFfList, partnerResponseNonExistentReturnWhList)
            .flatMap(Collection::stream)
            .distinct()
            .forEach(r -> when(lmsClient.getPartner(r.getId())).thenReturn(Optional.of(r)));
    }

    protected void mockGetDeliveryInterval(int hourTo) {
        when(lmsClient.getScheduleDay(1L))
            .thenReturn(Optional.of(LmsFactory.createScheduleDayResponse(1L, 10, hourTo)));
    }

    protected void mockSenderGetCredentials() {
        Optional<MarketAccount> marketAccount = Optional.of(MarketIdFactory.marketAccount());
        Stream.of(MARKET_ID_FROM, PICKUP_POINT_MARKET_ID, RETURN_PARTNER_FF_MARKET_ID, SORTING_CENTER_MARKET_ID)
            .forEach(id -> when(marketIdService.findAccountById(id)).thenReturn(marketAccount));
    }

    protected void mockPartnersGetCredentials() {
        List.of(
                Pair.of(
                    PARTNER_MARKET_ID,
                    MarketIdFactory.marketAccount(PARTNER_MARKET_ID, MarketIdFactory.anotherLegalInfoBuilder().build())
                ),
                Pair.of(
                    PICKUP_POINT_MARKET_ID,
                    MarketIdFactory.marketAccount(
                        PICKUP_POINT_MARKET_ID,
                        MarketIdFactory.anotherLegalInfoBuilder().build()
                    )
                ),
                Pair.of(
                    RETURN_PARTNER_MARKET_ID,
                    MarketIdFactory.marketAccount(
                        RETURN_PARTNER_MARKET_ID,
                        MarketIdFactory.anotherLegalInfoBuilder().build()
                    )
                )
            )
            .forEach(t -> when(marketIdService.findAccountById(t.getLeft())).thenReturn(Optional.of(t.getRight())));
    }

    @Nonnull
    protected LogisticsPointResponse getSortingCenterPoint() {
        return LmsFactory.createPickupPointResponseBuilder(SORTING_CENTER_POINT)
            .partnerId(SORTING_CENTER_PARTNER)
            .build();
    }

    @Nonnull
    protected LogisticsPointResponse getPickupPoint() {
        return LmsFactory.createPickupPointResponseBuilder(PICKUP_POINT_ID).partnerId(PICKUP_POINT_PARTNER_ID).build();
    }

    @Nonnull
    protected LogisticsPointResponse getPointWithoutBothPartnerIdAndMarketId() {
        return LmsFactory.createPickupPointResponseBuilder(POINT_ID_WITHOUT_PARTNER_ID).build();
    }

    @Nonnull
    protected LogisticsPointResponse getWarehousePoint() {
        return LmsFactory.createWarehouseResponseBuilder(WAREHOUSE_ID).partnerId(PARTNER_ID).build();
    }

    @Nonnull
    protected LogisticsPointResponse getReturnWarehousePoint() {
        return LmsFactory.createWarehouseResponseBuilder(RETURN_WAREHOUSE).partnerId(RETURN_PARTNER).build();
    }

    @Nonnull
    protected LogisticsPointResponse getReturnWarehouseFFPoint() {
        return LmsFactory.createWarehouseResponseBuilder(RETURN_WAREHOUSE).partnerId(RETURN_PARTNER_FF).build();
    }

    @Nonnull
    protected static SearchPartnerFilter partnerFilter(Set<Long> partnerId) {
        return SearchPartnerFilter.builder()
            .setIds(partnerId)
            .build();
    }

    @Nonnull
    protected static LogisticsPointFilter logisticsPointFilter(Set<Long> partnerIds) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(partnerIds)
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }
}
