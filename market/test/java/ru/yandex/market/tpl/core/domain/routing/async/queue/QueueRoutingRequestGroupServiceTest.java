package ru.yandex.market.tpl.core.domain.routing.async.queue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingRequestGroup;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingRequestGroupItem;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingRequestGroupRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.external.routing.api.CreateShiftRoutingRequestCommandData;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingDepot;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.routing.async.queue.QueueRoutingRequestGroupService.DEFAULT_DELAY_ROUTING_REQUEST_QUEUE;

@RequiredArgsConstructor
class QueueRoutingRequestGroupServiceTest extends TplAbstractTest {

    public static final LocalDate SHIFT_DATE = LocalDate.now();
    public static final String REQUEST_ID = "req_id";
    private final QueueRoutingRequestGroupService<CreateShiftRoutingRequestCommandData> queueService;

    private final TransactionTemplate transactionTemplate;
    private final RoutingRequestGroupRepository routingRequestGroupRepository;
    private final RoutingLogDao routingLogDao;
    private final TestUserHelper userHelper;

    @Test
    void reenqueueDelay_when_NoAnyCouriers() {
        //given
        var routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildManualRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });


        //then
        assertThat(queueService.reenqueueDelay(new QueueRoutingRequestGroupPayload("requestId",
                routingRequestGroup.getId())))
                .isEqualTo(Optional.empty());
    }

    @Test
    void reenqueueDelay_when_ManualType() {
        //given
        addCourierToSchedule();
        var routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildManualRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });


        //then
        assertThat(queueService.reenqueueDelay(new QueueRoutingRequestGroupPayload("requestId",
                routingRequestGroup.getId())))
                .isEqualTo(Optional.empty());
    }

    @Test
    void reenqueueDelay_when_NotManual_NotInterferingGroup() {
        //given
        addCourierToSchedule();
        var routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildGroupRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });


        //then
        assertThat(queueService.reenqueueDelay(new QueueRoutingRequestGroupPayload("requestId",
                routingRequestGroup.getId())))
                .isEqualTo(Optional.empty());
    }


    @Test
    void reenqueueDelay_when_InterferingGroup() {
        //given
        addCourierToSchedule();
        //Два запроса на один по одной и той же предполагаемой смене СЦ, с нетерминальным статусом
        transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildGroupRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });
        var routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildGroupRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });


        //then
        assertThat(queueService.reenqueueDelay(new QueueRoutingRequestGroupPayload("requestId",
                routingRequestGroup.getId())))
                .isEqualTo(Optional.of(DEFAULT_DELAY_ROUTING_REQUEST_QUEUE));
    }

    @Test
    void reenqueueDelay_when_InterferingGroup_withTerminalStatus() {
        //given
        addCourierToSchedule();
        //Два запроса на один по одной и той же предполагаемой смене СЦ, с нетерминальным статусом
        transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            String routingRequestId = UUID.randomUUID().toString();
            RoutingRequest routingRequest = buildGroupRequest(rrg, routingRequestId);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            routingLogDao.updateAtProcessingStart(REQUEST_ID, routingRequestId);
            routingLogDao.updatePublishingStatus(routingRequestId, RoutingResultStatus.SUCCESS);
            return rrg;
        });
        var routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(buildRoutingRequestGroup());
            RoutingRequest routingRequest = buildGroupRequest(rrg);
            Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                    routingRequest.getProfileType()));
            rrg.addItem(RoutingRequestGroupItem.forRoutingLogId(routingLogId));
            return rrg;
        });


        //then
        assertThat(queueService.reenqueueDelay(new QueueRoutingRequestGroupPayload("requestId",
                routingRequestGroup.getId())))
                .isEqualTo(Optional.empty());
    }


    private RoutingRequestGroup buildRoutingRequestGroup() {
        RoutingRequestGroup rrg = new RoutingRequestGroup();
        rrg.setShiftDate(SHIFT_DATE);
        rrg.setSortingCenterId(SortingCenter.DEFAULT_SC_ID);
        return rrg;
    }

    private RoutingRequest buildManualRequest(RoutingRequestGroup rrg) {
        return RoutingRequest.builder()
                .routingRequestGroupId(rrg.getId())
                .mockType(RoutingMockType.MANUAL)
                .profileType(RoutingProfileType.REROUTE)
                .items(List.of())
                .depot(new RoutingDepot(SortingCenter.DEFAULT_SC_ID, null, null))
                .users(Set.of())
                .requestId(REQUEST_ID)
                .routingDate(SHIFT_DATE)
                .build();
    }

    private RoutingRequest buildGroupRequest(RoutingRequestGroup rrg, String routingRequestId) {
        return RoutingRequest.builder()
                .routingRequestGroupId(rrg.getId())
                .mockType(RoutingMockType.REAL)
                .profileType(RoutingProfileType.GROUP)
                .items(List.of())
                .depot(new RoutingDepot(SortingCenter.DEFAULT_SC_ID, null, null))
                .users(Set.of())
                .baseRoutingRequestId(routingRequestId)
                .requestId(REQUEST_ID)
                .routingDate(SHIFT_DATE)
                .build();
    }

    private RoutingRequest buildGroupRequest(RoutingRequestGroup rrg) {
        return buildGroupRequest(rrg, "baseRoutingRequestId");
    }

    private void addCourierToSchedule() {
        userHelper.findOrCreateUser(1234567L, SHIFT_DATE);
    }
}
