package ru.yandex.market.abo.cpa.lms;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.cpa.lms.model.LMSCutoff;
import ru.yandex.market.abo.cpa.lms.model.LMSIntake;
import ru.yandex.market.abo.cpa.lms.model.LMSPartnerRelation;
import ru.yandex.market.abo.cpa.lms.repo.LMSCutoffRepo;
import ru.yandex.market.abo.cpa.lms.repo.LMSIntakeRepo;
import ru.yandex.market.abo.cpa.lms.repo.LMSPartnerRelationRepo;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.page.PageResult;
import ru.yandex.market.logistics.management.entity.response.CutoffResponse;
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 22/07/19.
 */
class LMSManagerTest extends EmptyTestWithTransactionTemplate {
    private static final Long PARTNER_ID = 1L;
    private static final Long FULFILMENT_ID = 2L;
    private static final Long DELIVERY_ID = 3L;

    private static final CutoffResponse LMS_CUTOFF = CutoffResponse.newBuilder()
            .locationId(225)
            .cutoffTime(LocalTime.now())
            .packagingDuration(Duration.ZERO.plusHours(1))
            .build();
    private static final Set<ScheduleDayResponse> SCHEDULES = IntStream.rangeClosed(1, 7)
            .mapToObj(day -> new ScheduleDayResponse(-1L, day, LocalTime.now(), LocalTime.now().plusHours(1)))
            .collect(Collectors.toSet());
    private static final PartnerRelationEntityDto PARTNER_RELATION = PartnerRelationEntityDto.newBuilder()
            .id(PARTNER_ID)
            .fromPartnerId(FULFILMENT_ID)
            .toPartnerId(DELIVERY_ID)
            .toPartnerLogisticsPointId(234234L)
            .handlingTime(1)
            .cutoffs(Set.of(LMS_CUTOFF))
            .intakeSchedule(SCHEDULES)
            .shipmentType(ShipmentType.WITHDRAW)
            .enabled(true)
            .build();

    @InjectMocks
    LMSManager lMSManager;
    @Mock
    LMSClient lmsClient;
    @Mock
    AboRetryableLmsClient aboRetryableLmsClient;
    @Mock
    LMSPartnerRelationRepo partnerRelationRepo;
    @Mock
    LMSCutoffRepo lmsCutoffRepo;
    @Mock
    LMSIntakeRepo lmsIntakeRepo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PageResult<PartnerRelationEntityDto> pageResult = new PageResult<PartnerRelationEntityDto>()
                .setPage(0)
                .setData(List.of(PARTNER_RELATION))
                .setSize(1)
                .setTotalElements(1);
        when(aboRetryableLmsClient.searchPartnerRelationWithPagination(any(), anyInt(), anyInt()))
                .thenReturn(pageResult);
    }

    @AfterEach
    void tearDown() {
        verify(aboRetryableLmsClient).searchPartnerRelationWithPagination(any(), anyInt(), anyInt());
    }

    @Test
    void testLoadData() {
        lMSManager.loadData();
        Stream.of(partnerRelationRepo, lmsCutoffRepo, lmsIntakeRepo).forEach(repo -> verify(repo).deleteAllInBatch());
        verify(partnerRelationRepo).saveAll(List.of(new LMSPartnerRelation(PARTNER_RELATION)));
        verify(lmsCutoffRepo).saveAll(List.of(new LMSCutoff(PARTNER_ID, LMS_CUTOFF)));
        verify(lmsIntakeRepo).saveAll(SCHEDULES.stream().map(sch -> new LMSIntake(PARTNER_ID, sch)).collect(Collectors.toList()));
    }

    @Test
    void testEmptyResponse() {
        PageResult<PartnerRelationEntityDto> pageResult = new PageResult<PartnerRelationEntityDto>()
                .setPage(0)
                .setData(List.of())
                .setSize(0)
                .setTotalElements(0);
        when(aboRetryableLmsClient.searchPartnerRelationWithPagination(any(), anyInt(), anyInt())).thenReturn(pageResult);
        lMSManager.loadData();
        verifyNoMoreInteractions(partnerRelationRepo, lmsCutoffRepo, lmsIntakeRepo);
    }
}
