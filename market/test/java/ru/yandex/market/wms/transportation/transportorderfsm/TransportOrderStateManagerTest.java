package ru.yandex.market.wms.transportation.transportorderfsm;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Loc;
import ru.yandex.market.wms.common.spring.dao.implementation.AnomalyLotDao;
import ru.yandex.market.wms.common.spring.dao.implementation.LocDAO;
import ru.yandex.market.wms.common.spring.service.SerialInventoryService;
import ru.yandex.market.wms.transportation.exception.FsmTransitionFailedException;
import ru.yandex.market.wms.transportation.model.TransportOrder;
import ru.yandex.market.wms.transportation.repository.TransportOrderRepository;
import ru.yandex.market.wms.transportation.service.TransportOrderStateManager;
import ru.yandex.market.wms.transportation.service.enricher.TransportOrderEnricher;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class TransportOrderStateManagerTest extends IntegrationTest {
    private final String userId = "transportation";

    @Autowired
    private TransportOrderStateManager orderStateManager;

    @Autowired
    private TransportOrderRepository transportOrderRepository;

    @Autowired
    private ObjectFactory<TransportOrderEnricher> enricherFactory;

    @Autowired
    private SerialInventoryService serialInventoryService;

    @Autowired
    private LocDAO locDAO;

    @Autowired
    private AnomalyLotDao anomalyLotDao;

    @Test
    @DatabaseSetup("/order/state-manager/assigned-to-in-progress/1/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/assigned-to-in-progress/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignedToInProgressWithSerialInventoryTest() {
        moveTransportOrder("F-123");
    }

    @Test
    @DatabaseSetup("/order/state-manager/assigned-to-in-progress/2/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/assigned-to-in-progress/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignedToInProgressWithAnomalyLotTest() {
        moveTransportOrder("F-123");
    }

    @Test
    @DatabaseSetup("/order/state-manager/in-progress-to-finished/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/in-progress-to-finished/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void inProgressToFinishedTest() {
        moveTransportOrder("F-123");
    }

    @Test
    @DatabaseSetup("/order/state-manager/in-progress-to-failed/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/in-progress-to-failed/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void inProgressToFailedTest() {
        moveTransportOrder("F-123");
    }

    @Test
    @DatabaseSetup("/order/state-manager/in-progress-to-failed-with-status/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/in-progress-to-failed-with-status/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void inProgressToFailedWithStatusTest() {
        moveTransportOrder("F-123", TransportUnitStatus.ERROR_NOSCAN);
    }

    @Test
    @DatabaseSetup("/order/state-manager/assigned-to-failed/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/assigned-to-failed/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignedToFailedTest() {
        moveTransportOrder("F-123");
    }

    /*
     * Попытка изменить статус ордера в терминальном статусе
     * */
    @Test
    @DatabaseSetup("/order/state-manager/validation-fail/terminal-state/immutable.xml")
    @ExpectedDatabase(value = "/order/state-manager/validation-fail/terminal-state/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void validationFailAttemptToProcessTransportOrderInTerminalStatusTest() {
        final String containerId = "F-123";
        TransportOrder transportOrder = transportOrderRepository.tryGetLastOrderByUnitId(containerId);
        TransportOrderEnricher enricher = enricherFactory.getObject();
        enricher.enrich(transportOrder);
        ListAppender<ILoggingEvent> appender = attachLogListAppender(TransportOrderStateMachineConfig.class);
        Loc loc = getActualLoc(containerId);
        assertThatThrownBy(() -> orderStateManager.updateState(transportOrder, loc, TransportUnitStatus.FINISHED))
                .isInstanceOf(FsmTransitionFailedException.class);

        assertEquals(1, appender.list.stream().filter(f -> f.getFormattedMessage().contains(
                "Event CHANGE_LOCATION not accepted."
        )).count());
    }

    /*
     *  Перевод ордера в статус CANCELED
     */
    @Test
    @DatabaseSetup("/order/state-manager/new-to-canceled/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/new-to-canceled/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void newToCanceledTest() {
        TransportOrder transportOrder = transportOrderRepository.tryRead("1cd02aae-257d-11eb-adc1-0242ac120002");
        TransportOrderEnricher enricher = enricherFactory.getObject();
        enricher.enrich(transportOrder);
        transportOrder = orderStateManager.cancelOrder(transportOrder);
        transportOrderRepository.update(transportOrder, userId);
    }

    @Test
    @DatabaseSetup("/order/state-manager/assigned-to-canceled/before.xml")
    @ExpectedDatabase(value = "/order/state-manager/assigned-to-canceled/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void assignedToCanceledTest() {
        TransportOrder transportOrder = transportOrderRepository.tryRead("1cd02aae-257d-11eb-adc1-0242ac120002");
        TransportOrderEnricher enricher = enricherFactory.getObject();
        enricher.enrich(transportOrder);
        transportOrder = orderStateManager.cancelOrder(transportOrder);
        transportOrderRepository.update(transportOrder, userId);
    }

    private void moveTransportOrder(String containerId) {
        moveTransportOrder(containerId, TransportUnitStatus.FINISHED);
    }

    private void moveTransportOrder(String containerId, TransportUnitStatus externalStatus) {
        TransportOrder transportOrder = transportOrderRepository.tryGetLastOrderByUnitId(containerId);
        Loc loc = getActualLoc(containerId);
        TransportOrderEnricher enricher = enricherFactory.getObject();
        enricher.enrich(transportOrder);
        transportOrder = orderStateManager.updateState(transportOrder, loc, externalStatus);
        transportOrderRepository.update(transportOrder, userId);
    }

    private Loc getActualLoc(String containerId) {
        String actualCell = serialInventoryService.findIdLoc(containerId).orElseGet(
                () -> anomalyLotDao.findAllLotsInContainer(containerId).get(0).getAnomalyContainer().getLoc()
        );
        return locDAO.findById(actualCell).get();
    }
}
