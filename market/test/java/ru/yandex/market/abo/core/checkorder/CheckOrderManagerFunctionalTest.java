package ru.yandex.market.abo.core.checkorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.OptimisticLockException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioErrorType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioStatus;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderScenarioType;
import ru.yandex.market.abo.api.entity.checkorder.CheckOrderStatus;
import ru.yandex.market.abo.api.entity.checkorder.OrderProcessMethod;
import ru.yandex.market.abo.api.entity.checkorder.PlacementType;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorDetail;
import ru.yandex.market.abo.api.entity.checkorder.ScenarioErrorParam;
import ru.yandex.market.abo.core.checkorder.model.CheckOrder;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderAttempt;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderScenario;
import ru.yandex.market.abo.core.checkorder.model.CheckOrderTypeControl;
import ru.yandex.market.abo.core.checkorder.model.ScenarioPayload;
import ru.yandex.market.abo.core.checkorder.repo.CheckOrderAttemptRepo;
import ru.yandex.market.abo.core.checkorder.repo.CheckOrderRepo;
import ru.yandex.market.abo.core.checkorder.scenario.CheckOrderScenarioManager;
import ru.yandex.market.abo.core.cutoff.feature.FeatureStatusManager;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author komarovns
 * @date 14.06.19
 */
class CheckOrderManagerFunctionalTest extends EmptyTest {
    private static final long SHOP_ID = 774;
    private static final OrderProcessMethod PROCESS_METHOD = OrderProcessMethod.API;

    @Autowired
    CheckOrderRepo checkOrderRepo;
    @Autowired
    TransactionTemplate transactionTemplate;
    @Autowired
    FeatureStatusManager featureStatusManager;
    @Autowired
    ExceptionalShopsService exceptionalShopsService;
    @Autowired
    CheckOrderManager checkOrderManager;
    @Autowired
    CheckOrderDbService checkOrderDbService;
    @Autowired
    @InjectMocks
    CheckOrderScenarioManager checkOrderScenarioManager;
    @Autowired
    CheckOrderAttemptService attemptService;
    @Autowired
    CheckOrderTypeControlService typeControlService;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    @Qualifier("jpaPgTransactionManager")
    PlatformTransactionManager transactionManager;
    @Autowired
    CheckOrderAttemptRepo attemptRepo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createCheckOrder() {
        checkOrderManager.createCheckOrder(SHOP_ID);
        var checkOrders = checkOrderRepo.findAll();
        assertEquals(1, checkOrders.size());
        var checkOrder = checkOrders.get(0);
        assertEquals(CheckOrderStatus.IN_PROGRESS, checkOrder.getStatus());
        assertFalse(checkOrder.getSelfCheck());
        assertFalse(checkOrder.getScenarios().isEmpty());
        checkOrder.getScenarios().forEach(s -> assertEquals(CheckOrderScenarioStatus.NEW, s.getStatus()));
        assertEquals(0, checkOrderManager.getAvailableCheckOrderAttempts(SHOP_ID));
    }

    @Test
    void cancelCheckOrder() {
        var checkOrder = checkOrderManager.createCheckOrder(SHOP_ID);
        checkOrderManager.cancel(checkOrder);

        var checkOrders = checkOrderRepo.findAll();
        assertEquals(1, checkOrders.size());
        checkOrder = checkOrders.get(0);
        assertEquals(CheckOrderStatus.CANCELLED, checkOrder.getStatus());
        checkOrder.getScenarios().forEach(s -> assertEquals(CheckOrderScenarioStatus.CANCELLED, s.getStatus()));
    }

    @ParameterizedTest
    @EnumSource(value = OrderProcessMethod.class)
    @NullSource
    void findStartedAndAvailableSelfChecks(OrderProcessMethod processMethod) {
        PlacementType placementType = PlacementType.DSBB;
        List<CheckOrderScenarioType> scenarioTypes = CheckOrderScenarioType.selfChecks(placementType).stream()
                .filter(type -> CheckOrderScenarioType.REJECTED_BY_PARTNER != type || OrderProcessMethod.PI == processMethod)
                .collect(Collectors.toList());

        List<CheckOrder> availableSelfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(
                SHOP_ID, placementType, processMethod);
        assertEquals(scenarioTypes.size(), availableSelfChecks.size());
        assertTrue(availableSelfChecks.stream()
                .allMatch(check -> {
                    CheckOrderScenario scenario = check.getSingleScenario();
                    return scenario.getStatus() == CheckOrderScenarioStatus.AVAILABLE &&
                            scenario.getType().getPlacementType() == placementType;
                }));

        CheckOrderScenarioType scenarioType = scenarioTypes.get(0);
        CheckOrder createdSelfCheck = checkOrderManager.createSelfCheck(SHOP_ID, scenarioType, processMethod);

        availableSelfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, processMethod);
        assertEquals(scenarioTypes.size(), availableSelfChecks.size());
        assertTrue(availableSelfChecks.remove(createdSelfCheck));
        assertTrue(availableSelfChecks.stream()
                .allMatch(check -> check.getSingleScenario().getStatus() == CheckOrderScenarioStatus.AVAILABLE));
        assertEquals(CheckOrderScenarioStatus.NEW, createdSelfCheck.getSingleScenario().getStatus());
        checkOrderManager.stop(SHOP_ID, createdSelfCheck.getSingleScenario().getId());

        CheckOrder laterCheck = checkOrderManager.createSelfCheck(SHOP_ID, scenarioType, processMethod);
        assertNotEquals(createdSelfCheck, laterCheck);
        availableSelfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, processMethod);
        assertEquals(scenarioTypes.size(), availableSelfChecks.size());
        assertFalse(availableSelfChecks.contains(createdSelfCheck));
        assertTrue(availableSelfChecks.remove(laterCheck));
        assertTrue(availableSelfChecks.stream()
                .allMatch(check -> check.getSingleScenario().getStatus() == CheckOrderScenarioStatus.AVAILABLE));
        assertEquals(CheckOrderScenarioStatus.NEW, laterCheck.getSingleScenario().getStatus());
    }

    @Test
    void typeControlTest() {
        CheckOrderScenarioType controlledType = CheckOrderScenarioType.SUCCESSFUL_ORDER;
        PlacementType placementType = controlledType.getPlacementType();

        List<CheckOrder> selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertTrue(containsType(selfChecks, controlledType));

        checkOrderManager.createSelfCheck(SHOP_ID, controlledType, PROCESS_METHOD);
        selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertTrue(containsType(selfChecks, controlledType));

        typeControlService.save(List.of(control(controlledType, false)));
        selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertFalse(containsType(selfChecks, controlledType));

        typeControlService.save(List.of(control(controlledType, true)));
        selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertTrue(containsType(selfChecks, controlledType));
    }

    @Test
    void alwaysAllChecks() {
        CheckOrderScenarioType controlledType = CheckOrderScenarioType.SUCCESSFUL_ORDER;
        PlacementType placementType = controlledType.getPlacementType();

        typeControlService.save(List.of(control(controlledType, false)));
        List<CheckOrder> selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertFalse(containsType(selfChecks, controlledType));

        exceptionalShopsService.addException(SHOP_ID, ExceptionalShopReason.ALWAYS_ALL_SELF_CHECKS, 0, "");
        selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertTrue(containsType(selfChecks, controlledType));

        exceptionalShopsService.deleteException(SHOP_ID, ExceptionalShopReason.ALWAYS_ALL_SELF_CHECKS);
        selfChecks = checkOrderManager.findStartedAndAvailableSelfChecks(SHOP_ID, placementType, PROCESS_METHOD);
        assertFalse(containsType(selfChecks, controlledType));
    }

    private static CheckOrderTypeControl control(CheckOrderScenarioType type, boolean active) {
        CheckOrderTypeControl typeControl = new CheckOrderTypeControl();
        typeControl.setScenarioType(type);
        typeControl.setActive(active);
        return typeControl;
    }

    private static boolean containsType(List<CheckOrder> selfChecks, CheckOrderScenarioType type) {
        return selfChecks.stream()
                .map(CheckOrder::getSingleScenario)
                .map(CheckOrderScenario::getType)
                .anyMatch(chType -> chType == type);
    }

    @Test
    void testErrorParams() {
        ScenarioErrorDetail[] errorDetails = new ScenarioErrorDetail[]{
                new ScenarioErrorDetail(ScenarioErrorParam.EXPECTED_ORDER_SUBSTATUS, OrderSubstatus.SHIPPED),
                new ScenarioErrorDetail(ScenarioErrorParam.ACTUAL_ORDER_SUBSTATUS, OrderSubstatus.USER_CHANGED_MIND)
        };
        CheckOrderScenarioStatus failStatus = CheckOrderScenarioStatus.FAIL;
        CheckOrderScenarioErrorType errorType = CheckOrderScenarioErrorType.FAIL_BY_TIMEOUT;

        CheckOrder selfCheck = checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.SUCCESSFUL_ORDER, null);
        CheckOrderScenario checkScenario = selfCheck.getSingleScenario();
        assertEquals(CheckOrderScenarioStatus.NEW, checkScenario.getStatus());

        checkScenario.setStatus(failStatus);
        checkScenario.setErrorType(errorType);
        checkScenario.setErrorDetails(errorDetails);
        checkOrderDbService.save(selfCheck);

        checkScenario = checkOrderRepo.getOne(selfCheck.getId()).getSingleScenario();
        assertEquals(failStatus, checkScenario.getStatus());
        assertEquals(errorType, checkScenario.getErrorType());
        assertArrayEquals(errorDetails, checkScenario.getErrorDetails());
    }

    @Test
    void runTwiceValidation() {
        CheckOrder created = checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.SUCCESSFUL_ORDER, PROCESS_METHOD);
        assertEquals(CheckOrderStatus.IN_PROGRESS, created.getStatus());
        assertThrows(IllegalArgumentException.class, () ->
                checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.SUCCESSFUL_ORDER, PROCESS_METHOD));
        CheckOrder otherCreated = checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.LARGE_ORDER, PROCESS_METHOD);
        assertEquals(CheckOrderStatus.IN_PROGRESS, otherCreated.getStatus());
    }

    @Test
    void stopValidation() {
        assertThrows(IllegalArgumentException.class, () -> checkOrderManager.stop(SHOP_ID, 1111));
        CheckOrder created = checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.LARGE_ORDER, PROCESS_METHOD);
        CheckOrder stopped = checkOrderManager.stop(SHOP_ID, created.getSingleScenario().getId());
        assertEquals(CheckOrderStatus.CANCELLED, stopped.getStatus());
        assertEquals(CheckOrderScenarioStatus.CANCELLED, stopped.getSingleScenario().getStatus());
    }

    @Test
    void noAttempts() {
        checkOrderManager.createCheckOrder(SHOP_ID);
        assertThrows(IllegalArgumentException.class, () -> checkOrderManager.createCheckOrder(SHOP_ID));
        assertEquals(0, checkOrderManager.getAvailableCheckOrderAttempts(SHOP_ID));
    }

    @Test
    void alreadyRunningCheckOrder() {
        attemptService.save(new CheckOrderAttempt(SHOP_ID, 10));
        checkOrderManager.createCheckOrder(SHOP_ID);
        assertThrows(IllegalArgumentException.class, () -> checkOrderManager.createCheckOrder(SHOP_ID));
    }

    @Test
    void findLast() {
        attemptService.save(new CheckOrderAttempt(SHOP_ID, 2));

        CheckOrder first = checkOrderManager.createCheckOrder(SHOP_ID);
        assertEquals(first, checkOrderManager.findLastCheckOrder(SHOP_ID).orElseThrow());

        checkOrderManager.cancel(first);
        CheckOrder second = checkOrderManager.createCheckOrder(SHOP_ID);
        assertNotEquals(first, second);
        assertEquals(second, checkOrderManager.findLastCheckOrder(SHOP_ID).orElseThrow());
    }

    @Test
    void forceSuccess() {
        CheckOrder created = checkOrderManager.createCheckOrder(SHOP_ID);
        assertEquals(CheckOrderStatus.IN_PROGRESS, created.getStatus());
        assertEquals(1, created.getScenarios().size());
        assertEquals(CheckOrderScenarioStatus.NEW, created.getSingleScenario().getStatus());

        long forcedUserId = 1312L;
        checkOrderManager.forceSuccess(created.getId(), forcedUserId);

        CheckOrder successful = checkOrderRepo.findByIdOrNull(created.getId());
        assertEquals(CheckOrderStatus.SUCCESS, successful.getStatus());
        assertEquals(forcedUserId, successful.getForcedUserId());
        assertEquals(1, successful.getScenarios().size());
        assertEquals(CheckOrderScenarioStatus.SUCCESS, successful.getSingleScenario().getStatus());
    }

    @Test
    void testPayload() {
        CheckOrderScenario scenario = new CheckOrderScenario(CheckOrderScenarioType.OFFLINE_ORDER, PROCESS_METHOD);
        scenario.setPayload(new ScenarioPayload().setShipmentApplicationId(RND.nextLong()));
        checkOrderDbService.save(new CheckOrder(SHOP_ID, false, List.of(scenario)));
        flushAndClear();

        List<CheckOrder> checkOrders = checkOrderDbService.findOfflineChecks(SHOP_ID);
        assertEquals(1, checkOrders.size());
        assertEquals(scenario, checkOrders.get(0).getSingleScenario());
    }

    @ParameterizedTest
    @EnumSource(value = OrderProcessMethod.class)
    @NullSource
    void findLastSelfChecks(OrderProcessMethod processMethod) {
        List<CheckOrder> lastCreated = new ArrayList<>();
        CheckOrderScenarioType.selfChecks(PlacementType.DSBB).forEach(scType -> {
            CheckOrder selfCheck = checkOrderManager.createSelfCheck(SHOP_ID, scType, processMethod);
            checkOrderManager.cancel(selfCheck);

            Arrays.stream(OrderProcessMethod.values()).filter(val -> val != processMethod).findFirst()
                    .ifPresent(otherMethod -> {
                        CheckOrder otherMethodCheck = checkOrderManager.createSelfCheck(SHOP_ID, scType, otherMethod);
                        checkOrderManager.cancel(otherMethodCheck);
                    });

            lastCreated.add(checkOrderManager.createSelfCheck(SHOP_ID, scType, processMethod));
        });

        List<CheckOrder> lastSelfChecksFromDb = checkOrderDbService.findLastSelfChecks(SHOP_ID, processMethod);
        assertEquals(new HashSet<>(lastCreated), new HashSet<>(lastSelfChecksFromDb));
    }

    @Test
    void concurrentCancel() {
        var otherTransaction = new TransactionTemplate(
                transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW)
        );

        // PUBLIC - CREATE
        CheckOrder created = checkOrderManager.createSelfCheck(SHOP_ID, CheckOrderScenarioType.SUCCESSFUL_ORDER, null);
        assertEquals(CheckOrderScenarioStatus.NEW, created.getSingleScenario().getStatus());
        assertNotNull(created.getSingleScenario().getModificationTime());
        flushAndCommit();

        //TMS - PROCESS
        CheckOrder tmsCheckOrder = checkOrderDbService.findChecksInProgress().get(0);
        assertEquals(created, tmsCheckOrder);

        //PUBLIC - CANCEL
        otherTransaction.executeWithoutResult(__ -> {
            checkOrderManager.stop(SHOP_ID, created.getSingleScenario().getId());
            flushAndCommit();
        });

        // TMS - FINISH PROCESS
        assertThrows(OptimisticLockException.class, () -> {
            tmsCheckOrder.getSingleScenario().setStatus(CheckOrderScenarioStatus.IN_PROGRESS);
            checkOrderDbService.save(tmsCheckOrder);
            flushAndCommit();
        });

        flushAndCommit();
        CheckOrder cancelled = checkOrderDbService.findCheckOrder(created.getId());
        assertEquals(CheckOrderStatus.CANCELLED, cancelled.getStatus());
        assertEquals(CheckOrderScenarioStatus.CANCELLED, cancelled.getSingleScenario().getStatus());
    }

    @AfterEach
    void tearDown() {
        entityManager.clear();
        Stream.of(attemptRepo, checkOrderRepo).forEach(CrudRepository::deleteAll);
        typeControlService.selfCheckControls().forEach(control -> {
            control.setActive(true);
            typeControlService.save(List.of(control));
        });
        flushAndCommit();
    }

    private void flushAndCommit() {
        flushAndClear();
        jdbcTemplate.update("COMMIT");
    }
}
