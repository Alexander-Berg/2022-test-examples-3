package ru.yandex.market.antifraud.orders.service.loyalty;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.antifraud.orders.entity.MarketUserId;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyCoin;
import ru.yandex.market.antifraud.orders.entity.loyalty.LoyaltyPromo;
import ru.yandex.market.antifraud.orders.service.BuyerDataService;
import ru.yandex.market.antifraud.orders.service.ConfigurationService;
import ru.yandex.market.antifraud.orders.service.GluesService;
import ru.yandex.market.antifraud.orders.service.LoyaltyDataService;
import ru.yandex.market.antifraud.orders.service.OrdersService;
import ru.yandex.market.antifraud.orders.service.RoleService;
import ru.yandex.market.antifraud.orders.service.WalletTransactionsService;
import ru.yandex.market.antifraud.orders.service.exceptions.TooManyRequestsException;
import ru.yandex.market.antifraud.orders.service.loyalty.detectors.PromoRestrictedDetector;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountPromoFilter;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCountRequest;
import ru.yandex.market.antifraud.orders.service.loyalty.orderCount.OrderCounter;
import ru.yandex.market.antifraud.orders.storage.dao.MarketUserIdDao;
import ru.yandex.market.antifraud.orders.web.dto.CoinDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDto;
import ru.yandex.market.antifraud.orders.web.dto.PromoVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.checkouter.OrderBuyerRequestDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyRestrictionType;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;
import ru.yandex.market.crm.platform.commons.RGBType;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.models.Order;
import ru.yandex.market.crm.platform.models.OrderProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.antifraud.orders.test.utils.AntifraudTestUtils.roleServiceSpy;

/**
 * @author dzvyagin
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class LoyaltyAntifraudServiceTest {

    @Mock
    private LoyaltyDataService loyaltyDataService;
    @Mock
    private GluesService gluesService;
    @Mock
    private OrdersService ordersService;
    @Mock
    private WalletTransactionsService walletTransactionsService;
    @Mock
    private ConfigurationService configurationService;
    @Mock
    private MarketUserIdDao marketUserIdDao;

    private RoleService roleService;

    private LoyaltyAntifraudLogFormatter logFormatter = new LoyaltyAntifraudLogFormatter();

    @Before
    public void init() {
        roleService = roleServiceSpy();
        when(ordersService.getOrders(any(MarketUserId.class), any())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
    }

    @Test
    public void blackRuleOnly() {
        LoyaltyDetector blackRule = rule(LoyaltyDetectorResult.BLACKLIST_RESULT);
        LoyaltyDetector greyRule = mock(LoyaltyDetector.class);
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Arrays.asList(blackRule, greyRule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService,
                logFormatter, marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyVerdictRequestDto requestDto = request(USER_UID);
        initMocks(USER_UID);
        LoyaltyVerdictDto verdictDto = antifraudService.checkPromoRequest(requestDto);
        assertThat(verdictDto.getVerdict()).isEqualTo(LoyaltyVerdictType.BLACKLIST);
        verify(greyRule, never()).check(any(), any());
    }

    @Test
    public void shouldPrepareAnswer() {
        LoyaltyDetector greyRule = rule(new LoyaltyDetectorResult(
            "rule",
            LoyaltyVerdictType.OTHER,
            Collections.singletonList(new PromoVerdictDto(2001L, 1201L, PromoVerdictType.USED))
        ));
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.singletonList(greyRule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService,
                logFormatter, marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyVerdictRequestDto requestDto = request(USER_UID);
        initMocks(USER_UID);
        LoyaltyVerdictDto verdictDto = antifraudService.checkPromoRequest(requestDto);
        assertThat(verdictDto.getVerdict()).isEqualTo(LoyaltyVerdictType.OTHER);
        List<PromoVerdictDto> verdicts = verdictDto.getPromos();
        assertThat(verdicts.stream().filter(v -> v.getVerdict().equals(PromoVerdictType.OK)).count()).isEqualTo(1L);
        assertThat(verdicts.stream().filter(v -> v.getVerdict().equals(PromoVerdictType.USED)).count()).isEqualTo(1L);
    }

    @Test
    public void shouldPass() {
        LoyaltyDetector rule1 = rule(LoyaltyDetectorResult.OK_RESULT);
        LoyaltyDetector rule2 = rule(LoyaltyDetectorResult.OK_RESULT);
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Arrays.asList(rule1, rule2), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter,
                marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyVerdictRequestDto requestDto = request(USER_UID);
        initMocks(USER_UID);
        LoyaltyVerdictDto verdictDto = antifraudService.checkPromoRequest(requestDto);
        assertThat(verdictDto.getVerdict()).isEqualTo(LoyaltyVerdictType.OK);
        List<PromoVerdictDto> verdicts = verdictDto.getPromos();
        assertThat(verdicts.stream().filter(v -> !v.getVerdict().equals(PromoVerdictType.OK)).count()).isEqualTo(0L);
        assertThat(verdictDto.getFirstOrder()).isTrue();
    }

    @Test
    public void notFirstOrder() {
        LoyaltyDetector rule1 = rule(LoyaltyDetectorResult.OK_RESULT);
        LoyaltyDetector rule2 = rule(LoyaltyDetectorResult.OK_RESULT);
        when(ordersService.getOrders(any(MarketUserId.class), any())).thenReturn(CompletableFuture.completedFuture(List.of(
            Order.newBuilder().setId(1L).setStatus("DELIVERED").setRgb(RGBType.BLUE).build()
        )));
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Arrays.asList(rule1, rule2), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter,
                marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyVerdictRequestDto requestDto = request(USER_UID);
        initMocks(USER_UID);
        LoyaltyVerdictDto verdictDto = antifraudService.checkPromoRequest(requestDto);
        assertThat(verdictDto.getFirstOrder()).isFalse();
    }


    @Test
    public void testOrderCount() {
        when(ordersService.getOrders(any(MarketUserId.class), any())).thenReturn(CompletableFuture.completedFuture(List.of(
            Order.newBuilder()
                .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                .setCreationDate(Instant.now().toEpochMilli()).build(),
            Order.newBuilder()
                .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
            Order.newBuilder()
                .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1124L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1124L).build()).setStatus("DELIVERY")
                .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
            Order.newBuilder()
                .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERED")
                .setCreationDate(Instant.now().minus(8L, ChronoUnit.DAYS).toEpochMilli()).build()
        )));
        LoyaltyDetector rule = new PromoRestrictedDetector();
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.singletonList(rule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter
                , marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyBuyerRestrictionsRequestDto requestDto = new LoyaltyBuyerRestrictionsRequestDto(USER_UID, null);
        initMocks(USER_UID);
        LoyaltyBuyerRestrictionsDto restrictions = antifraudService.checkRestrictions(requestDto);
        OrderCountDto glueDayCount = restrictions.getOrderStats().getLastDayGlueOrderStat();
        OrderCountDto glueWeekCount = restrictions.getOrderStats().getLastWeekGlueOrderStat();
        OrderCountDto userDayCount = restrictions.getOrderStats().getLastDayUserOrderStat();
        OrderCountDto userWeekCount = restrictions.getOrderStats().getLastWeekUserOrderStat();
        assertThat(glueDayCount.getTotal()).isEqualTo(1);
        assertThat(glueDayCount.getCancelled()).isEqualTo(1);
        assertThat(glueWeekCount.getTotal()).isEqualTo(3);
        assertThat(glueWeekCount.getDelivered()).isEqualTo(1);
        assertThat(glueWeekCount.getCancelled()).isEqualTo(1);
        assertThat(glueWeekCount.getActive()).isEqualTo(1);
        assertThat(userDayCount.getTotal()).isEqualTo(1);
        assertThat(userDayCount.getCancelled()).isEqualTo(1);
        assertThat(userWeekCount.getTotal()).isEqualTo(2);
        assertThat(userWeekCount.getDelivered()).isEqualTo(1);
        assertThat(userWeekCount.getCancelled()).isEqualTo(1);
    }

    @Test
    public void testMultiOrderCount() {
        when(ordersService.getOrders(any(MarketUserId.class), any())).thenReturn(CompletableFuture.completedFuture(List.of(
            Order.newBuilder()
                .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                .setCreationDate(Instant.now().toEpochMilli()).build(),
            Order.newBuilder()
                .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                .setCreationDate(Instant.now().toEpochMilli()).build(),
            Order.newBuilder()
                .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
            Order.newBuilder()
                .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERY")
                .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
            Order.newBuilder()
                .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
        )));
        LoyaltyDetector rule = new PromoRestrictedDetector();
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.singletonList(rule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyBuyerRestrictionsRequestDto requestDto = new LoyaltyBuyerRestrictionsRequestDto(USER_UID, null);
        initMocks(USER_UID);
        LoyaltyBuyerRestrictionsDto restrictions = antifraudService.checkRestrictions(requestDto);
        OrderCountDto glueDayCount = restrictions.getOrderStats().getLastDayGlueOrderStat();
        OrderCountDto glueWeekCount = restrictions.getOrderStats().getLastWeekGlueOrderStat();
        OrderCountDto userDayCount = restrictions.getOrderStats().getLastDayUserOrderStat();
        OrderCountDto userWeekCount = restrictions.getOrderStats().getLastWeekUserOrderStat();
        assertThat(glueDayCount.getTotal()).isEqualTo(2);
        assertThat(glueDayCount.getCancelled()).isEqualTo(1);
        assertThat(glueDayCount.getActive()).isEqualTo(1);
        assertThat(glueWeekCount.getTotal()).isEqualTo(4);
        assertThat(glueWeekCount.getDelivered()).isEqualTo(1);
        assertThat(glueWeekCount.getCancelled()).isEqualTo(1);
        assertThat(glueWeekCount.getActive()).isEqualTo(2);
        assertThat(userDayCount.getTotal()).isEqualTo(1);
        assertThat(userDayCount.getCancelled()).isEqualTo(1);
        assertThat(userWeekCount.getTotal()).isEqualTo(3);
        assertThat(userWeekCount.getDelivered()).isEqualTo(1);
        assertThat(userWeekCount.getCancelled()).isEqualTo(1);
        assertThat(userWeekCount.getActive()).isEqualTo(1);
    }

    @Test
    public void testCheckRestrictions() {
        LoyaltyDetector rule = new PromoRestrictedDetector();
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.singletonList(rule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao,
                Executors.newSingleThreadExecutor());
        Long USER_UID = 1123L;
        LoyaltyBuyerRestrictionsRequestDto requestDto = new LoyaltyBuyerRestrictionsRequestDto(USER_UID, null);
        initMocks(USER_UID);
        LoyaltyBuyerRestrictionsDto restrictions = antifraudService.checkRestrictions(requestDto);
        assertThat(restrictions.getRestriction()).isEqualTo(LoyaltyRestrictionType.PROHIBITED);
    }

    @Test
    public void testCheckRestrictionsBatch() {
        LoyaltyDetector rule = new PromoRestrictedDetector();
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.singletonList(rule), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter
                , marketUserIdDao, Executors.newSingleThreadExecutor());
        Long USER_UID_1 = 1123L;
        Long USER_UID_2 = 1124L;
        Collection<LoyaltyBuyerRestrictionsRequestDto> requestDto = List.of(
            new LoyaltyBuyerRestrictionsRequestDto(USER_UID_1, null),
            new LoyaltyBuyerRestrictionsRequestDto(USER_UID_2, null)
        );
        initMocks(USER_UID_1);
        initMocks(USER_UID_2);
        Collection<LoyaltyBuyerRestrictionsDto> restrictions = antifraudService.checkRestrictionsBatch(requestDto);
        assertThat(restrictions).hasSize(2);
        for (var restriction : restrictions) {
            assertThat(restriction.getUid()).isNotNull();
            assertThat(restriction.getRestriction()).isEqualTo(LoyaltyRestrictionType.PROHIBITED);
        }
    }

    @SneakyThrows
    @Test
    public void testCountOrders() {
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.emptyList(), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao, Executors.newSingleThreadExecutor());
        when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(MarketUserId.fromUid(1123L))));
        when(ordersService.getOrders(any(OrderCountRequestDto.class), anySet())).thenReturn(List.of(
                Order.newBuilder()
                        .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                        .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERY")
                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
        ));
        Instant from = Instant.now().minus(10L, ChronoUnit.DAYS);
        Instant to = Instant.now();
        OrderCountRequestDto requestDto = OrderCountRequestDto.builder()
                .puid(1123L)
                .from(from)
                .to(to)
                .build();
        OrderCountResponseDto responseDto = antifraudService.countOrders(requestDto).get();
        assertThat(responseDto.getPuid()).isEqualTo(1123L);
        assertThat(responseDto.getFrom()).isEqualTo(from);
        assertThat(responseDto.getTo()).isEqualTo(to);

        OrderCountDto userOrderCount = responseDto.getUserOrderCount();
        assertThat(userOrderCount.getActive()).isEqualTo(1);
        assertThat(userOrderCount.getCancelled()).isEqualTo(1);
        assertThat(userOrderCount.getDelivered()).isEqualTo(1);
        assertThat(userOrderCount.getTotal()).isEqualTo(3);

        OrderCountDto glueOrderCount = responseDto.getGlueOrderCount();
        assertThat(glueOrderCount.getActive()).isEqualTo(2);
        assertThat(glueOrderCount.getCancelled()).isEqualTo(1);
        assertThat(glueOrderCount.getDelivered()).isEqualTo(1);
        assertThat(glueOrderCount.getTotal()).isEqualTo(4);
    }

    @SneakyThrows
    @Test
    public void testCountOrdersPromoFiltered() {
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.emptyList(), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao, Executors.newSingleThreadExecutor());
        when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(MarketUserId.fromUid(1123L))));
        when(configurationService.loyaltyPromoParamName()).thenReturn("promoKey");
        when(ordersService.getOrders(any(OrderCountRequestDto.class), anySet())).thenReturn(List.of(
                Order.newBuilder()
                        .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo1").build())
                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo2").build())
                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo3").build())
                        .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERY")
                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                Order.newBuilder()
                        .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
        ));
        Instant from = Instant.now().minus(10L, ChronoUnit.DAYS);
        Instant to = Instant.now();
        OrderCountRequestDto requestDto = OrderCountRequestDto.builder()
                .puid(1123L)
                .promoFilter("promo1, promo2")
                .from(from)
                .to(to)
                .build();
        OrderCountResponseDto responseDto = antifraudService.countOrders(requestDto).get();
        assertThat(responseDto.getPuid()).isEqualTo(1123L);
        assertThat(responseDto.getFrom()).isEqualTo(from);
        assertThat(responseDto.getTo()).isEqualTo(to);

        OrderCountDto userOrderCount = responseDto.getUserOrderCount();
        assertThat(userOrderCount.getActive()).isEqualTo(0);
        assertThat(userOrderCount.getCancelled()).isEqualTo(1);
        assertThat(userOrderCount.getDelivered()).isEqualTo(0);
        assertThat(userOrderCount.getTotal()).isEqualTo(1);

        OrderCountDto glueOrderCount = responseDto.getGlueOrderCount();
        assertThat(glueOrderCount.getActive()).isEqualTo(1);
        assertThat(glueOrderCount.getCancelled()).isEqualTo(1);
        assertThat(glueOrderCount.getDelivered()).isEqualTo(0);
        assertThat(glueOrderCount.getTotal()).isEqualTo(2);
    }

    @SneakyThrows
    @Test
    public void testCountOrdersV2() {
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.emptyList(), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao, Executors.newSingleThreadExecutor());
        when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(MarketUserId.fromUid(1123L))));
        when(ordersService.getOrders(any(OrderCountRequest.class), anySet()))
                .thenReturn(CompletableFuture.completedFuture(new OrdersService.CachedOrders(
                        List.of(
                                Order.newBuilder()
                                        .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                                        .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERY")
                                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
                        ),
                        Instant.MIN,
                        null
                )));
        Instant from = Instant.now().minus(10L, ChronoUnit.DAYS);
        Instant to = Instant.now();
        OrderCountRequestDto requestDto = OrderCountRequestDto.builder()
                .puid(1123L)
                .from(from)
                .to(to)
                .build();
        var responseDto = antifraudService.countOrders(toRequestV2(requestDto, null)).get();
        assertThat(responseDto.getPuid()).isEqualTo(1123L);
        assertThat(responseDto.getGlueSize()).isEqualTo(1);

        var itemDto = responseDto.getResponseItems().values().iterator().next();

        OrderCountDto userOrderCount = itemDto.getUserOrderCount();
        assertThat(userOrderCount.getActive()).isEqualTo(1);
        assertThat(userOrderCount.getCancelled()).isEqualTo(1);
        assertThat(userOrderCount.getDelivered()).isEqualTo(1);
        assertThat(userOrderCount.getTotal()).isEqualTo(3);

        OrderCountDto glueOrderCount = itemDto.getGlueOrderCount();
        assertThat(glueOrderCount.getActive()).isEqualTo(2);
        assertThat(glueOrderCount.getCancelled()).isEqualTo(1);
        assertThat(glueOrderCount.getDelivered()).isEqualTo(1);
        assertThat(glueOrderCount.getTotal()).isEqualTo(4);
    }

    @SneakyThrows
    @Test
    public void testCountOrdersPromoFilteredV2() {
        LoyaltyAntifraudService antifraudService =
            new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.emptyList(), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao, Executors.newSingleThreadExecutor());
        when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(MarketUserId.fromUid(1123L))));
        when(ordersService.getOrders(any(OrderCountRequest.class), anySet()))
                .thenReturn(CompletableFuture.completedFuture(new OrdersService.CachedOrders(
                        List.of(
                                Order.newBuilder()
                                        .setId(1L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("CANCELLED")
                                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo1").build())
                                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(2L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1125L).build()).setStatus("DELIVERY")
                                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo2").build())
                                        .setCreationDate(Instant.now().toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(3L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                                        .addOrderProperties(OrderProperty.newBuilder().setKey("promoKey").setTextValue("promo3").build())
                                        .setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(4L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERY")
                                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build(),
                                Order.newBuilder()
                                        .setId(5L).setKeyUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build())
                                        .addUid(Uid.newBuilder().setType(UidType.PUID).setIntValue(1123L).build()).setStatus("DELIVERED")
                                        .setMultiOrderId("1").setCreationDate(Instant.now().minus(2L, ChronoUnit.DAYS).toEpochMilli()).build()
                        ),
                        Instant.MIN,
                        null
                )));
        Instant from = Instant.now().minus(10L, ChronoUnit.DAYS);
        Instant to = Instant.now();
        OrderCountRequestDto requestDto = OrderCountRequestDto.builder()
                .puid(1123L)
                .promoFilter("promo1, promo2")
                .from(from)
                .to(to)
                .build();
        var responseDto = antifraudService.countOrders(toRequestV2(requestDto, "promoKey")).get();
        assertThat(responseDto.getPuid()).isEqualTo(1123L);
        assertThat(responseDto.getGlueSize()).isEqualTo(1);

        var itemDto = responseDto.getResponseItems().values().iterator().next();

        OrderCountDto userOrderCount = itemDto.getUserOrderCount();
        assertThat(userOrderCount.getActive()).isEqualTo(0);
        assertThat(userOrderCount.getCancelled()).isEqualTo(1);
        assertThat(userOrderCount.getDelivered()).isEqualTo(0);
        assertThat(userOrderCount.getTotal()).isEqualTo(1);

        OrderCountDto glueOrderCount = itemDto.getGlueOrderCount();
        assertThat(glueOrderCount.getActive()).isEqualTo(1);
        assertThat(glueOrderCount.getCancelled()).isEqualTo(1);
        assertThat(glueOrderCount.getDelivered()).isEqualTo(0);
        assertThat(glueOrderCount.getTotal()).isEqualTo(2);
    }

    @Test
    public void testCountOrdersException() {
        try {
            LoyaltyAntifraudService antifraudService =
                new LoyaltyAntifraudService(loyaltyDataService, gluesService, Collections.emptyList(), List.of(), roleService, mock(BuyerDataService.class), ordersService, walletTransactionsService, configurationService, logFormatter, marketUserIdDao, Executors.newSingleThreadExecutor());
            when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(MarketUserId.fromUid(1123L))));
            when(ordersService.getOrders(any(OrderCountRequest.class), anySet())).thenThrow(new RejectedExecutionException());
            antifraudService.countOrders(OrderCountRequest.builder().build().builder()
                    .puid(1123L)
                    .from(Instant.now())
                    .to(Instant.now())
                    .build()).join();
        } catch (CompletionException e) {
            assertThat(e.getCause()).isOfAnyClassIn(TooManyRequestsException.class);
        }
    }

    private OrderCountRequest toRequestV2(OrderCountRequestDto requestDto, String propertyName) {
        return OrderCountRequest.builder()
                .puid(requestDto.getPuid())
                .timeout(null)
                .from(requestDto.getFrom())
                .to(requestDto.getTo())
                .items(Map.of("zzz", OrderCounter.builder()
                        .from(requestDto.getFrom())
                        .to(requestDto.getTo())
                        .filters(requestDto.getPromoFilters() == null
                                ? List.of()
                                : List.of(OrderCountPromoFilter.builder()
                                .propertyName(propertyName)
                                .promoFilters(requestDto.getPromoFilters())
                                .build()))
                        .build()))
                .build();
    }

    private LoyaltyDetector rule(LoyaltyDetectorResult result) {
        LoyaltyDetector rule = mock(LoyaltyDetector.class);
        when(rule.check(any(), any())).thenReturn(result);
        when(rule.getUniqName()).thenReturn("test_LoyaltyDetector");
        return rule;
    }

    private void initMocks(Long uid) {
        when(loyaltyDataService.findPromoBindedOnlyOnceByPromoIds(anyList())).thenReturn(Arrays.asList(
                LoyaltyPromo.builder().promoId(1201L).bindOnlyOnce(true).build(),
                LoyaltyPromo.builder().promoId(1202L).bindOnlyOnce(true).build()
        ));
        when(loyaltyDataService.findCoinsUsedByUsers(anyLong(), any())).thenReturn(CompletableFuture.completedFuture(Arrays.asList(
                LoyaltyCoin.builder().uid(uid).promoId(1201L).coinId(2001L).build(),
                LoyaltyCoin.builder().uid(uid + 1).promoId(1202L).coinId(2002L).build(),
                LoyaltyCoin.builder().uid(uid + 2).promoId(1203L).coinId(2003L).build()
        )));
        when(gluesService.getGluedIdsWithCache(anyLong(), anyBoolean())).thenReturn(CompletableFuture.completedFuture(Set.of(
                MarketUserId.fromUid(uid),
                MarketUserId.fromUid(uid + 1)
        )));
    }


    private LoyaltyVerdictRequestDto request(Long uid) {
        return LoyaltyVerdictRequestDto.builder()
                .uid(uid)
                .reason("USER_CHECK")
                .coins(Arrays.asList(
                        new CoinDto(2001L, 1201L),
                        new CoinDto(2002L, 1202L)
                ))
                .userParams(OrderBuyerRequestDto.builder().build())
                .build();
    }
}
