package ru.yandex.market.clab.tms.service.erp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.clab.common.config.component.ServicesConfig;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodRepository;
import ru.yandex.market.clab.common.service.requested.good.RequestedGoodService;
import ru.yandex.market.clab.common.service.requested.movement.RequestedMovementService;
import ru.yandex.market.clab.common.test.RandomTestUtils;
import ru.yandex.market.clab.db.config.ControlledClockConfiguration;
import ru.yandex.market.clab.db.config.MainLiquibaseConfig;
import ru.yandex.market.clab.db.jooq.generated.enums.MovementDirection;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodMovementState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedGoodState;
import ru.yandex.market.clab.db.jooq.generated.enums.RequestedMovementState;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGood;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedGoodMovement;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.RequestedMovement;
import ru.yandex.market.clab.db.test.BasePgaasIntegrationTest;
import ru.yandex.market.clab.test.config.TestRemoteServicesConfiguration;
import ru.yandex.market.clab.tms.config.MovementControlConfig;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrder;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderItem;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderState;
import ru.yandex.market.clab.tms.service.erp.data.TransferOrderStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
    properties = {
        "spring.main.allow-bean-definition-overriding=true"
    },
    classes = {
        ServicesConfig.class,
        TestRemoteServicesConfiguration.class,
        ControlledClockConfiguration.class,
        MainLiquibaseConfig.class,
        MovementControlConfig.class,
        MovementControlServiceTest.MovementControlServiceTestConfiguration.class
    })
@ActiveProfiles("test")
public class MovementControlServiceTest extends BasePgaasIntegrationTest {

    @MockBean(name = "erpDataSource")
    private DataSource erpDataSource;

    @Autowired
    private MovementControlService movementControlService;

    @Autowired
    private RequestedMovementService requestedMovementService;

    @Autowired
    private RequestedGoodService requestedGoodService;

    @Autowired
    private RequestedGoodRepository requestedGoodRepository;

    @Autowired
    private MovementControlRepositoryStub movementControlRepositoryStub;

    @Configuration
    public static class MovementControlServiceTestConfiguration {
        @Primary
        @Bean("movementControlRepository")
        public MovementControlRepositoryStub movementControlRepository() {
            return new MovementControlRepositoryStub();
        }
    }

    @Test
    public void testProcessPlannedMovement() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);
        RequestedMovement movement2 = createAndSaveIncomingMovement(RequestedMovementState.NEW);
        RequestedMovement movement3 = createAndSaveIncomingMovement(RequestedMovementState.IN_PROCESS);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good3 = createAndSaveGood(
            movement2, RequestedGoodState.NEW, RequestedGoodMovementState.NEW);
        RequestedGood good4 = createAndSaveGood(
            movement3, RequestedGoodState.INCOMING, RequestedGoodMovementState.PLANNED);

        movementControlService.planRequestedMovements();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());
        RequestedMovement updatedMovement2 = requestedMovementService.getById(movement2.getId());
        RequestedMovement updatedMovement3 = requestedMovementService.getById(movement3.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));
        List<RequestedGoodMovement> goodMovements2 = requestedGoodService.getGoodMovements(
            movement2.getId(), Collections.singletonList(good3));
        List<RequestedGoodMovement> goodMovements3 = requestedGoodService.getGoodMovements(
            movement3.getId(), Collections.singletonList(good4));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.REQUESTED);
        assertThat(updatedMovement2).isEqualTo(movement2);
        assertThat(updatedMovement3).isEqualTo(movement3);
        assertThat(goodMovements1).allSatisfy(
            gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REQUESTED)
        );
        assertThat(goodMovements2).allSatisfy(
            gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.NEW)
        );
        assertThat(goodMovements3).allSatisfy(
            gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.PLANNED)
        );

        List<String> orderIds = Stream.of(movement1, movement2, movement3)
            .map(RequestedMovement::getId)
            .map(String::valueOf)
            .collect(Collectors.toList());
        List<TransferOrder> orders = movementControlRepositoryStub.getTransferOrders(orderIds);

        assertThat(orders).singleElement().satisfies(item -> {
            assertThat(item).extracting(TransferOrder::getOrderId).isEqualTo(String.valueOf(movement1.getId()));
            assertThat(item).extracting(TransferOrder::getState).isEqualTo(TransferOrderState.NEW);
        });

        List<TransferOrderItem> ordersItems = movementControlRepositoryStub.getOrderItems(orderIds);
        assertThat(ordersItems)
            .hasSize(2)
            .anySatisfy(item -> {
                assertThat(item).extracting(TransferOrderItem::getSupplierId)
                    .isEqualTo(good1.getSupplierId().toString());
                assertThat(item).extracting(TransferOrderItem::getSupplierSkuId)
                    .isEqualTo(good1.getSupplierSkuId());
            })
            .anySatisfy(item -> {
                assertThat(item).extracting(TransferOrderItem::getSupplierId)
                    .isEqualTo(good2.getSupplierId().toString());
                assertThat(item).extracting(TransferOrderItem::getSupplierSkuId)
                    .isEqualTo(good2.getSupplierSkuId());
            });
    }

    @Test
    public void testOrderProcessingStarted() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        movementControlRepositoryStub.startProcessing(movement1.getId().toString());

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.REQUESTED);

        assertThat(goodMovements1)
            .extracting(RequestedGoodMovement::getState)
            .containsOnly(RequestedGoodMovementState.REQUESTED);
    }

    @Test
    public void testOrderInProgress() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.IN_PROCESS, "InProgress",
            "AxOrderId", null);

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.IN_PROCESS);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");

        assertThat(goodMovements1)
            .extracting(RequestedGoodMovement::getState)
            .containsOnly(RequestedGoodMovementState.CONFIRMED);
    }

    @Test
    public void testOrderProcessed() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.PROCESSED, "Processed",
            "AxOrderId", "FfOrderId");
        List<TransferOrderItem> ordersItems = movementControlRepositoryStub
            .getOrderItems(Collections.singletonList(orderId));
        ordersItems.forEach(i -> movementControlRepositoryStub.createItemStatus(i, true));

        // Check that we're not changing state if inbound is already received
        RequestedGoodMovement rgm = requestedGoodRepository.getGoodMovement(movement1.getId(), good1.getId());
        requestedGoodRepository.updateGoodMovement(rgm.setState(RequestedGoodMovementState.IN_PROCESS));

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.PROCESSED);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getFfRequestIds)
            .isEqualTo("FfOrderId");

        // We received inbound with one good only
        assertThat(goodMovements1)
            .anySatisfy(
                gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.IN_PROCESS)
            )
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Товар отсутствует в поставке");
                }
            );
    }

    @Test
    public void testOrderPartiallyProcessed() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.PROCESSED, "Processed",
            "AxOrderId", "FfOrderId");
        List<TransferOrderItem> ordersItems = movementControlRepositoryStub
            .getOrderItems(Collections.singletonList(orderId));
        TransferOrderItem successItem = ordersItems.get(0);
        movementControlRepositoryStub.createItemStatus(successItem, true);
        movementControlRepositoryStub.createItemStatus(ordersItems.get(1), false);

        // Got inbound with one good.
        good1.setState(RequestedGoodState.INCOMING);
        good1 = requestedGoodRepository.save(good1);
        RequestedGoodMovement rgm = requestedGoodRepository.getGoodMovement(movement1.getId(), good1.getId());
        requestedGoodRepository.updateGoodMovement(rgm.setState(RequestedGoodMovementState.IN_PROCESS));

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        RequestedGood updatedGood1 = requestedGoodService.getById(good1.getId());
        RequestedGood updatedGood2 = requestedGoodService.getById(good2.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.PROCESSED);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getFfRequestIds)
            .isEqualTo("FfOrderId");

        assertThat(updatedGood1.getRequestedMovementId()).isEqualTo(movement1.getId());
        assertThat(updatedGood1.getState()).isEqualTo(RequestedGoodState.INCOMING);
        assertThat(updatedGood2.getRequestedMovementId()).isNull();
        assertThat(updatedGood2.getState()).isEqualTo(RequestedGoodState.NEW);
        assertThat(updatedGood2.getComment()).isNull();

        assertThat(goodMovements1)
            .anySatisfy(
                gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.IN_PROCESS)
            )
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Товар отсутствует на складе");
                }
            );
    }

    @Test
    public void testOrderPostponedProcessed() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.POSTPONED, "Postponed",
            "AxOrderId", "FfOrderId");
        List<TransferOrderItem> ordersItems = movementControlRepositoryStub
            .getOrderItems(Collections.singletonList(orderId));
        movementControlRepositoryStub.createItemStatus(ordersItems.get(0), true);
        movementControlRepositoryStub.createItemStatus(ordersItems.get(1), true);

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        RequestedGood updatedGood1 = requestedGoodService.getById(good1.getId());
        RequestedGood updatedGood2 = requestedGoodService.getById(good2.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.POSTPONED);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getFfRequestIds)
            .isEqualTo("FfOrderId");

        assertThat(updatedGood1.getRequestedMovementId()).isEqualTo(movement1.getId());
        assertThat(updatedGood1.getState()).isEqualTo(RequestedGoodState.PLANNED);
        assertThat(updatedGood2.getState()).isEqualTo(RequestedGoodState.PLANNED);

        assertThat(goodMovements1)
            .extracting(RequestedGoodMovement::getState)
            .containsOnly(RequestedGoodMovementState.POSTPONED);

        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.PROCESSED, "Processed",
            "AxOrderId", "FfOrderId");

        // Got inbound with one good.
        updatedGood1.setState(RequestedGoodState.INCOMING);
        updatedGood1 = requestedGoodRepository.save(updatedGood1);
        RequestedGoodMovement rgm = requestedGoodRepository.getGoodMovement(movement1.getId(), updatedGood1.getId());
        requestedGoodRepository.updateGoodMovement(rgm.setState(RequestedGoodMovementState.IN_PROCESS));

        movementControlService.fetchOrdersStatuses();

        updatedMovement1 = requestedMovementService.getById(movement1.getId());

        updatedGood1 = requestedGoodService.getById(good1.getId());
        updatedGood2 = requestedGoodService.getById(good2.getId());

        goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.PROCESSED);

        assertThat(updatedGood1.getRequestedMovementId()).isEqualTo(movement1.getId());
        assertThat(updatedGood1.getState()).isEqualTo(RequestedGoodState.INCOMING);
        assertThat(updatedGood2.getRequestedMovementId()).isNull();
        assertThat(updatedGood2.getState()).isEqualTo(RequestedGoodState.NEW);
        assertThat(updatedGood2.getComment()).isNull();

        assertThat(goodMovements1)
            .anySatisfy(
                gm -> assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.IN_PROCESS)
            )
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Товар отсутствует в поставке");
                }
            );
    }

    @Test
    public void testOrderFailed() {
        RequestedMovement movement1 = createAndSaveIncomingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.FAILED, "Failed",
            "AxOrderId", "FfOrderId");
        List<TransferOrderItem> ordersItems = movementControlRepositoryStub
            .getOrderItems(Collections.singletonList(orderId));
        movementControlRepositoryStub.createItemStatus(ordersItems.get(0), true);
        movementControlRepositoryStub.createItemStatus(ordersItems.get(1), true);
        movementControlRepositoryStub.addError(ordersItems.get(1), "Some custom error");

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        RequestedGood updatedGood1 = requestedGoodService.getById(good1.getId());
        RequestedGood updatedGood2 = requestedGoodService.getById(good2.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.FAILED);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getFfRequestIds)
            .isEqualTo("FfOrderId");

        assertThat(updatedGood1.getRequestedMovementId()).isNull();
        assertThat(updatedGood1.getState()).isEqualTo(RequestedGoodState.NEW);
        assertThat(updatedGood1.getComment()).isNull();
        assertThat(updatedGood2.getRequestedMovementId()).isNull();
        assertThat(updatedGood2.getState()).isEqualTo(RequestedGoodState.NEW);
        assertThat(updatedGood2.getComment()).isNull();

        assertThat(goodMovements1)
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Failed");
                }
            )
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Some custom error");
                }
            );
    }

    @Test
    public void testOrderFailedOutgoing() {
        RequestedMovement movement1 = createAndSaveOutgoingMovement(RequestedMovementState.PLANNED);

        RequestedGood good1 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED_OUTGOING, RequestedGoodMovementState.NEW);
        RequestedGood good2 = createAndSaveGood(
            movement1, RequestedGoodState.PLANNED_OUTGOING, RequestedGoodMovementState.NEW);

        movementControlService.planRequestedMovements();

        String orderId = movement1.getId().toString();
        movementControlRepositoryStub.startProcessing(orderId);
        movementControlRepositoryStub.setStatus(orderId, TransferOrderStatus.FAILED, "Failed",
            "AxOrderId", "FfOrderId");
        List<TransferOrderItem> ordersItems = movementControlRepositoryStub
            .getOrderItems(Collections.singletonList(orderId));
        movementControlRepositoryStub.createItemStatus(ordersItems.get(0), true);
        movementControlRepositoryStub.createItemStatus(ordersItems.get(1), true);
        movementControlRepositoryStub.addError(ordersItems.get(1), "Some custom error");

        movementControlService.fetchOrdersStatuses();

        RequestedMovement updatedMovement1 = requestedMovementService.getById(movement1.getId());

        RequestedGood updatedGood1 = requestedGoodService.getById(good1.getId());
        RequestedGood updatedGood2 = requestedGoodService.getById(good2.getId());

        List<RequestedGoodMovement> goodMovements1 = requestedGoodService.getGoodMovements(
            movement1.getId(), Arrays.asList(good1, good2));

        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getState)
            .isEqualTo(RequestedMovementState.FAILED);
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getErpRequestIds)
            .isEqualTo("AxOrderId");
        assertThat(updatedMovement1)
            .extracting(RequestedMovement::getFfRequestIds)
            .isEqualTo("FfOrderId");

        assertThat(updatedGood1.getRequestedMovementId()).isNull();
        assertThat(updatedGood1.getState()).isEqualTo(RequestedGoodState.PROCESSED);
        assertThat(updatedGood1.getComment()).isNull();
        assertThat(updatedGood2.getRequestedMovementId()).isNull();
        assertThat(updatedGood2.getState()).isEqualTo(RequestedGoodState.PROCESSED);
        assertThat(updatedGood2.getComment()).isNull();

        assertThat(goodMovements1)
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Failed");
                }
            )
            .anySatisfy(
                gm -> {
                    assertThat(gm.getState()).isEqualTo(RequestedGoodMovementState.REJECTED);
                    assertThat(gm.getStateMessage()).isEqualTo("Some custom error");
                }
            );
    }

    private RequestedMovement createAndSaveIncomingMovement(RequestedMovementState state) {
        return createAndSaveMovement(state, MovementDirection.INCOMING);
    }

    private RequestedMovement createAndSaveOutgoingMovement(RequestedMovementState state) {
        return createAndSaveMovement(state, MovementDirection.OUTGOING);
    }

    private RequestedMovement createAndSaveMovement(RequestedMovementState state,
                                                            MovementDirection direction) {
        RequestedMovement movement = RandomTestUtils
            .randomObject(RequestedMovement.class, "id", "modifiedDate");
        movement.setState(state);
        movement.setDirection(direction);
        return requestedMovementService.saveMovement(movement);
    }

    private RequestedGood createAndSaveGood(RequestedMovement movement,
                                           RequestedGoodState goodState,
                                           RequestedGoodMovementState goodMovementState) {
        RequestedGood requestedGood = RandomTestUtils
            .randomObject(RequestedGood.class, "id", "modifiedDate")
            .setRequestedMovementId(movement.getId())
            .setState(goodState);
        requestedGood = requestedGoodService.save(requestedGood);

        RequestedGoodMovement requestedGoodMovement = new RequestedGoodMovement()
            .setRequestedGoodId(requestedGood.getId())
            .setRequestedMovementId(movement.getId())
            .setState(goodMovementState);
        requestedGoodRepository.createGoodMovement(requestedGoodMovement);

        return requestedGood;
    }

}
