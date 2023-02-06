package ru.yandex.market.antifraud.orders.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.apache.zookeeper.KeeperException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import ru.yandex.market.antifraud.orders.detector.LegacyOrderFraudDetector;
import ru.yandex.market.antifraud.orders.detector.OrderFraudDetector;
import ru.yandex.market.antifraud.orders.entity.AntifraudCheckResult;
import ru.yandex.market.antifraud.orders.entity.OrderVerdict;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.processors.BundleEqualizer;
import ru.yandex.market.antifraud.orders.service.processors.CountMerger;
import ru.yandex.market.antifraud.orders.service.processors.OrderCheckPostprocessor;
import ru.yandex.market.antifraud.orders.service.processors.OrderCheckPreprocessor;
import ru.yandex.market.antifraud.orders.service.processors.SurrogateItemIdCleaner;
import ru.yandex.market.antifraud.orders.service.processors.SurrogateItemIdGenerator;
import ru.yandex.market.antifraud.orders.service.processors.ZeroCountItemsRemover;
import ru.yandex.market.antifraud.orders.storage.dao.AntifraudDao;
import ru.yandex.market.antifraud.orders.storage.entity.roles.BuyerRole;
import ru.yandex.market.antifraud.orders.storage.entity.rules.BaseDetectorConfiguration;
import ru.yandex.market.antifraud.orders.storage.entity.rules.DetectorConfiguration;
import ru.yandex.market.antifraud.orders.test.providers.OrderBuyerRequestProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderRequestProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderResponseDtoProvider;
import ru.yandex.market.antifraud.orders.util.concurrent.FutureValueHolder;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderItemResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.OrderItemChange;
import ru.yandex.market.common.zk.ZooClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.CANCEL_ORDER;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.CANCEL_PROMO_CODE;
import static ru.yandex.market.antifraud.orders.entity.AntifraudAction.ORDER_ITEM_CHANGE;
import static ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils.roleServiceSpy;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 23.05.19
 */
@RunWith(MockitoJUnitRunner.class)
public class AntifraudServiceTest {
    private static final long TEST_UID_1 = 123456789L;
    private static final long TEST_UID_2 = 987654321L;
    private static final long AUTHOR_UID = 555555555L;

    @Mock
    private AntifraudLogFormatter antifraudLogFormatter;
    @Mock
    private ZooClient zooClient;
    @Mock
    private AntifraudDao antifraudDao;
    @Mock
    private LegacyOrderFraudDetector detector1;
    @Mock
    private AbstractOrderFraudDetector<BaseDetectorConfiguration> detector2;
    @Mock
    private BuyerDataService buyerDataService;
    @Mock
    private OrderDataContainerFactory orderContextFactory;
    private RoleService roleService;
    private List<OrderFraudDetector> detectors;
    private List<OrderCheckPreprocessor> preprocessors = List.of(new ZeroCountItemsRemover(), new SurrogateItemIdGenerator());
    private List<OrderCheckPostprocessor> postprocessors = List.of(new CountMerger(), new BundleEqualizer(), new SurrogateItemIdCleaner());
    private AntifraudService antifraudService;

    @Before
    public void setUp() throws KeeperException {
        detectors = asList(detector1, detector2);
        roleService = roleServiceSpy();
        antifraudService = new AntifraudService(detectors,
            antifraudLogFormatter,
            roleService, buyerDataService, orderContextFactory, preprocessors, postprocessors);
        when(orderContextFactory.prepareContext(any())).then(new Answer<OrderDataContainer>() {
            @Override
            public OrderDataContainer answer(InvocationOnMock invocation) throws Throwable {
                MultiCartRequestDto orderRequest = invocation.getArgument(0);
                return OrderDataContainer.builder()
                        .orderRequest(orderRequest)
                        .gluedIdsFuture(new FutureValueHolder<>(Set.of()))
                        .lastOrdersFuture(new FutureValueHolder<>(List.of()))
                        .build();
            }
        });
        doReturn(Optional.of(BuyerRole.builder()
            .name("white-list")
            .detectorConfigurations(
                ImmutableMap.of(
                    "test_detector1", new BaseDetectorConfiguration(false),
                    "test_detector2", new BaseDetectorConfiguration(false)
                )
            )
            .build())
        ).when(roleService).getRoleByUid(eq(String.valueOf(TEST_UID_2)));
        when(detector1.getUniqName())
            .thenReturn("test_detector1");
        when(detector1.detectFraud(any(), any(BuyerContext.class))).thenCallRealMethod();
        when(detector2.detectFraud(any(OrderDataContainer.class), any(DetectorConfiguration.class)))
            .thenReturn(OrderDetectorResult.empty("test_detector2"));
        when(detector2.detectFraud(any(), any(BuyerContext.class))).thenCallRealMethod();
        when(detector2.getUniqName())
            .thenReturn("test_detector2");
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(antifraudLogFormatter, detector1, detector2);
    }

    @Test
    public void orderOkTest() throws KeeperException {
        var order = OrderRequestProvider.getOrderRequest();

        when(detector1.detectFraud(any()))
            .thenReturn(OrderDetectorResult.empty("test_detector1"));

        final OrderVerdict expected = OrderVerdict.builder().checkResults(emptySet()).isDegradation(false).build();
        final OrderVerdict actual = antifraudService.checkOrder(order);

        assertEquals(expected, actual);

        verify(detector1).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector1).detectFraud(any());
        verify(detector1).getUniqName();
        verify(detector2).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector2).detectFraud(any(), any(DetectorConfiguration.class));
        verify(detector2).getUniqName();
    }

    @Test
    public void orderWhitelisted() throws KeeperException {
        final var order = OrderRequestProvider.getPreparedOrderRequestBuilder()
            .buyer(OrderBuyerRequestProvider.getDefaultBuyerWithCustomId(TEST_UID_2))
            .build();

        final OrderVerdict expected = OrderVerdict.builder().checkResults(emptySet()).isDegradation(false).build();
        final OrderVerdict actual = antifraudService.checkOrder(order);

        assertEquals(expected, actual);

        verify(roleService).getRoleByUid(eq(String.valueOf(TEST_UID_2)));
        verify(detector1).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector1).getUniqName();
        verify(detector2).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector2).getUniqName();
    }

    @Test
    public void cancelOrder() throws KeeperException {
        final var order = OrderRequestProvider.getPreparedOrderRequestBuilder()
                .buyer(OrderBuyerRequestProvider.getDefaultBuyerWithCustomId(TEST_UID_1))
                .build();
        final OrderVerdict expected = OrderVerdict.builder()
                .checkResults(singleton(new AntifraudCheckResult(CANCEL_ORDER, "", "")))
                .isDegradation(false)
                .build();
        when(detector1.detectFraud(any())).thenReturn(
            OrderDetectorResult.builder()
                .ruleName("test_detector1")
                .answerText("")
                .reason("")
                .actions(Set.of(CANCEL_ORDER))
                .build()
        );

        final OrderVerdict actual = antifraudService.checkOrder(order);

        assertEquals(expected, actual);

        verify(antifraudLogFormatter).format(order, "test_detector1", singleton(CANCEL_ORDER), "");
        verify(detector1).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector1).detectFraud(any());
        verify(detector1, times(2)).getUniqName();
        verify(roleService).getRoleByUid(eq(String.valueOf(TEST_UID_1)));
    }

    @Test
    public void fixOrder() {
        var order = OrderRequestProvider.getOrderRequest();

        OrderResponseDto fixedOrder = new OrderResponseDto(
                List.of(OrderItemResponseDto.builder().changes(singleton(OrderItemChange.FRAUD_FIXED)).build()));
        OrderVerdict expected = OrderVerdict.builder()
                .checkResults(singleton(new AntifraudCheckResult(ORDER_ITEM_CHANGE, "", "")))
                .fixedOrder(fixedOrder)
                .isDegradation(false)
                .build();
        when(detector1.detectFraud(any())).thenReturn(
            OrderDetectorResult.builder()
                .ruleName("test_detector1")
                .answerText("")
                .reason("")
                .fixedOrder(fixedOrder)
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .build()
        );

        final OrderVerdict actual = antifraudService.checkOrder(order);

        assertEquals(expected, actual);

        verify(antifraudLogFormatter).format(order, "test_detector1", singleton(ORDER_ITEM_CHANGE), "");
        verify(detector1).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector1).detectFraud(any());
        verify(detector1, times(2)).getUniqName();
        verify(detector2).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector2).detectFraud(any(), any(DetectorConfiguration.class));
        verify(detector2).getUniqName();
    }

    @Test
    public void mergeVerdicts() throws KeeperException {
        final var order = OrderRequestProvider.getOrderRequest();

        OrderResponseDto fixedOrder = new OrderResponseDto(
                List.of(OrderItemResponseDto.builder().changes(singleton(OrderItemChange.FRAUD_FIXED)).build()));
        final AntifraudCheckResult detector1Check = new AntifraudCheckResult(ORDER_ITEM_CHANGE, "fix", "limit");
        when(detector1.detectFraud(any())).thenReturn(
            OrderDetectorResult.builder()
                .ruleName("test_detector1")
                .answerText("fix")
                .reason("limit")
                .fixedOrder(fixedOrder)
                .actions(Set.of(ORDER_ITEM_CHANGE))
                .build()
        );

        final AntifraudCheckResult detector2Check1 = new AntifraudCheckResult(CANCEL_PROMO_CODE, "no promo", "already" +
                " used");
        when(detector2.detectFraud(any(), any(DetectorConfiguration.class)))
                .thenReturn(OrderDetectorResult.builder()
                        .ruleName("test_detector2")
                        .answerText("no promo")
                        .reason("already used")
                        .fixedOrder(OrderResponseDtoProvider.getEmptyOrderResponse())
                        .actions(Set.of(CANCEL_PROMO_CODE))
                        .build());

        final OrderVerdict expected = OrderVerdict.builder()
            .checkResults(new HashSet<>(asList(detector1Check, detector2Check1)))
            .fixedOrder(fixedOrder)
            .isDegradation(false)
            .build();
        final OrderVerdict actual = antifraudService.checkOrder(order);

        assertEquals(expected, actual);

        verify(antifraudLogFormatter).format(order, "test_detector1", singleton(ORDER_ITEM_CHANGE), "limit");
        verify(antifraudLogFormatter).format(order, "test_detector2", new HashSet<>(asList(CANCEL_PROMO_CODE)), "already used");
        verify(detector1).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector1).detectFraud(any());
        verify(detector1, times(2)).getUniqName();
        verify(detector2).detectFraud(any(OrderDataContainer.class), any(BuyerContext.class));
        verify(detector2).detectFraud(any(), any(DetectorConfiguration.class));
        verify(detector2, times(2)).getUniqName();
    }
}
