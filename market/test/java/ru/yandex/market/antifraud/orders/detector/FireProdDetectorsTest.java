package ru.yandex.market.antifraud.orders.detector;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyAntifraudContext;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyBonusContext;
import ru.yandex.market.antifraud.orders.model.OrderDataContainer;
import ru.yandex.market.antifraud.orders.model.OrderDetectorResult;
import ru.yandex.market.antifraud.orders.service.BuyerContext;
import ru.yandex.market.antifraud.orders.service.FireProdBeanPostProcessor;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyBonusDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.MultiCartRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.BonusState;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoRequestDto;
import ru.yandex.market.sdk.userinfo.service.ResolveUidService;
import ru.yandex.market.sdk.userinfo.service.ResolveUidServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static ru.yandex.market.antifraud.orders.service.loyalty.LoyaltyDetectorResult.BLACKLIST_RESULT;

public class FireProdDetectorsTest {

    public static final long FIRE_PROD_UID = 2_190_550_858_753_437_196L;
    public static final long USUAL_UID = 123;

    private FireProdBeanPostProcessor fireProdBeanPostProcessor;

    @Before
    public void setUp() {
        ResolveUidService resolveUidService = new ResolveUidServiceImpl();
        fireProdBeanPostProcessor = new FireProdBeanPostProcessor(resolveUidService);
    }

    @Test
    public void fireProdOrderFraudDetectorOk() {

        OrderFraudDetector detector = mock(OrderFraudDetector.class);
        OrderDetectorResult detectorResult = OrderDetectorResult.failed("rule", "error");
        when(detector.detectFraud(any(), any())).thenReturn(Optional.of(detectorResult));

        OrderFraudDetector wrapper = callBeanPostProcessor(detector);
        OrderDetectorResult result = wrapper.detectFraud(mockOrderDataContainer(FIRE_PROD_UID), mockBuyerContext()).get();

        verify(detector).detectFraud(any(), any());
        Assert.assertEquals(OrderDetectorResult.empty(detector.getUniqName()), result);
    }


    @Test
    public void usualOrderFraudDetectorOk() {

        OrderFraudDetector detector = mock(OrderFraudDetector.class);
        OrderDetectorResult detectorResult = OrderDetectorResult.failed("rule", "error");
        when(detector.detectFraud(any(), any())).thenReturn(Optional.of(detectorResult));

        OrderFraudDetector wrapper = callBeanPostProcessor(detector);
        OrderDetectorResult result = wrapper.detectFraud(mockOrderDataContainer(USUAL_UID), mockBuyerContext()).get();

        verify(detector).detectFraud(any(), any());
        Assert.assertSame(detectorResult, result);
    }

    @Test
    public void fireProdLoyaltyDetectorOk() {

        LoyaltyDetector detector = mock(LoyaltyDetector.class);
        when(detector.check(any(), any())).thenReturn(BLACKLIST_RESULT);

        LoyaltyDetector wrapper = callBeanPostProcessor(detector);
        LoyaltyDetectorResult result = wrapper.check(mockLoyaltyAntifraudContext(FIRE_PROD_UID));

        verify(detector).check(any(), any());
        Assert.assertEquals(LoyaltyDetectorResult.OK_RESULT, result);
    }

    @Test
    public void usualLoyaltyDetectorOk() {

        LoyaltyDetector detector = mock(LoyaltyDetector.class);
        when(detector.check(any(), any())).thenReturn(BLACKLIST_RESULT);

        LoyaltyDetector wrapper = callBeanPostProcessor(detector);
        LoyaltyDetectorResult result = wrapper.check(mockLoyaltyAntifraudContext(USUAL_UID));

        verify(detector).check(any(), any());
        Assert.assertEquals(BLACKLIST_RESULT, result);
    }

    @Test
    public void fireProdLoyaltyBonusDetectorOk() {

        LoyaltyBonusDetector detector = mock(LoyaltyBonusDetector.class);
        when(detector.checkBonuses(any())).thenReturn(BonusState.DISABLED);

        LoyaltyBonusDetector wrapper = callBeanPostProcessor(detector);
        BonusState result = wrapper.checkBonuses(mockLoyaltyBonusContext(FIRE_PROD_UID));

        verify(detector).checkBonuses(any());
        Assert.assertEquals(BonusState.ENABLED, result);
    }

    @Test
    public void usualLoyaltyBonusDetectorOk() {

        LoyaltyBonusDetector detector = mock(LoyaltyBonusDetector.class);
        when(detector.checkBonuses(any())).thenReturn(BonusState.DISABLED);

        LoyaltyBonusDetector wrapper = callBeanPostProcessor(detector);
        BonusState result = wrapper.checkBonuses(mockLoyaltyBonusContext(USUAL_UID));

        verify(detector).checkBonuses(any());
        Assert.assertSame(BonusState.DISABLED, result);
    }

    private <T> T callBeanPostProcessor(T detector) {
        return (T) fireProdBeanPostProcessor.postProcessAfterInitialization(detector, "");
    }

    private static OrderDataContainer mockOrderDataContainer(long uid) {
        OrderBuyerRequestDto buyer = mock(OrderBuyerRequestDto.class);
        when(buyer.getUid()).thenReturn(uid);

        MultiCartRequestDto requestDto = mock(MultiCartRequestDto.class);
        when(requestDto.getBuyer()).thenReturn(buyer);

        OrderDataContainer result = mock(OrderDataContainer.class);
        when(result.getOrderRequest()).thenReturn(requestDto);

        return result;
    }

    private static BuyerContext mockBuyerContext() {
        return mock(BuyerContext.class);
    }

    private static LoyaltyAntifraudContext mockLoyaltyAntifraudContext(long uid) {
        LoyaltyAntifraudContext result = mock(LoyaltyAntifraudContext.class);
        when(result.getUid()).thenReturn(uid);
        return result;
    }

    private static LoyaltyBonusContext mockLoyaltyBonusContext(long uid) {
        LoyaltyBonusInfoRequestDto requestDto = mock(LoyaltyBonusInfoRequestDto.class);
        when(requestDto.getUid()).thenReturn(uid);
        LoyaltyBonusContext result = mock(LoyaltyBonusContext.class);
        when(result.getRequest()).thenReturn(requestDto);
        return result;
    }
}
