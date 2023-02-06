package ru.yandex.market.ocrm.module.loyalty.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.entity.EntityStorageService;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.loyalty.api.model.CouponValueType;
import ru.yandex.market.loyalty.api.model.UsageClientDeviceType;
import ru.yandex.market.loyalty.api.model.coin.CoinCreationReason;
import ru.yandex.market.loyalty.api.model.coin.CoinStatus;
import ru.yandex.market.loyalty.api.model.coin.CoinType;
import ru.yandex.market.loyalty.api.model.coin.OCRMUserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.ReasonParamExt;
import ru.yandex.market.loyalty.api.model.ocrm.Coupon;
import ru.yandex.market.loyalty.api.model.ocrm.CouponRestrictions;
import ru.yandex.market.loyalty.api.model.ocrm.OrderCoins;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCoin;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCoinCreationReason;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCoinStatus;
import ru.yandex.market.ocrm.module.loyalty.LoyaltyCoinType;
import ru.yandex.market.ocrm.module.loyalty.MarketLoyaltyService;
import ru.yandex.market.ocrm.module.loyalty.ModuleLoyaltyTestConfiguration;
import ru.yandex.market.ocrm.module.order.domain.Order;
import ru.yandex.market.ocrm.module.order.test.OrderTestUtils;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleLoyaltyTestConfiguration.class)
public class ModuleLoyaltyTest {

    @Inject
    MarketLoyaltyService marketLoyaltyService;
    @Inject
    EntityStorageService entityStorageService;
    @Inject
    OrderTestUtils orderTestUtils;

    Order order;
    OCRMUserCoinResponse coin2;
    OCRMUserCoinResponse coin3;
    private Coupon coupon;

    @BeforeEach
    public void setUp() {
        order = orderTestUtils.createOrder();

        coin2 = createCoin(2);
        coin3 = createCoin(3);
        coupon = createCoupon();
    }

    /**
     * Проверяем, что умеем получить список монет по id-заказа
     */
    @Test
    public void checkQuery_orderId() {
        doMokLoyaltyClient();

        List<LoyaltyCoin> list = getLoyaltyCoins(order.getTitle());

        Assertions.assertEquals(2, list.size());
    }

    /**
     * Проверяем, что умеем получть список монет по {@link HasGid#getGid() gid} заказа
     */
    @Test
    public void checkQuery_orderGid() {
        doMokLoyaltyClient();

        List<LoyaltyCoin> list = getLoyaltyCoins(order.getGid());

        Assertions.assertEquals(2, list.size());
    }

    /**
     * Проверяем правильность заполнения атрибутов монеты по данным из лоялти
     */
    @Test
    public void check_coin2() {
        doMokLoyaltyClient();

        List<LoyaltyCoin> list = getLoyaltyCoins(order.getGid());

        Assertions.assertEquals(2, list.size());

        LoyaltyCoin c2 = getLoyaltyCoin(list, 2);
        Assertions.assertNotNull(c2);
        Assertions.assertEquals(coin2.getCreationDate().getTime() / 1_000,
                c2.<OffsetDateTime>getAttribute(LoyaltyCoin.CREATION_DATE).toEpochSecond());
        Assertions.assertEquals(coin2.getDescription(), c2.getAttribute(LoyaltyCoin.DESCRIPTION));
        Assertions.assertEquals(coin2.getEndDate().getTime() / 1_000,
                c2.<OffsetDateTime>getAttribute(LoyaltyCoin.END_DATE).toEpochSecond());
        Assertions.assertEquals(coin2.getImage(), c2.getAttribute(LoyaltyCoin.IMAGE));
        Assertions.assertEquals(coin2.getNominal(), c2.getAttribute(LoyaltyCoin.NOMINAL));
        Assertions.assertEquals(coin2.getReason().name(),
                c2.<LoyaltyCoinCreationReason>getAttribute(LoyaltyCoin.REASON).getCode());
        Assertions.assertEquals(coin2.getStatus().name(),
                c2.<LoyaltyCoinStatus>getAttribute(LoyaltyCoin.STATUS).getCode());
        Assertions.assertEquals(coin2.getSubtitle(), c2.getAttribute(LoyaltyCoin.SUBTITLE));
        Assertions.assertEquals(coin2.getTitle(), c2.getAttribute(LoyaltyCoin.TITLE));
        Assertions.assertEquals(coin2.getCoinType().name(),
                c2.<LoyaltyCoinType>getAttribute(LoyaltyCoin.TYPE).getCode());
    }

    @Test
    public void check_used() {
        doMokLoyaltyClient();

        List<LoyaltyCoin> list = getLoyaltyCoins(order.getGid());

        LoyaltyCoin c2 = getLoyaltyCoin(list, 2);
        Assertions.assertNotNull(c2);
        Assertions.assertTrue(c2.<Boolean>getAttribute(LoyaltyCoin.USED_OR_ISSUED));
    }

    @Test
    public void check_issued() {
        doMokLoyaltyClient();

        List<LoyaltyCoin> list = getLoyaltyCoins(order.getGid());

        LoyaltyCoin c3 = getLoyaltyCoin(list, 3);
        Assertions.assertNotNull(c3);
        Assertions.assertFalse(c3.<Boolean>getAttribute(LoyaltyCoin.USED_OR_ISSUED));
    }

    @Test
    public void filter_issued() {
        doMokLoyaltyClient();

        Query query = Query.of(LoyaltyCoin.FQN)
                .withFilters(
                        Filters.eq(LoyaltyCoin.ORDERS, order.getGid()),
                        Filters.eq(LoyaltyCoin.USED_OR_ISSUED, false)
                );
        List<LoyaltyCoin> list = entityStorageService.list(query);

        Assertions.assertEquals(1, list.size());
        LoyaltyCoin c3 = getLoyaltyCoin(list, 3);
        Assertions.assertNotNull(c3);
    }

    @Test
    public void filter_used() {
        doMokLoyaltyClient();

        Query query = Query.of(LoyaltyCoin.FQN)
                .withFilters(
                        Filters.eq(LoyaltyCoin.ORDERS, order.getGid()),
                        Filters.eq(LoyaltyCoin.USED_OR_ISSUED, true)
                );
        List<LoyaltyCoin> list = entityStorageService.list(query);

        Assertions.assertEquals(1, list.size());
        LoyaltyCoin c3 = getLoyaltyCoin(list, 2);
        Assertions.assertNotNull(c3);
    }

    private LoyaltyCoin getLoyaltyCoin(List<LoyaltyCoin> list, int entityId) {
        return Iterables.find(list, i -> LoyaltyCoin.FQN.gidOf(entityId).equals(i.getGid()), null);
    }

    private List<LoyaltyCoin> getLoyaltyCoins(Object order) {
        Query query = Query.of(LoyaltyCoin.FQN)
                .withFilters(Filters.eq(LoyaltyCoin.ORDERS, order));
        return entityStorageService.list(query);
    }

    private void doMokLoyaltyClient() {
        OrderCoins result = new OrderCoins(List.of(coin2), List.of(coin3), coupon);
        Mockito.when(marketLoyaltyService.getIssuedAndUsedCoinsForOrder(Mockito.eq(order.getTitle()))).thenReturn(result);
    }

    private OCRMUserCoinResponse createCoin(long id) {
        return new OCRMUserCoinResponse(
                id,
                Randoms.string(),
                Randoms.string(),
                CoinType.FIXED,
                Randoms.bigDecimal(),
                Randoms.string(),
                Randoms.string(),
                new Date(),
                new Date(),
                Randoms.string(),
                new HashMap<>(),
                Randoms.string(),
                CoinStatus.ACTIVE,
                true,
                Randoms.string(),
                CoinCreationReason.ORDER,
                Randoms.string(),
                new ArrayList<>(),
                Randoms.string(),
                new ReasonParamExt(
                        Collections.emptyList(),
                        "multiOrderId"),
                123L

        );
    }

    private Coupon createCoupon() {
        return new Coupon("couponCode",
                BigDecimal.valueOf(11),
                BigDecimal.valueOf(123),
                CouponValueType.FIXED,
                Date.from(
                        OffsetDateTime.of(
                                LocalDate.of(2020, 9, 1),
                                LocalTime.of(12, 01, 0),
                                ZoneOffset.UTC).toInstant()),
                Date.from(
                        OffsetDateTime.of(
                                LocalDate.of(2020, 10, 1),
                                LocalTime.of(12, 01, 0),
                                ZoneOffset.UTC).toInstant()),
                new CouponRestrictions(
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1000),
                        true,
                        Set.of(UsageClientDeviceType.DESKTOP),
                        "some coupon description"
                ),
                Collections.emptyList()
        );
    }
}
