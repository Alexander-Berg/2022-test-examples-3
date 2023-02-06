package ru.yandex.market.wms.transportation.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.transportation.core.domain.TransportOrderStatus;
import ru.yandex.market.wms.transportation.core.domain.TransportOrderType;
import ru.yandex.market.wms.transportation.core.domain.TransportUnitQualifier;
import ru.yandex.market.wms.transportation.exception.UpdateTransportOrderException;
import ru.yandex.market.wms.transportation.model.TransportOrder;
import ru.yandex.market.wms.transportation.model.rule.DirectDestinationRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.wms.transportation.core.domain.TransportUnitType.CONTAINER;


public class TransportOrderRepositoryTest extends IntegrationTest {

    @Autowired
    private TransportOrderRepository repository;

    @Test
    @DatabaseSetup("/repository/transport-order/create-duplicate/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/create-duplicate/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createDuplicateTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .id("1cd02aae-257d-11eb-adc1-0242ac120002")
                .fromCell("C1")
                .toCell("C20")
                .destinationRule(DirectDestinationRule.builder()
                        .cells(Collections.singleton("C20"))
                        .build())
                .unit(TransportUnitQualifier.builder()
                        .id("1")
                        .type(CONTAINER)
                        .build())
                .actualCell("C1")
                .transporterId("1")
                .assignee("TEST")
                .status(TransportOrderStatus.NEW)
                .priority(0)
                .type(TransportOrderType.MANUAL)
                .build();
        assertThrows(DuplicateKeyException.class, () -> repository.create(transportOrder, "TEST"));
    }

    @Test
    @DatabaseSetup("/repository/transport-order/create-new/initial-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/create-new/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void createNewTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .id("1cd02aae-257d-11eb-adc1-0242ac120002")
                .fromCell("C1")
                .toCell("C20")
                .destinationRule(DirectDestinationRule.builder()
                        .cells(Collections.singleton("C20"))
                        .build())
                .unit(TransportUnitQualifier.builder()
                        .id("1")
                        .type(CONTAINER)
                        .build())
                .actualCell("C1")
                .transporterId("1")
                .assignee("TEST")
                .status(TransportOrderStatus.NEW)
                .priority(0)
                .type(TransportOrderType.AUTOMATIC)
                .build();
        repository.create(transportOrder, "TEST");
    }

    @Test
    @DatabaseSetup("/repository/transport-order/delete-when-exist/initial-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/delete-when-exist/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void deleteWhenExistTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .transporterId("1cd02aae-257d-11eb-adc1-0242ac120002")
                .build();
        repository.delete(transportOrder);
    }

    @Test
    @DatabaseSetup("/repository/transport-order/delete-when-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/delete-when-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void deleteWhenNotExistTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .transporterId("1cd02aae-257d-11eb-adc1-0242ac120002")
                .build();
        repository.delete(transportOrder);
    }

    @Test
    @DatabaseSetup("/repository/transport-order/read-when-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/read-when-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void readWhenExistTest() {
        Optional<TransportOrder> optional = repository.read("1cd02aae-257d-11eb-adc1-0242ac120002");
        assertThat(optional).isNotEmpty();
        TransportOrder transportOrder = optional.get();
        assertEquals("1cd02aae-257d-11eb-adc1-0242ac120002", transportOrder.getId());
        assertEquals("C1", transportOrder.getFromCell());
        assertEquals("C20", transportOrder.getToCell());
        assertEquals("1", transportOrder.getUnit().getId());
        assertEquals(CONTAINER, transportOrder.getUnit().getType());
        assertEquals("C1", transportOrder.getActualCell());
        assertEquals("1", transportOrder.getTransporterId());
        assertEquals("TEST", transportOrder.getAssignee());
        assertEquals(TransportOrderStatus.ASSIGNED, transportOrder.getStatus());
        assertEquals(0, transportOrder.getPriority());
        assertEquals(TransportOrderType.MANUAL, transportOrder.getType());
    }

    @Test
    @DatabaseSetup("/repository/transport-order/read-when-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/read-when-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void readWhenNotExistTest() {
        Optional<TransportOrder> optional = repository.read("1cd02aae-257d-11eb-adc1-0242ac120002");
        assertThat(optional).isEmpty();
    }

    @Test
    @DatabaseSetup("/repository/transport-order/update-read-only-field/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/update-read-only-field/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateReadOnlyFieldTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .id("1cd02aae-257d-11eb-adc1-0242ac120002")
                .fromCell("C11")
                .toCell("C21")
                .unit(TransportUnitQualifier.builder()
                        .id("2")
                        .type(null)
                        .build())
                .actualCell("C1")
                .transporterId("1")
                .assignee("TEST")
                .status(TransportOrderStatus.ASSIGNED)
                .priority(0)
                .rowVersion("1234")
                .build();
        repository.update(transportOrder, "TEST");
    }

    @Test
    @DatabaseSetup("/repository/transport-order/update-when-exist/initial-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/update-when-exist/final-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateWhenExistTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .id("1cd02aae-257d-11eb-adc1-0242ac120002")
                .fromCell("C1")
                .toCell("C20")
                .unit(TransportUnitQualifier.builder()
                        .id("1")
                        .type(CONTAINER)
                        .build())
                .transporterId("1")
                .actualCell("C20")
                .assignee(null)
                .status(TransportOrderStatus.FINISHED)
                .priority(1)
                .rowVersion("1234")
                .build();
        repository.update(transportOrder, "TEST");
    }

    @Test
    @DatabaseSetup("/repository/transport-order/update-when-not-exist/immutable-state.xml")
    @ExpectedDatabase(value = "/repository/transport-order/update-when-not-exist/immutable-state.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void updateWhenNotExistTest() {
        TransportOrder transportOrder = TransportOrder.builder()
                .id("1cd02aae-257d-11eb-adc1-0242ac120002")
                .fromCell("C1")
                .toCell("C20")
                .unit(TransportUnitQualifier.builder()
                        .id("1")
                        .type(CONTAINER)
                        .build())
                .actualCell("C1")
                .transporterId("1")
                .assignee("TEST")
                .status(TransportOrderStatus.NEW)
                .priority(0)
                .rowVersion("9999")
                .build();
        assertThrows(UpdateTransportOrderException.class, () -> repository.update(transportOrder, "TEST"));
    }

    @Test
    @DatabaseSetup("/repository/transport-order/find-active/immutable.xml")
    @ExpectedDatabase(
            value = "/repository/transport-order/find-active/immutable.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void findActiveFromDate() {
        LocalDateTime fromDate = LocalDateTime.parse("2020-04-01T14:00:00");
        List<TransportOrder> transportOrders = repository.findActiveAutomaticFromDate(fromDate);

        assertAll(
                () -> assertEquals(3, transportOrders.size()),
                () -> assertEquals(3, transportOrders.stream().filter(m -> !m.getStatus().isCompleted()).count()),
                () -> assertEquals(3, transportOrders.stream().filter(m -> m.getEditDate().isAfter(fromDate)).count())
        );
    }

}
