package ru.yandex.market.logistics.lom.service.tracker.listener.implementation;

import java.time.Clock;
import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistics.lom.dto.queue.LomSegmentCheckpoint;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentStatus;
import ru.yandex.market.logistics.lom.repository.InternalVariableRepository;
import ru.yandex.market.logistics.lom.repository.OrderRepository;
import ru.yandex.market.logistics.lom.utils.WaybillUtils;

@DisplayName("Обработка 41, 44, 46, 47 чекпойнтов: перенос даты доставки")
public class DeliveryDateUpdatedListenerTest extends AbstractCheckpointListenerTest {

    @Autowired
    private DeliveryDateUpdatedListener listener;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private InternalVariableRepository internalVariableRepository;

    @SpyBean
    private Clock clock;

    @Test
    @DatabaseSetup("/service/listener/deliveryDateUpdated/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/listener/deliveryDateUpdated/after/expected.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Приходит 44 чекпойнт и создается change order request на изменение даты доставки")
    void testProcess() {
        transactionTemplate.execute(st -> {
            Order order = orderRepository.getById(1L);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, 2L);
            LomSegmentCheckpoint checkpoint =
                createCheckpoint(SegmentStatus.TRANSIT_UPDATED_BY_SHOP);

            listener.process(order, checkpoint, segment, null);
            return null;
        });
    }

    @Test
    @DatabaseSetup("/service/listener/deliveryDateUpdated/before/setup.xml")
    @ExpectedDatabase(
        value = "/service/listener/deliveryDateUpdated/after/recalculate_route.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Включена опция пересчета маршрута и приходит 44 чекпойнт")
    void processWithEnabledRecalculateRouteFlag() {
        Mockito.doReturn(Instant.ofEpochSecond(3600)).when(clock).instant();
        transactionTemplate.execute(st -> {
            Order order = orderRepository.getById(1L);
            WaybillSegment segment = WaybillUtils.getSegmentById(order, 2L);
            LomSegmentCheckpoint checkpoint =
                createCheckpoint(SegmentStatus.TRANSIT_UPDATED_BY_SHOP);

            internalVariableRepository.save(new InternalVariable()
                .setType(InternalVariableType.RECALCULATE_ROUTE_WHEN_UPDATE_DELIVERY_DATE)
                .setValue(segment.getPartnerId().toString()));

            listener.process(order, checkpoint, segment, null);
            return null;
        });
    }

}
