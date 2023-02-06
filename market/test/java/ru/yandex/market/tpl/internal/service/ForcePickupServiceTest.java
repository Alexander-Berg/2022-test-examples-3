package ru.yandex.market.tpl.internal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.config.props.YardClientEventsProducerProperties;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ForcePickupServiceTest {
    private final ForcePickupService forcePickupService;

    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TestDataFactory testDataFactory;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final YardClientEventsProducerProperties yardClientProperties;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
    }

    @ParameterizedTest
    @ArgumentsSource(TestDataProvider.class)
    void forcePickupWithOtherInitialOrderFlowStatus(
            OrderFlowStatus initialOrderFlowStatus,
            OrderFlowStatus expectedOrderFlowStatusAfterForcePickup,
            boolean isTerminalStatusAfterForcePickup

    ) {
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .flowStatus(initialOrderFlowStatus)
                .build());
        userShiftReassignManager.assign(userShift, order);

        forcePickupService.forcePickup(user.getUid());

        assertThat(order.getOrderFlowStatus()).isEqualTo(expectedOrderFlowStatusAfterForcePickup);
        userShift.streamDeliveryTasks()
                .forEach(task -> assertThat(task.isInTerminalStatus()).isEqualTo(isTerminalStatusAfterForcePickup));
        assertThat(
                dbQueueTestUtil.getTasks(QueueType.LOGBROKER_WRITER).stream()
                        .filter(it -> it.getPayload().isPresent()
                                && it.getPayload().get().toString().contains(yardClientProperties.getTopic()))
                        .count()
        ).isEqualTo(1);
    }

    static class TestDataProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(
                            OrderFlowStatus.SORTING_CENTER_PREPARED,
                            OrderFlowStatus.TRANSPORTATION_RECIPIENT,
                            false
                    ),
                    Arguments.of(
                            OrderFlowStatus.SORTING_CENTER_CREATED,
                            OrderFlowStatus.SORTING_CENTER_CREATED,
                            true
                    )
            );
        }
    }

}
