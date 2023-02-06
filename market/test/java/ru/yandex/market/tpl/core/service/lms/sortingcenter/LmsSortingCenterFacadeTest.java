package ru.yandex.market.tpl.core.service.lms.sortingcenter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.front.library.dto.detail.DetailData;
import ru.yandex.market.logistics.front.library.dto.detail.DetailMeta;
import ru.yandex.market.logistics.front.library.dto.grid.GridData;
import ru.yandex.market.logistics.front.library.dto.grid.GridItem;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.common.db.jpa.BaseJpaEntity;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.lms.deliveryservice.LmsDeliveryServiceCreateDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.LmsScDsMappingCreateDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.LmsSortingCenterCreateDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.LmsSortingCenterFilterDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.LmsSortingCenterUpdateDto;
import ru.yandex.market.tpl.core.domain.lms.sortingcenter.property.SortingCenterEqueuePropertiesStateDto;
import ru.yandex.market.tpl.core.domain.partner.PartnerRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.lms.deliveryservice.LmsDeliveryServiceFacade;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.front.library.dto.Mode.EDIT;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_MINUTES_TO_ARRIVE_TO_LOADING;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_PARKING_CAPACITY;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_QTY_FREE_PLACES_TO_NEXT_SLOT;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_RATE_PASS_LATECOMERS;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.SLOT_DURATION_IN_MINUTES;

@RequiredArgsConstructor
class LmsSortingCenterFacadeTest extends TplAbstractTest {

    public static final long DELIVERY_SERVICE_ID = 123123;
    private static final long SORTING_CENTER_ID = 963258L;
    public static final Long EXPECTED_PARKING_CAPACITY = 1L;
    public static final Long EXPECTED_MINUTES_TO_LOADING = 20L;
    public static final Long EXPECTED_QTY_TO_NEXT_SLOT = 300L;
    public static final Long EXPECTED_DURATION_IN_MINUTES = 4000L;
    public static final Long EXPECTED_RATE_PASS_LATECOMERS = 5L;



    private final TestUserHelper testUserHelper;
    private final PartnerRepository<SortingCenter> sortingCenterRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final LmsSortingCenterFacade lmsSortingCenterFacade;
    private final LmsDeliveryServiceFacade lmsDeliveryServiceFacade;
    private final LMSClient lmsClient;
    private final SortingCenterPropertyService scPropertyService;

    @BeforeEach
    void setup() {
        reset(lmsClient);
        when(lmsClient.getPartner(SORTING_CENTER_ID))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.SORTING_CENTER).build()));
        when(lmsClient.getPartner(DELIVERY_SERVICE_ID))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.DELIVERY).build()));
        when(lmsClient.getLogisticsPoint(1L))
                .thenReturn(Optional.of(LogisticsPointResponse.newBuilder().partnerId(SORTING_CENTER_ID).build()));
    }

    @Test
    void getEqPropertiesStateById() {
        //given
        long scId = 777L;
        SortingCenter sc = testUserHelper.sortingCenter(scId);

        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_PARKING_CAPACITY, EXPECTED_PARKING_CAPACITY);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_MINUTES_TO_ARRIVE_TO_LOADING, EXPECTED_MINUTES_TO_LOADING);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_QTY_FREE_PLACES_TO_NEXT_SLOT, EXPECTED_QTY_TO_NEXT_SLOT);
        scPropertyService.upsertPropertyToSortingCenter(sc, SLOT_DURATION_IN_MINUTES, EXPECTED_DURATION_IN_MINUTES);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_RATE_PASS_LATECOMERS, EXPECTED_RATE_PASS_LATECOMERS);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_ENABLED, false);

        //when
        DetailData detailData = lmsSortingCenterFacade.getEqPropertiesStateById(scId);

        //then
        assertDetailData(scId, detailData, List.of(EXPECTED_PARKING_CAPACITY, EXPECTED_MINUTES_TO_LOADING,
                EXPECTED_QTY_TO_NEXT_SLOT, EXPECTED_DURATION_IN_MINUTES,
                EXPECTED_RATE_PASS_LATECOMERS));
    }

    @Test
    void updateEqPropertiesStateById() {
        //given
        long scId = 888L;
        testUserHelper.sortingCenter(scId);

        //when
        DetailData detailData = lmsSortingCenterFacade.updateEqPropertiesState(
                SortingCenterEqueuePropertiesStateDto
                        .builder()
                        .id(scId)
                        .minutesToArriveToLoading(EXPECTED_MINUTES_TO_LOADING)
                        .parkingCapacity(EXPECTED_PARKING_CAPACITY)
                        .qtyFreePlacesToNextSlot(EXPECTED_QTY_TO_NEXT_SLOT)
                        .ratePassLatecomers(EXPECTED_RATE_PASS_LATECOMERS)
                        .slotDurationInMinutes(EXPECTED_DURATION_IN_MINUTES)
                        .build()
        );

        //then
        assertDetailData(scId, detailData, List.of(EXPECTED_PARKING_CAPACITY, EXPECTED_MINUTES_TO_LOADING,
                EXPECTED_QTY_TO_NEXT_SLOT, EXPECTED_DURATION_IN_MINUTES,
                EXPECTED_RATE_PASS_LATECOMERS));
    }

    private void assertDetailData(long scId, DetailData detailData, List<Long> expectedValues) {
        DetailMeta meta = detailData.getMeta();
        assertThat(meta.getActions()).hasSize(1);
        assertThat(meta.getActions().get(0).getSlug()).contains("ENABLE");
        assertThat(meta.getMode()).isEqualTo(EDIT);
        assertThat(detailData.getItem().getId()).isEqualTo(scId);
        assertThat(detailData.getItem().getValues().values()).containsExactlyInAnyOrderElementsOf(expectedValues);
    }

    @Test
    void getSortingCenters() {
        SortingCenter sortingCenter1 = testUserHelper.sortingCenter(123L);
        SortingCenter sortingCenter2 = testUserHelper.sortingCenter(456L);

        GridData unfiltered = lmsSortingCenterFacade.getSortingCenters(null, Pageable.unpaged());

        // в ликвибейзе есть миграции автоматически добавляющие СЦ...
        assertThat(unfiltered.getTotalCount()).isGreaterThan(2);


        GridData filteredById = lmsSortingCenterFacade.getSortingCenters(
                new LmsSortingCenterFilterDto(sortingCenter1.getId(), null),
                Pageable.unpaged()
        );
        assertThat(filteredById.getItems())
                .extracting(GridItem::getId)
                .containsExactly(sortingCenter1.getId());

        GridData filteredByName = lmsSortingCenterFacade.getSortingCenters(
                new LmsSortingCenterFilterDto(null, "" + sortingCenter2.getName()),
                Pageable.unpaged()
        );
        assertThat(filteredByName.getItems())
                .extracting(GridItem::getId)
                .containsExactly(sortingCenter2.getId());
    }

    @Test
    void updateSortingCenter() {
        String expectedToken = "test_token";
        String expectedAddress = "789";
        String expectedName = "SuperSC";
        String expectedJugglerTag = "tag";
        BigDecimal expectedLogitude = BigDecimal.valueOf(37);
        BigDecimal expectedLatitude = BigDecimal.valueOf(56);
        LocalTime expectedStartTime = LocalTime.of(4, 0);
        LocalTime expectedEndTime = LocalTime.of(16, 0);
        ZoneOffset expectedZoneOffset = ZoneOffset.ofHours(5);
        long expectedRegionId = 2L;

        SortingCenter sortingCenter = testUserHelper.sortingCenter(123L);

        assertThat(sortingCenter.getToken()).isNotEqualTo(expectedToken);
        assertThat(sortingCenter.getAddress()).isNotEqualTo(expectedAddress);
        assertThat(sortingCenter.getName()).isNotEqualTo(expectedName);
        assertThat(sortingCenter.getLongitude()).isNotEqualTo(expectedLogitude);
        assertThat(sortingCenter.getLatitude()).isNotEqualTo(expectedLatitude);
        assertThat(sortingCenter.getStartTime()).isNotEqualTo(expectedStartTime);
        assertThat(sortingCenter.getEndTime()).isNotEqualTo(expectedEndTime);
        assertThat(sortingCenter.getZoneOffset()).isNotEqualTo(expectedZoneOffset);
        assertThat(sortingCenter.getRegionId()).isNotEqualTo(expectedRegionId);

        lmsSortingCenterFacade.updateSortingCenter(
                sortingCenter.getId(),
                LmsSortingCenterUpdateDto.builder()
                        .token(expectedToken)
                        .address(expectedAddress)
                        .name(expectedName)
                        .longitude(expectedLogitude)
                        .latitude(expectedLatitude)
                        .startTime(expectedStartTime)
                        .endTime(expectedEndTime)
                        .zoneOffset(expectedZoneOffset)
                        .regionId(expectedRegionId)
                        .jugglerRegionTag(expectedJugglerTag)
                        .build()
        );

        sortingCenter = sortingCenterRepository.findByIdOrThrow(sortingCenter.getId());

        assertThat(sortingCenter.getToken()).isEqualTo(expectedToken);
        assertThat(sortingCenter.getAddress()).isEqualTo(expectedAddress);
        assertThat(sortingCenter.getName()).isEqualTo(expectedName);
        assertThat(sortingCenter.getLongitude()).isEqualTo(expectedLogitude);
        assertThat(sortingCenter.getLatitude()).isEqualTo(expectedLatitude);
        assertThat(sortingCenter.getStartTime()).isEqualTo(expectedStartTime);
        assertThat(sortingCenter.getEndTime()).isEqualTo(expectedEndTime);
        assertThat(sortingCenter.getZoneOffset()).isEqualTo(expectedZoneOffset);
        assertThat(sortingCenter.getRegionId()).isEqualTo(expectedRegionId);
        assertThat(sortingCenter.getJugglerRegionTag()).isEqualTo(expectedJugglerTag);
    }

    @Test
    void simpleCreateSortingCenter() {
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 0);
        createSortingCenter(false);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 1);
    }

    @Test
    void createSiblingSortingCenter() {
        createSortingCenter(false);

        String expectedAddress = "789";
        String expectedName = "СЦ Новый дубль";
        String expectedPartnerName = "ExpectedPartnerName";
        BigDecimal expectedLongitude = BigDecimal.valueOf(37);
        BigDecimal expectedLatitude = BigDecimal.valueOf(56);
        LocalTime expectedStartTime = LocalTime.of(4, 0);
        LocalTime expectedEndTime = LocalTime.of(16, 0);
        ZoneOffset expectedZoneOffset = ZoneOffset.ofHours(5);
        long expectedRegionId = 2L;
        Long SIBLING_SORTING_CENTER_ID = 987654321L;

        when(lmsClient.getPartner(SIBLING_SORTING_CENTER_ID))
                .thenReturn(Optional.of(PartnerResponse.newBuilder().partnerType(PartnerType.SORTING_CENTER).build()));
        when(lmsClient.getLogisticsPoint(1L))
                .thenReturn(Optional.of(LogisticsPointResponse.newBuilder().partnerId(SIBLING_SORTING_CENTER_ID).build()));

        lmsSortingCenterFacade.createSortingCenter(
                LmsSortingCenterCreateDto.builder()
                        .sortingCenterId(SIBLING_SORTING_CENTER_ID)
                        .address(expectedAddress)
                        .name(expectedName)
                        .partnerName(expectedPartnerName)
                        .longitude(expectedLongitude)
                        .latitude(expectedLatitude)
                        .startTime(expectedStartTime)
                        .endTime(expectedEndTime)
                        .zoneOffset(expectedZoneOffset)
                        .regionId(expectedRegionId)
                        .createNewScInScSystem(false)
                        .logisticPointId("1")
                        .thirdParty(true)
                        .siblingSortingCenterId(SORTING_CENTER_ID)
                        .build()
        );
        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(SORTING_CENTER_ID);
        SortingCenter sortingCenter2 = sortingCenterRepository.findByIdOrThrow(SIBLING_SORTING_CENTER_ID);

        assertThat(sortingCenter.getToken()).isEqualTo(sortingCenter2.getToken());
    }

    @Test
    void complexCreateSortingCenter() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.EMAIL_FOR_REGISTER_SC, "test@test.ru");
        configurationServiceAdapter.insertValue(ConfigurationProperties.UID_FOR_REGISTER_SC, "100500");

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_SORTING_CENTER_MBI_CAMPAIGN, 0);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 0);

        // action
        createSortingCenter(true);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_API_SETTINGS, 1);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_SORTING_CENTER_MBI_CAMPAIGN, 1);
    }

    void createSortingCenter(boolean createNewRowInOtherSystem) {
        String expectedAddress = "789";
        String expectedName = "СЦ Новый SV";
        String expectedPartnerName = "ExpectedPartnerName";
        BigDecimal expectedLongitude = BigDecimal.valueOf(37);
        BigDecimal expectedLatitude = BigDecimal.valueOf(56);
        LocalTime expectedStartTime = LocalTime.of(4, 0);
        LocalTime expectedEndTime = LocalTime.of(16, 0);
        ZoneOffset expectedZoneOffset = ZoneOffset.ofHours(5);
        long expectedRegionId = 2L;
        String expectedJugglerTag = "SC_Novyj_SV";
        lmsSortingCenterFacade.createSortingCenter(
                LmsSortingCenterCreateDto.builder()
                        .sortingCenterId(SORTING_CENTER_ID)
                        .address(expectedAddress)
                        .name(expectedName)
                        .partnerName(expectedPartnerName)
                        .longitude(expectedLongitude)
                        .latitude(expectedLatitude)
                        .startTime(expectedStartTime)
                        .endTime(expectedEndTime)
                        .zoneOffset(expectedZoneOffset)
                        .regionId(expectedRegionId)
                        .createNewScInScSystem(createNewRowInOtherSystem)
                        .logisticPointId("1")
                        .thirdParty(true)
                        .build()
        );
        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(SORTING_CENTER_ID);
        assertThat(sortingCenter.getToken()).isNotEmpty();
        assertThat(sortingCenter.getAddress()).isEqualTo(expectedAddress);
        assertThat(sortingCenter.getName()).isEqualTo(expectedName);
        assertThat(sortingCenter.getPartnerName()).isEqualTo(expectedPartnerName);
        assertThat(sortingCenter.getLongitude()).isEqualTo(expectedLongitude);
        assertThat(sortingCenter.getLatitude()).isEqualTo(expectedLatitude);
        assertThat(sortingCenter.getStartTime()).isEqualTo(expectedStartTime);
        assertThat(sortingCenter.getEndTime()).isEqualTo(expectedEndTime);
        assertThat(sortingCenter.getZoneOffset()).isEqualTo(expectedZoneOffset);
        assertThat(sortingCenter.getRegionId()).isEqualTo(expectedRegionId);
        assertThat(sortingCenter.getJugglerRegionTag()).isEqualTo(expectedJugglerTag);
    }

    @Test
    void createScDsMapping() {
        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_SC_DS_MAPPING, 0);

        long sortingCenterId = 123L;
        testUserHelper.sortingCenter(sortingCenterId);

        String dsName = "SuperDS";
        lmsDeliveryServiceFacade.createDeliveryService(
                LmsDeliveryServiceCreateDto.builder()
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .name(dsName)
                        .deliveryAreaMarginWidth(0L)
                        .kkt("")
                        .build()
        );

        lmsSortingCenterFacade.createScDsMapping(
                LmsScDsMappingCreateDto.builder()
                        .sortingCenterId(sortingCenterId)
                        .deliveryServiceId(DELIVERY_SERVICE_ID)
                        .build()
        );

        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(sortingCenterId);

        assertThat(sortingCenter.getDeliveryServices()).hasSize(1);
        assertThat(sortingCenter.getDeliveryServices())
                .extracting(BaseJpaEntity.LongFixed::getId)
                .containsExactly(DELIVERY_SERVICE_ID);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CREATE_SC_DS_MAPPING, 1);
    }
}
