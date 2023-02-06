package ru.yandex.market.sc.core.domain.sorting_center;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServicePropertyRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertyRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.model.DeliveryServicePropertyDto;
import ru.yandex.market.sc.internal.model.DropOffDto;
import ru.yandex.market.sc.internal.model.InternalSortingCenterDto;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty.NEED_TRANSPORT_BARCODE;
import static ru.yandex.market.sc.core.domain.delivery_service.repository.DeliveryServiceProperty.TYPE_ON_SC_PREFIX;
import static ru.yandex.market.sc.core.domain.sorting_center.SortingCenterCommandService.TRUE_PROPERTY_VALUE;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ALWAYS_RESORT_DIRECT;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ALWAYS_RESORT_RETURNS;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.AUTO_CLOSE_OUTBOUND_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.DROPPED_ORDERS_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.IS_DROPOFF;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.IS_SORT_UNSKIPPABLE;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.PRE_RETURN_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.PRE_SHIP_ENABLED;
import static ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortingCenterCommandServiceTest {

    private static final long PARTNER_ID = 40948L;
    private static final String ADDRESS = "г. Москва, ул. Тверская, д.1";
    private static final String TOKEN = "544cc6c0f11e403da4ef84b27381d0f4898205270d5949609cc4876fc9903889";
    private static final String CAMPAIGN_ID = "480670";
    private static final String YANDEX_ID = "3854834";
    private static final String PARTNER_NAME = "ИП Ручкин Дмитрий Иванович";
    private static final String DROP_OFF_NAME = "ПВЗ Колотушкина на улице Пушкина";
    private static final long COURIER_DELIVERY_SERVICE_ID = 40949L;
    public static final String INCREDIBLE_SORTING_CENTER = "Incredible sorting center";
    public static final String TAG_SUFFIX = "incredible";
    private final long DELIVERY_SERVICE_UID_START = 1100000000000000L;

    private final TestFactory testFactory;
    private final SortingCenterCommandService sortingCenterCommandService;
    private final SortingCenterRepository sortingCenterRepository;
    private final SortingCenterPropertyRepository sortingCenterPropertyRepository;
    private final DeliveryServicePropertyRepository deliveryServicePropertyRepository;

    @BeforeEach
    void createPartner() {
        testFactory.storedSortingCenterPartner(PARTNER_ID, TOKEN);
    }

    @Test
    void createDropOff() {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + COURIER_DELIVERY_SERVICE_ID);

        DropOffDto inputParams = DropOffDto.builder()
                .deliveryPartnerId(PARTNER_ID)
                .address(ADDRESS)
                .apiToken(TOKEN)
                .campaignId(CAMPAIGN_ID)
                .partnerName(PARTNER_NAME)
                .dropOffName(DROP_OFF_NAME)
                .logisticPointId(YANDEX_ID)
                .courierDeliveryServiceId(COURIER_DELIVERY_SERVICE_ID)
                .build();

        sortingCenterCommandService.createAsDropOff(inputParams);

        var actualSc = sortingCenterRepository.findById(Long.parseLong(YANDEX_ID)).orElseThrow();
        var expectedSc = new SortingCenter(Long.parseLong(YANDEX_ID), ADDRESS, TOKEN, CAMPAIGN_ID, YANDEX_ID,
                PARTNER_NAME, DROP_OFF_NAME, null, PARTNER_ID);
        assertThat(actualSc).isEqualTo(expectedSc);

        var actualScProperties = StreamEx.of(sortingCenterPropertyRepository
                        .findAllBySortingCenterId(Long.parseLong(YANDEX_ID)))
                .map(p -> new SortingCenterPropertyParams(p.getSortingCenterId(), p.getKey(), p.getValue()))
                .toList();
        var expectedScProperties = List.of(
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), IS_DROPOFF, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), PRE_SHIP_ENABLED, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), PRE_RETURN_ENABLED, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), AUTO_CLOSE_OUTBOUND_ENABLED, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), ALWAYS_RESORT_DIRECT, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), ALWAYS_RESORT_RETURNS, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), ENABLE_SC_TO_SC_TRANSPORTATIONS, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(
                        Long.parseLong(YANDEX_ID), ENABLE_SC_TO_SC_TRANSPORTATIONS_FOR_ALL_DELIVERY_SERVICES, TRUE_PROPERTY_VALUE)

        );
        assertThat(actualScProperties).containsExactlyInAnyOrderElementsOf(expectedScProperties);

        var actualDsProperties = StreamEx.of(
                        deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(
                                String.valueOf(COURIER_DELIVERY_SERVICE_ID)))
                .map(p -> new DeliveryServicePropertyParams(p.getDeliveryServiceYandexId(), p.getKey(), p.getValue()))
                .toList();
        var expectedDsProperties = List.of(
                new DeliveryServicePropertyParams(
                        String.valueOf(COURIER_DELIVERY_SERVICE_ID),
                        TYPE_ON_SC_PREFIX + YANDEX_ID,
                        DeliveryServiceType.TRANSIT.name()
                )
        );
        assertThat(actualDsProperties).containsExactlyInAnyOrderElementsOf(expectedDsProperties);
    }

    @Test
    void idempotentCreateDropOffWithTheSameCampaignId() {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + COURIER_DELIVERY_SERVICE_ID);

        DropOffDto inputParams = DropOffDto.builder()
                .deliveryPartnerId(PARTNER_ID)
                .address(ADDRESS)
                .apiToken(TOKEN)
                .campaignId(CAMPAIGN_ID)
                .partnerName(PARTNER_NAME)
                .dropOffName(DROP_OFF_NAME)
                .logisticPointId(YANDEX_ID)
                .courierDeliveryServiceId(COURIER_DELIVERY_SERVICE_ID)
                .build();

        sortingCenterCommandService.createAsDropOff(inputParams);

        DropOffDto inputParams2 = DropOffDto.builder()
                .deliveryPartnerId(PARTNER_ID)
                .address(ADDRESS + " к.6")
                .apiToken(TOKEN + "999")
                .campaignId(CAMPAIGN_ID)
                .partnerName(PARTNER_NAME)
                .dropOffName(DROP_OFF_NAME)
                .logisticPointId(YANDEX_ID)
                .courierDeliveryServiceId(COURIER_DELIVERY_SERVICE_ID)
                .build();
        sortingCenterCommandService.createAsDropOff(inputParams2);

        var actual = sortingCenterRepository.findById(Long.parseLong(YANDEX_ID)).orElseThrow();

        var expected = new SortingCenter(Long.parseLong(YANDEX_ID), ADDRESS, TOKEN, CAMPAIGN_ID, YANDEX_ID,
                PARTNER_NAME, DROP_OFF_NAME, null, PARTNER_ID);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void tryToCreateDropOffWithTheSameYandexId() {
        testFactory.storedCourier(DELIVERY_SERVICE_UID_START + COURIER_DELIVERY_SERVICE_ID);

        DropOffDto inputParams = DropOffDto.builder()
                .deliveryPartnerId(PARTNER_ID)
                .address(ADDRESS)
                .apiToken(TOKEN)
                .campaignId(CAMPAIGN_ID)
                .partnerName(PARTNER_NAME)
                .dropOffName(DROP_OFF_NAME)
                .logisticPointId(YANDEX_ID)
                .courierDeliveryServiceId(COURIER_DELIVERY_SERVICE_ID)
                .build();
        sortingCenterCommandService.createAsDropOff(inputParams);

        DropOffDto inputParams2 = DropOffDto.builder()
                .deliveryPartnerId(PARTNER_ID)
                .address(ADDRESS + " к.6")
                .apiToken(TOKEN + "999")
                .campaignId(CAMPAIGN_ID + "2")
                .partnerName(PARTNER_NAME)
                .dropOffName(DROP_OFF_NAME)
                .logisticPointId(YANDEX_ID)
                .courierDeliveryServiceId(COURIER_DELIVERY_SERVICE_ID)
                .build();

        assertThatThrownBy(() -> sortingCenterCommandService.createAsDropOff(inputParams2))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    @Test
    void createRegularSortingCenter() {
        InternalSortingCenterDto sortingCenterDto = InternalSortingCenterDto.builder()
                .address(ADDRESS)
                .token(TOKEN)
                .partnerId(CAMPAIGN_ID)
                .logisticPointId(YANDEX_ID)
                .partnerName(PARTNER_NAME)
                .scName(INCREDIBLE_SORTING_CENTER)
                .regionTagSuffix(TAG_SUFFIX)
                .id(PARTNER_ID)
                .build();

        sortingCenterCommandService.createRegularSortingCenter(sortingCenterDto);

        var actualSc = sortingCenterRepository.findById(PARTNER_ID).orElseThrow();
        assertThat(actualSc.getId()).isEqualTo(PARTNER_ID);
        assertThat(actualSc.getPartnerId()).isEqualTo(CAMPAIGN_ID);
        assertThat(actualSc.getToken()).isEqualTo(TOKEN);

        var actualScProperties = StreamEx.of(sortingCenterPropertyRepository.findAllBySortingCenterId(PARTNER_ID))
                .map(p -> new SortingCenterPropertyParams(p.getSortingCenterId(), p.getKey(), p.getValue()))
                .toList();
        var expectedScProperties = List.of(
                new SortingCenterPropertyParams(PARTNER_ID, DROPPED_ORDERS_ENABLED, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(PARTNER_ID, SUPPORTS_SORT_LOTS_WITHOUT_CELL, TRUE_PROPERTY_VALUE),
                new SortingCenterPropertyParams(PARTNER_ID, IS_SORT_UNSKIPPABLE, TRUE_PROPERTY_VALUE)
        );
        assertThat(actualScProperties).containsExactlyInAnyOrderElementsOf(expectedScProperties);
    }

    @Test
    void addSimpleProperty() {
        DeliveryServicePropertyDto dto = DeliveryServicePropertyDto.builder()
                .deliveryServiceYandexId(COURIER_DELIVERY_SERVICE_ID)
                .key(NEED_TRANSPORT_BARCODE)
                .value(TRUE_PROPERTY_VALUE)
                .build();

        sortingCenterCommandService.addDeliveryServiceProperty(dto);

        var actualDsProperties = StreamEx.of(
                        deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(
                                String.valueOf(COURIER_DELIVERY_SERVICE_ID)))
                .map(p -> new DeliveryServicePropertyParams(p.getDeliveryServiceYandexId(), p.getKey(), p.getValue()))
                .toList();
        var expectedDsProperties = List.of(new DeliveryServicePropertyParams(
                String.valueOf(COURIER_DELIVERY_SERVICE_ID),
                NEED_TRANSPORT_BARCODE,
                TRUE_PROPERTY_VALUE));
        assertThat(actualDsProperties).containsExactlyInAnyOrderElementsOf(expectedDsProperties);
    }

    @Test
    void addTypeOnScProperty() {
        var sc = testFactory.storedSortingCenter();
        DeliveryServicePropertyDto dto = DeliveryServicePropertyDto.builder()
                .deliveryServiceYandexId(COURIER_DELIVERY_SERVICE_ID)
                .key(TYPE_ON_SC_PREFIX)
                .value("TRANSIT")
                .sortingCenterToken(sc.getToken())
                .build();

        sortingCenterCommandService.addDeliveryServiceProperty(dto);

        var actualDsProperties = StreamEx.of(
                        deliveryServicePropertyRepository.findAllByDeliveryServiceYandexId(
                                String.valueOf(COURIER_DELIVERY_SERVICE_ID)))
                .map(p -> new DeliveryServicePropertyParams(p.getDeliveryServiceYandexId(), p.getKey(), p.getValue()))
                .toList();
        var expectedDsProperties = List.of(new DeliveryServicePropertyParams(
                String.valueOf(COURIER_DELIVERY_SERVICE_ID),
                TYPE_ON_SC_PREFIX + sc.getId(),
                DeliveryServiceType.TRANSIT.name()));
        assertThat(actualDsProperties).containsExactlyInAnyOrderElementsOf(expectedDsProperties);
    }

    @Value
    private static class SortingCenterPropertyParams {

        long sortingCenterId;
        SortingCenterPropertiesKey key;
        String value;

    }

    @Value
    private static class DeliveryServicePropertyParams {

        String deliveryServiceYandexId;
        String key;
        String value;

    }

}
