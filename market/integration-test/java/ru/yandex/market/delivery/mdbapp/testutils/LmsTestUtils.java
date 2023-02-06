package ru.yandex.market.delivery.mdbapp.testutils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.when;

public final class LmsTestUtils {

    public static final long MIDDLE_MILE_DS_ID = 1005372L;
    public static final long DYNAMIC_RESOLVE_MIDDLE_MILE_DS_ID = 12345L;
    public static final long LAST_MILE_DS_ID = 1003562L;
    public static final long PICKUP_POINT_ID = 1007111L;
    public static final long PICKUP_POINT_PARTNER_ID = 1007222L;
    public static final long CROSSDOCK_PARTNER_ID = 47732L;

    private LmsTestUtils() {
        throw new UnsupportedOperationException();
    }

    public static void mockGet145ActiveLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(145L, true);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(create145LogisticsPoint()));
    }

    public static void mockGet145LogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(145L, null);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(create145LogisticsPoint()));
    }

    public static void mockGetLogisticsPoint(LMSClient lmsClient, long partnerId) {
        LogisticsPointFilter filter = logisticsPointFilter(partnerId, null);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(create145LogisticsPoint()));
    }

    public static void mockGetDropshipLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(47798L, true);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(create47798LogisticsPoint()));
    }

    public static void mockGetCrossdockLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(CROSSDOCK_PARTNER_ID, true);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(createCrossdockLogisticsPoint()));
    }

    public static void mockGetDropshipWithoutScLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(47821L, true);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(create47821LogisticsPoint()));
    }

    public static void mockGetPickupLogisticsPoint(LMSClient lmsClient) {
        mockGetPickupLogisticsPoint(lmsClient, 1003937L);
    }

    public static void mockGetPickupLogisticsPoint(LMSClient lmsClient, long partnerId) {
        LogisticsPointFilter filter = logisticsPointFilterPickup(partnerId, "107014");
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(createPickupLogisticsPoint()));
    }

    public static void mockGetPostamatLogisticPoint(LMSClient lmsClient) {
        when(lmsClient.getLogisticsPoints(refEq(getPostamatLogisticsPointFilter())))
            .thenReturn(List.of(createPostamatLogisticsPoint()));
    }

    public static void mockGetPostamatMiddleMileLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilter(MIDDLE_MILE_DS_ID, true);
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(createPickupLogisticsPoint()));

        when(lmsClient.getLogisticsPoints(refEq(logisticsPointFilter(DYNAMIC_RESOLVE_MIDDLE_MILE_DS_ID, true))))
            .thenReturn(List.of(createPickupLogisticsPoint()));
    }

    public static void mockGetPostamatPickupLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilterPickup(10000893081L, "78");
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(createPickupLogisticsPoint(10000893081L, LAST_MILE_DS_ID, "78")));
    }

    public static void mockGetOutletPickupLogisticsPoint(LMSClient lmsClient) {
        LogisticsPointFilter filter = logisticsPointFilterPickup(LAST_MILE_DS_ID, "630060");
        when(lmsClient.getLogisticsPoints(refEq(filter)))
            .thenReturn(List.of(createPickupLogisticsPoint(10000893080L, LAST_MILE_DS_ID, "630060")));
    }

    public static void mockGetFfToDsPartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getFfToDsPartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = getFfToDsPartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetFfToDsPartnerPosteRestanteRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getFfToDsPartnerPosteRestanteRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = getFfToDsPartnerPosteRestanteRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetDropshipFfToDsPartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getDropshipFfToDsPartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = getDropshipFfToDsPartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetDropshipToScPartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getDropshipToScPartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = getDropshipToScPartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetDropshipSdtToScPartnerRelation(
        LMSClient lmsClient,
        long fromPartnerId,
        long toPartnerId
    ) {
        PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
            .fromPartnerId(fromPartnerId)
            .toPartnerId(toPartnerId)
            .build();
        PartnerRelationEntityDto partnerRelationEntityDto = PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(fromPartnerId)
            .toPartnerId(toPartnerId)
            .enabled(true)
            .returnPartnerId(fromPartnerId)
            .shipmentType(ShipmentType.IMPORT)
            .build();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetCrossdockToFfPartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getCrossdockToFfPartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = getCrossdockToFfPartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetDropshipWithoutScToDeliveryServicePartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = getDropshipWithoutScToDeliveryServicePartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto =
            getDropshipWithoutScToDeliveryServicePartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(List.of(partnerRelationEntityDto));
    }

    public static void mockGetFfToDsPostamatPartnerRelation(LMSClient lmsClient, Long toPartnerId) {
        PartnerRelationFilter partnerRelationFilter = getPostamatPartnerRelationFilter(toPartnerId);
        PartnerRelationEntityDto partnerRelationEntityDto = getPostamatPartnerRelationResponse(toPartnerId);

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(Collections.singletonList(partnerRelationEntityDto));
    }

    public static void mockGet133To105PostamatPartnerRelation(LMSClient lmsClient) {
        PartnerRelationFilter partnerRelationFilter = get133To105PartnerRelationFilter();
        PartnerRelationEntityDto partnerRelationEntityDto = get133To105PartnerRelationResponse();

        when(lmsClient.searchPartnerRelation(refEq(partnerRelationFilter)))
            .thenReturn(Collections.singletonList(partnerRelationEntityDto));
    }

    public static void mockGet145Partner(LMSClient lmsClient) {
        SearchPartnerFilter partnerFilter = get145PartnerFilter();
        PartnerResponse partner = get145PartnerResponse();

        when(lmsClient.searchPartners(refEq(partnerFilter))).thenReturn(List.of(partner));
    }

    public static void mockGet133Partner(LMSClient lmsClient) {
        SearchPartnerFilter partnerFilter = get133PartnerFilter();
        PartnerResponse partner = get133PartnerResponse();

        when(lmsClient.searchPartners(refEq(partnerFilter))).thenReturn(List.of(partner));
    }

    public static void mockGetDropshipPartner(LMSClient lmsClient) {
        SearchPartnerFilter partnerFilter = getDropshipPartnerFilter();
        PartnerResponse dropshipPartner = getDropshipPartnerResponse();

        when(lmsClient.searchPartners(refEq(partnerFilter))).thenReturn(List.of(dropshipPartner));
    }

    public static void mockGetCrossdockPartner(LMSClient lmsClient) {
        SearchPartnerFilter partnerFilter = getCrossdockPartnerFilter();
        PartnerResponse crossdockPartner = getCrossdockPartnerResponse();

        when(lmsClient.searchPartners(refEq(partnerFilter))).thenReturn(List.of(crossdockPartner));
    }

    public static void mockGetDropshipWithoutScPartner(LMSClient lmsClient) {
        SearchPartnerFilter partnerFilter = getDropshipWithoutScPartnerFilter();
        PartnerResponse dropshipPartner = getDropshipWithoutScPartnerResponse();

        when(lmsClient.searchPartners(refEq(partnerFilter))).thenReturn(List.of(dropshipPartner));
    }

    private static SearchPartnerFilter get145PartnerFilter() {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(145L))
            .build();
    }

    private static PartnerResponse get145PartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(145L)
            .partnerType(PartnerType.FULFILLMENT)
            .build();
    }

    private static SearchPartnerFilter get133PartnerFilter() {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(133L))
            .build();
    }

    private static PartnerResponse get133PartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(133L)
            .partnerType(PartnerType.FULFILLMENT)
            .build();
    }

    private static SearchPartnerFilter getDropshipPartnerFilter() {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(47798L))
            .build();
    }

    private static PartnerResponse getDropshipPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(47798L)
            .partnerType(PartnerType.DROPSHIP)
            .marketId(2012862L)
            .build();
    }

    private static SearchPartnerFilter getCrossdockPartnerFilter() {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(CROSSDOCK_PARTNER_ID))
            .build();
    }

    private static PartnerResponse getCrossdockPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(CROSSDOCK_PARTNER_ID)
            .partnerType(PartnerType.SUPPLIER)
            .marketId(50L)
            .build();
    }

    private static SearchPartnerFilter getDropshipWithoutScPartnerFilter() {
        return SearchPartnerFilter.builder()
            .setIds(Set.of(47821L))
            .build();
    }

    private static PartnerResponse getDropshipWithoutScPartnerResponse() {
        return PartnerResponse.newBuilder()
            .id(47821L)
            .partnerType(PartnerType.SUPPLIER)
            .marketId(2000676L)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getFfToDsPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(8L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getFfToDsPartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(8L)
            .enabled(true)
            .returnPartnerId(171L)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getFfToDsPartnerPosteRestanteRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(1005117L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getFfToDsPartnerPosteRestanteRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(1005117L)
            .enabled(true)
            .returnPartnerId(171L)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getDropshipToScPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(47798L)
            .toPartnerId(145L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getDropshipToScPartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(47798L)
            .toPartnerId(145L)
            .enabled(true)
            .returnPartnerId(47798L)
            .shipmentType(ShipmentType.IMPORT)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getDropshipFfToDsPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(1003937L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getDropshipFfToDsPartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(1003937L)
            .enabled(true)
            .returnPartnerId(171L)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getCrossdockToFfPartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(CROSSDOCK_PARTNER_ID)
            .toPartnerId(145L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getCrossdockToFfPartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(CROSSDOCK_PARTNER_ID)
            .toPartnerId(145L)
            .enabled(true)
            .returnPartnerId(171L)
            .shipmentType(ShipmentType.WITHDRAW)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getDropshipWithoutScToDeliveryServicePartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(47821L)
            .toPartnerId(8L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getDropshipWithoutScToDeliveryServicePartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(47821L)
            .toPartnerId(8L)
            .enabled(true)
            .returnPartnerId(47821L)
            .shipmentType(ShipmentType.WITHDRAW)
            .build();
    }

    @NotNull
    private static LogisticsPointFilter getPostamatLogisticsPointFilter() {
        return LogisticsPointFilter.newBuilder()
            .type(PointType.PICKUP_POINT)
            .partnerIds(Set.of(LAST_MILE_DS_ID))
            .externalIds(Set.of("78"))
            .build();
    }

    @NotNull
    private static PartnerRelationFilter getPostamatPartnerRelationFilter(Long toPartnerId) {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(toPartnerId)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto getPostamatPartnerRelationResponse(Long toPartnerId) {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(145L)
            .toPartnerId(toPartnerId)
            .enabled(true)
            .returnPartnerId(171L)
            .build();
    }

    @NotNull
    private static PartnerRelationFilter get133To105PartnerRelationFilter() {
        return PartnerRelationFilter.newBuilder()
            .fromPartnerId(133L)
            .toPartnerId(105L)
            .build();
    }

    @NotNull
    private static PartnerRelationEntityDto get133To105PartnerRelationResponse() {
        return PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(133L)
            .toPartnerId(105L)
            .enabled(true)
            .returnPartnerId(171L)
            .build();
    }

    @Nonnull
    public static LogisticsPointFilter logisticsPointFilter(long id, Boolean active) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(id))
            .type(PointType.WAREHOUSE)
            .active(active)
            .build();
    }

    @Nonnull
    public static LogisticsPointFilter logisticsPointFilterPickup(long id, String externalId) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(id))
            .externalIds(Set.of(externalId))
            .type(PointType.PICKUP_POINT)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse create145LogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000010749L)
            .partnerId(145L)
            .externalId("main")
            .type(PointType.WAREHOUSE)
            .name("г.Котельники, Яничкин проезд, д.7, терминал БД6")
            .address(Address.newBuilder()
                .locationId(21651)
                .settlement("")
                .postCode("111024")
                .latitude(new BigDecimal("55.659840"))
                .longitude(new BigDecimal("37.863199"))
                .street("Яничкин проезд")
                .house("7")
                .housing("")
                .building("")
                .apartment("")
                .comment("")
                .addressString("111024, Московская область, Котельники, Яничкин проезд, 7")
                .shortAddressString("Яничкин проезд, 7")
                .build())
            .phones(Set.of(
                new Phone("78006008076", null, null, PhoneType.ADDITIONAL),
                new Phone("78006008076", null, null, PhoneType.PRIMARY)
            ))
            .active(true)
            .schedule(Set.of(
                scheduleDay(1051533L, 6),
                scheduleDay(1051536L, 2),
                scheduleDay(1051530L, 3),
                scheduleDay(1051532L, 5),
                scheduleDay(1051534L, 7),
                scheduleDay(1051535L, 1),
                scheduleDay(1051531L, 4)
            ))
            .contact(new Contact("контактное", "лицо", ""))
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse create171LogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000010735L)
            .partnerId(171L)
            .externalId("171")
            .type(PointType.WAREHOUSE)
            .name("Склад Яндекс Маркет Томилино")
            .address(Address.newBuilder()
                .locationId(101060)
                .settlement("")
                .postCode("140073")
                .latitude(new BigDecimal("55.661399"))
                .longitude(new BigDecimal("37.950838"))
                .street("микрорайон Птицефабрика")
                .house("8")
                .housing("")
                .building("")
                .apartment("")
                .comment("140073, Московская область, Люберецкий район, "
                    + "посёлок городского типа Томилино, микрорайон Птицефабрика, к8")
                .region(null)
                .addressString(null)
                .shortAddressString(null)
                .build())
            .phones(Set.of(
                new Phone("79153880333", null, null, PhoneType.ADDITIONAL)
            ))
            .active(true)
            .schedule(Set.of(
                scheduleDay(1067286L, 6),
                scheduleDay(1067285L, 5),
                scheduleDay(1067284L, 4),
                scheduleDay(1067287L, 7),
                scheduleDay(1067282L, 2),
                scheduleDay(1067283L, 3),
                scheduleDay(1067281L, 1)
            ))
            .contact(new Contact("Дмитрий", "Колпаков", ""))
            .instruction("140073, Московская область, Люберецкий район, "
                + "посёлок городского типа Томилино, микрорайон Птицефабрика, к8")
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse create47798LogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000879640L)
            .partnerId(171L)
            .externalId("100136")
            .type(PointType.WAREHOUSE)
            .name("Dropship склад")
            .address(Address.newBuilder()
                .locationId(213)
                .settlement("г. Москва")
                .postCode("140073")
                .latitude(new BigDecimal("55.471942"))
                .longitude(new BigDecimal("37.551046"))
                .street("Магистральная 3-я")
                .house("30")
                .housing("")
                .building("1")
                .apartment("")
                .comment("")
                .build())
            .phones(Set.of(
                new Phone("79153880333", null, null, PhoneType.ADDITIONAL)
            ))
            .active(true)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createCrossdockLogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000879641L)
            .partnerId(CROSSDOCK_PARTNER_ID)
            .externalId("123123")
            .type(PointType.WAREHOUSE)
            .name("Crossdock склад")
            .address(Address.newBuilder()
                .locationId(213)
                .settlement("г. Москва")
                .postCode("140073")
                .latitude(new BigDecimal("55.471942"))
                .longitude(new BigDecimal("37.551046"))
                .street("Магистральная 3-я")
                .house("30")
                .housing("")
                .building("1")
                .apartment("")
                .comment("")
                .build())
            .phones(Set.of(
                new Phone("79153880333", null, null, PhoneType.ADDITIONAL)
            ))
            .active(true)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse create47821LogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000829134L)
            .partnerId(47821L)
            .externalId("123123")
            .type(PointType.WAREHOUSE)
            .name("Dropship склад")
            .address(Address.newBuilder()
                .locationId(213)
                .settlement("г. Москва")
                .postCode("140073")
                .latitude(new BigDecimal("55.471942"))
                .longitude(new BigDecimal("37.551046"))
                .street("Магистральная 3-я")
                .house("30")
                .housing("")
                .building("1")
                .apartment("")
                .comment("")
                .build())
            .phones(Set.of(
                new Phone("79153880333", null, null, PhoneType.ADDITIONAL)
            ))
            .active(true)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createPickupLogisticsPoint() {
        return createPickupLogisticsPoint(10000342537L, 1003937L, "107014");
    }

    @Nonnull
    public static LogisticsPointResponse createPickupLogisticsPoint(Long id, Long partnerId, String externalId) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .partnerId(partnerId)
            .externalId(externalId)
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.TERMINAL)
            .name("Точка самовывооза")
            .address(Address.newBuilder()
                .locationId(213)
                .settlement("г. Москва")
                .postCode("140073")
                .latitude(new BigDecimal("55.471942"))
                .longitude(new BigDecimal("37.551046"))
                .street("Магистральная 3-я")
                .house("30")
                .housing("")
                .building("1")
                .apartment("")
                .comment("")
                .build())
            .phones(Set.of(
                new Phone("79153880333", null, null, PhoneType.ADDITIONAL)
            ))
            .active(true)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createSpbPickupLogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(10000342537L)
            .partnerId(1003937L)
            .externalId("107014")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.TERMINAL)
            .name("Отделение почтовой связи МОСКВА 140073")
            .address(Address.newBuilder()
                .locationId(2)
                .settlement("Санкт-Петербург")
                .postCode("107014")
                .latitude(new BigDecimal("55.471942"))
                .longitude(new BigDecimal("37.551046"))
                .street("Новоизмайловский пр-кт")
                .house("22")
                .housing("2")
                .build())
            .phones(Set.of(
                new Phone("74992687735", null, null, PhoneType.PRIMARY)
            ))
            .schedule(Set.of(
                new ScheduleDayResponse(1L, 1, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new ScheduleDayResponse(2L, 2, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new ScheduleDayResponse(3L, 3, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new ScheduleDayResponse(4L, 4, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(8, 0), LocalTime.of(20, 0)),
                new ScheduleDayResponse(5L, 5, LocalTime.of(9, 0), LocalTime.of(18, 0))
            ))
            .active(true)
            .build();
    }

    @Nonnull
    public static LogisticsPointResponse createPostamatLogisticsPoint() {
        return LogisticsPointResponse.newBuilder()
            .id(LAST_MILE_DS_ID)
            .partnerId(LAST_MILE_DS_ID)
            .externalId("78")
            .type(PointType.PICKUP_POINT)
            .pickupPointType(PickupPointType.TERMINAL)
            .name("Точка самовывооза")
            .build();
    }

    @Nonnull
    public static ScheduleDayResponse scheduleDay(long id, int day) {
        return new ScheduleDayResponse(id, day, LocalTime.of(0, 0), LocalTime.of(23, 59));
    }
}
