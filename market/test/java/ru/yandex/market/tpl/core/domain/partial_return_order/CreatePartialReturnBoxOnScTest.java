package ru.yandex.market.tpl.core.domain.partial_return_order;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.domain.clientreturn.CreatedSource;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.sc.clientreturn.ScClientReturnMapper;
import ru.yandex.market.tpl.core.domain.sc.clientreturn.model.ScClientReturn;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.dbqueue.model.QueueType.CREATE_CLIENT_RETURN;

@RequiredArgsConstructor
public class CreatePartialReturnBoxOnScTest extends TplAbstractTest {
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final OrderGenerateService orderGenerateService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final ScClientReturnMapper scClientReturnMapper;
    private final TransactionTemplate transactionTemplate;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final UserShiftRepository userShiftRepository;
    private final TestDataFactory testDataFactory;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final Clock clock;


    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order order;
    private OrderDeliveryTask orderDeliveryTask;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(ts -> {
                    user = testUserHelper.findOrCreateUser(1L);
                    shift = testUserHelper.findOrCreateOpenShiftForSc(
                            LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId()
                    );
                    userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));

                    order = orderGenerateService.createOrder(
                            OrderGenerateService.OrderGenerateParam.builder()
                                    .items(
                                            OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).build()
                                    )
                                    .build()
                    );

                    userShiftReassignManager.assign(userShift, order);
                    testUserHelper.checkinAndFinishPickup(userShift);
                    orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
                    partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
                    return null;
                }
        );
    }

    @Test
    void createPartialReturnOnSc() {
        var box = transactionTemplate.execute(ts -> {
            PartialReturnOrder partialReturnOrder = partialReturnOrderRepository.findByOrder(order).get();
            partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder, orderDeliveryTask, 3);
            return partialReturnOrderRepository.findByOrder(order).get().getBoxes();
        });

        dbQueueTestUtil.assertQueueHasSize(CREATE_CLIENT_RETURN, 3);
    }

    @Test
    void mapPartialReturnOnSc() {
        var box = transactionTemplate.execute(ts -> {
            PartialReturnOrder partialReturnOrder = partialReturnOrderRepository.findByOrder(order).get();
            partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder, orderDeliveryTask, 1);
            return partialReturnOrderRepository.findByOrder(order).get().getBoxes().get(0);
        });

        ScClientReturn scClientReturn = transactionTemplate.execute(ts ->
            scClientReturnMapper.mapPartialReturnBox(box, userShiftRepository.findByIdOrThrow(userShift.getId()))
        );

        assertThat(scClientReturn.getBarcode()).isEqualTo(box.getBarcode());
        assertThat(scClientReturn.getCreatedSource()).isEqualTo(CreatedSource.SELF);
        assertThat(scClientReturn.getReturnId()).isEqualTo(box.getBarcode());
    }

}
