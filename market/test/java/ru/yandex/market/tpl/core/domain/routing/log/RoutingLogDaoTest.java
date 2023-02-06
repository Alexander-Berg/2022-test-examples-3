package ru.yandex.market.tpl.core.domain.routing.log;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.external.routing.api.RoutingApiEvent;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequest;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.model.MvrpResponse;
import ru.yandex.market.tpl.core.external.routing.vrp.model.SolutionMetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RoutingLogDaoTest {

    public static final String NOT_EXISTED_REQUEST_ID = UUID.randomUUID().toString();
    private final RoutingRequestGroupRepository routingRequestGroupRepository;
    private final RoutingApiDataHelper routingApiDataHelper = new RoutingApiDataHelper();
    private final TransactionTemplate transactionTemplate;
    private final RoutingLogDao routingLogDao;

    @Test
    void getRoutingRequestFromGroup() {
        RoutingRequestGroup routingRequestGroup = routingRequestGroupRepository.save(new RoutingRequestGroup());

        addRequestToGroup(routingRequestGroup);
        addRequestToGroup(routingRequestGroup);
        routingRequestGroupRepository.saveAndFlush(routingRequestGroup);

        List<RoutingRequest> routingRequests = transactionTemplate.execute(st ->
                routingLogDao.findRoutingRequestsForGroup(routingRequestGroup.getId())
        );
        assertThat(routingRequests).hasSize(2);
    }

    @Test
    void getFindResultByProcessingId_processingAndProfile() {
        //given
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, LocalDate.now(), 77, 0)
                .withProfileType(RoutingProfileType.PARTIAL);
        var rawResponse = new MvrpResponse();
        rawResponse.setMetrics(OBJECT_GENERATOR.nextObject(SolutionMetrics.class));
        var processingId = UUID.randomUUID().toString();
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(),
                rawResponse));
        routingLogDao.updateAtFinished(
                routingRequest.getRequestId(),
                routingApiDataHelper.mockResult(routingRequest, false),
                rawResponse);
        //when
        Optional<RoutingResultWithShiftDate> result = transactionTemplate.execute(st ->
                routingLogDao.findResultByProcessingId(processingId, RoutingProfileType.PARTIAL)
        );

        //then
        assertThat(result).isPresent();
    }

    @Test
    void findResultWithShiftDateByRequestId() {
        //given
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, LocalDate.now(), 77, 0)
                .withProfileType(RoutingProfileType.PARTIAL);
        var rawResponse = new MvrpResponse();
        rawResponse.setMetrics(OBJECT_GENERATOR.nextObject(SolutionMetrics.class));
        var processingId = UUID.randomUUID().toString();
        routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest, routingRequest.getProfileType()));
        routingLogDao.updateAtProcessingStart(routingRequest.getRequestId(), processingId);
        routingLogDao.updateRawResponse(new RoutingApiEvent.ResponseReceived(routingRequest.getRequestId(),
                rawResponse));
        routingLogDao.updateAtFinished(
                routingRequest.getRequestId(),
                routingApiDataHelper.mockResult(routingRequest, false),
                rawResponse);

        //when
        Optional<RoutingResultWithShiftDate> result = transactionTemplate.execute(st ->
                routingLogDao.findResultWithShiftDateByRequestId(routingRequest.getRequestId())
        );

        //then
        assertThat(result).isPresent();
    }

    @Test
    void findResultWithShiftDateByRequestId_whenNotExists() {
        //when
        assertThat((Optional<?>) transactionTemplate.execute(st ->
                routingLogDao.findResultWithShiftDateByRequestId(NOT_EXISTED_REQUEST_ID)
        )).isEmpty();
    }

    @SneakyThrows
    @Test
    void getRoutingRequestFromGroupLock() {
        RoutingRequestGroup routingRequestGroup = transactionTemplate.execute(st -> {
            RoutingRequestGroup rrg = routingRequestGroupRepository.save(new RoutingRequestGroup());
            addRequestToGroup(rrg);
            return rrg;
        });
        routingRequestGroupRepository.saveAndFlush(routingRequestGroup);

        transactionTemplate.execute(st -> {
            List<RoutingRequest> requests = routingLogDao.findRoutingRequestsForGroup(routingRequestGroup.getId());
            log.info("First thread requests: {}", requests);
            assertThat(requests).hasSize(1);
            Future<List<RoutingRequest>> future = findRoutingRequestsForGroupAsync(routingRequestGroup);
            assertThatThrownBy(future::get).hasCauseInstanceOf(CannotAcquireLockException.class);
            return requests;
        });
        Future<List<RoutingRequest>> future = findRoutingRequestsForGroupAsync(routingRequestGroup);
        assertThat(future.get()).hasSize(1);
    }

    private void addRequestToGroup(RoutingRequestGroup routingRequestGroup) {
        RoutingRequest routingRequest = routingApiDataHelper.getRoutingRequest(123L, LocalDate.now(), 1, 0);
        Long routingLogId = routingLogDao.createRecord(new RoutingApiEvent.Started(routingRequest,
                routingRequest.getProfileType()));

        RoutingRequestGroupItem item = new RoutingRequestGroupItem();
        item.setRoutingLogId(routingLogId);
        routingRequestGroup.addItem(item);
    }

    private Future<List<RoutingRequest>> findRoutingRequestsForGroupAsync(RoutingRequestGroup routingRequestGroup) {
        return Executors.newSingleThreadExecutor().submit(() ->
                transactionTemplate.execute(st2 ->
                        routingLogDao.findRoutingRequestsForGroup(routingRequestGroup.getId())
                )
        );
    }
}
