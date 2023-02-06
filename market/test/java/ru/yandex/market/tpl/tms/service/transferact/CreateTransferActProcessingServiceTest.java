package ru.yandex.market.tpl.tms.service.transferact;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.transferact.client.api.TransferApi;
import ru.yandex.market.tpl.common.transferact.client.model.ActorTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDirectionDto;
import ru.yandex.market.tpl.common.transferact.client.model.RegistryDto;
import ru.yandex.market.tpl.common.transferact.client.model.SignatureDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferCreateRequestDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierDto;
import ru.yandex.market.tpl.common.transferact.client.model.TransferQualifierTypeDto;
import ru.yandex.market.tpl.common.transferact.client.model.TwoActorQualifierDto;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.UserShiftTestHelper;
import ru.yandex.market.tpl.core.domain.transferact.TransferAct;
import ru.yandex.market.tpl.core.domain.transferact.TransferActStatus;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
class CreateTransferActProcessingServiceTest extends TplTmsAbstractTest {

    private final TestDataFactory testDataFactory;
    private final TestUserHelper userHelper;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftCommandDataHelper userShiftCommandDataHelper;
    private final UserShiftTestHelper userShiftTestHelper;
    private final RoutePointRepository routePointRepository;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final UserPropertyService userPropertyService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TransferApi transferApi;

    private User user;
    private Shift shift;
    private Long userShiftId;
    private Long pickupRoutePointId;
    private Long orderId1;
    private Long orderId2;

    @BeforeEach
    void beforeEach() {
        transactionTemplate.execute(status -> {
            user = userHelper.findOrCreateUser(991L);
            userPropertyService.addPropertyToUser(user, UserProperties.TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, true);
            return null;
        });

        shift = userHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), 18769);

        orderId1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        orderId2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CASH)
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .build()
        ).getId();

        userShiftId = userShiftTestHelper.start(UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(userShiftCommandDataHelper.taskOrderPickup(clock.instant()))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr1", 12, orderId1))
                .routePoint(userShiftCommandDataHelper.taskUnpaid("addr2", 12, orderId2))
                .mergeStrategy(SimpleStrategies.NO_MERGE)
                .build()
        );
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                userShift.getShift().getSortingCenter(),
                SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED,
                false
        );
        pickupRoutePointId = userShift.getCurrentRoutePoint().getId();

        var pickupTask = userShift.getCurrentRoutePoint().streamPickupTasks().findFirst().orElseThrow();

        userShiftTestHelper.arriveAtRoutePoint(userShift);
        userShiftTestHelper.startOrderPickup(userShift, pickupTask);
        userShiftTestHelper.createTransferAct(userShift, pickupTask, List.of(orderId1), List.of(orderId2));

        clearAfterTest(user);
        clearAfterTest(shift);
    }

    @Test
    void createTransfer() {
        TransferDto transferDto = new TransferDto();
        transferDto.setId("123");
        ArgumentCaptor<TransferCreateRequestDto> requestCaptor = ArgumentCaptor.forClass(TransferCreateRequestDto.class);
        when(transferApi.transferPut(requestCaptor.capture())).thenReturn(transferDto);

        dbQueueTestUtil.executeAllQueueItems(QueueType.TRANSFER_ACT_CREATE);

        transactionTemplate.execute(status -> {
            var userShiftResult = userShiftRepository.findByIdOrThrow(userShiftId);
            var pickupRoutePointResult = routePointRepository.findByIdOrThrow(pickupRoutePointId);
            var pickupTaskResult = pickupRoutePointResult.streamPickupTasks().findFirst().orElseThrow();

            TransferAct transferAct = pickupTaskResult.getTransferActs().get(0);
            assertThat(transferAct.getStatus()).isEqualTo(TransferActStatus.WAITING_FOR_SIGNATURE);
            assertThat(transferAct.getExternalId()).isEqualTo(transferDto.getId());

            TransferCreateRequestDto request = requestCaptor.getValue();
            assertThat(request.getIdempotencyKey()).isEqualTo("tpl-" + transferAct.getId());

            RegistryDto registry = request.getRegistry();
            assertThat(registry.getDirection()).isEqualTo(RegistryDirectionDto.RECEIVER);
            assertThat(registry.getItems().size()).isEqualTo(pickupTaskResult.getPickupOrderIds().size());
            assertThat(registry.getSkippedItems().size()).isEqualTo(pickupTaskResult.getSkippedOrderIds().size());

            SignatureDto signature = request.getSignature();
            assertThat(signature.getSignerId()).isEqualTo(String.valueOf(userShiftResult.getUser().getUid()));
            assertThat(signature.getSignerName()).isEqualTo(userShiftResult.getUser().getName());
            assertThat(signature.getSignatureData())
                    .isEqualTo(DigestUtils.sha256Hex(String.valueOf(userShiftResult.getUser().getUid())));

            TransferQualifierDto transferQualifier = request.getTransferQualifier();
            assertThat(transferQualifier.getType()).isEqualTo(TransferQualifierTypeDto.TWO_ACTOR);
            TwoActorQualifierDto twoActorQualifier = transferQualifier.getTwoActorQualifier();

            assertThat(twoActorQualifier.getActorTo().getExternalId())
                    .isEqualTo(String.valueOf(userShiftResult.getUser().getUid()));
            assertThat(twoActorQualifier.getActorTo().getType()).isEqualTo(ActorTypeDto.MARKET_COURIER);

            assertThat(twoActorQualifier.getActorFrom().getExternalId())
                    .isEqualTo(String.valueOf(userShiftResult.getShift().getSortingCenter().getLogisticPointId()));
            assertThat(twoActorQualifier.getActorFrom().getType()).isEqualTo(ActorTypeDto.MARKET_SC);

            return null;
        });
    }

}
