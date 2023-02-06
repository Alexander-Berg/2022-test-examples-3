package ru.yandex.market.loyalty.core.dao;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.querydsl.sql.SQLQueryFactory;
import lombok.AllArgsConstructor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.core.model.*;
import ru.yandex.market.loyalty.core.model.promo.CorePromoType;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoExternalIdentifier;
import ru.yandex.market.loyalty.core.dao.promo.PromoLightDto;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.PromocodePromoBuilder;
import ru.yandex.market.loyalty.core.model.promo.QPromo;
import ru.yandex.market.loyalty.core.model.sort.enums.PromoSortColumn;
import ru.yandex.market.loyalty.core.rule.ParamsContainer;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static ru.yandex.market.loyalty.api.model.PromoStatus.ACTIVE;
import static ru.yandex.market.loyalty.api.model.PromoStatus.INACTIVE;
import static ru.yandex.market.loyalty.core.dao.promo.Queries.PROMO_STATUS_FIELD;
import static ru.yandex.market.loyalty.core.dao.promo.Queries.START_DATE_FIELD;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.ANAPLAN_ID;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.COUPON_EMISSION_DATE_TO;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.IS_PERSONAL_PROMO;
import static ru.yandex.market.loyalty.spring.utils.DaoUtils.args;
import static ru.yandex.market.loyalty.spring.utils.DaoUtils.numeric;
import static ru.yandex.market.loyalty.spring.utils.DaoUtils.text;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;
import static NMarket.Common.Promo.Promo.ESourceType.ANAPLAN_VALUE;

/**
 * @author Ivan Anisimov
 * valter@yandex-team.ru
 * 01.06.17
 */
public class PromoDaoTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SQLQueryFactory queryFactory;

    private static final String PROMO_KEY = "new_promo_key";

    @Test
    public void testUpdateStatus() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        promoDao.updatePromoStatus(promo, PromoStatus.ACTIVE);

        assertEquals(PromoStatus.ACTIVE, promoDao.getPromo(promo.getPromoId().getId()).getStatus());
    }

    @Test
    public void generatePromoKeyOnInsert() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());

        Promo actualPromo = promoDao.getPromo(promo.getPromoId().getId());
        String promoKey = actualPromo.getPromoKey();

        assertNotNull(promoKey);
        assertEquals(22, promoKey.length());
        assertThat(promoKey, not(containsString("+")));
        assertThat(promoKey, not(containsString("/")));
    }

    @Test
    public void shouldSortPromosById() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());


        List<Long> actualPromoOrder = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            SortedResponse<Promo> pagedPromos = promoDao.getPagedPromos(new Sort<>(PromoSortColumn.ID,
                            Sort.Direction.ASC), i, 2, false, null,
                    Collections.singleton(CoreMarketPlatform.BLUE),
                    Collections.singleton(CorePromoType.MARKET_COUPON), null, null, null
            );
            actualPromoOrder.addAll(
                    pagedPromos.getData()
                            .stream()
                            .map(promo -> promo.getPromoId().getId())
                            .collect(Collectors.toList())
            );
        }
        List<Long> expectedPromoOrder = actualPromoOrder.stream().sorted().collect(Collectors.toList());

        assertThat(actualPromoOrder, hasSize(8));
        assertEquals(actualPromoOrder, expectedPromoOrder);
    }

    @Test
    public void shouldSortPromosByBudget() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(100_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(200_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(300_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(400)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(16_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.ONE));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(1_200_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setBudget(BigDecimal.valueOf(15_300_000)));

        List<Long> actualPromoOrder = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            SortedResponse<Promo> pagedPromos = promoDao.getPagedPromos(new Sort<>(PromoSortColumn.CURRENT_BUDGET,
                            Sort.Direction.ASC), i, 2, false, null,
                    Collections.singleton(CoreMarketPlatform.BLUE),
                    Collections.singleton(CorePromoType.MARKET_COUPON), null, null, null
            );
            actualPromoOrder.addAll(pagedPromos.getData().stream().map(promo -> promo.getCurrentBudget().longValueExact()).collect(Collectors.toList()));
        }
        List<Long> expectedPromoOrder = Arrays.asList(1L, 400L, 16_000L, 100_000L, 200_000L, 300_000L, 1_200_000L,
                15_300_000L);

        assertEquals(expectedPromoOrder, actualPromoOrder);
    }

    @Test
    public void shouldSortPromosByEmissionBudget() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(100_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(200_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(300_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(400)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(16_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.ONE));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(1_200_000)));
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse().setEmissionBudget(BigDecimal.valueOf(15_300_000)));

        List<Long> actualPromoOrder = new ArrayList<>();
        for (int i = 1; i < 5; i++) {
            SortedResponse<Promo> pagedPromos =
                    promoDao.getPagedPromos(new Sort<>(PromoSortColumn.CURRENT_EMISSION_BUDGET, Sort.Direction.ASC), i,
                            2, false, null,
                            Collections.singleton(CoreMarketPlatform.BLUE),
                            Collections.singleton(CorePromoType.MARKET_COUPON), null, null, null
                    );
            actualPromoOrder.addAll(pagedPromos.getData().stream().map(promo -> promo.getCurrentEmissionBudget().longValueExact()).collect(Collectors.toList()));
        }
        List<Long> expectedPromoOrder = Arrays.asList(1L, 400L, 16_000L, 100_000L, 200_000L, 300_000L, 1_200_000L,
                15_300_000L);

        assertEquals(expectedPromoOrder, actualPromoOrder);
    }


    @Test
    public void shouldSkipUnknownParameters() {
        Promo promo = promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        jdbcTemplate.update(
                "INSERT INTO promo_params(promo_id,name,value) VALUES(?,?,?)",
                args(
                        numeric(promo.getPromoId().getId()),
                        text("__EMISSIONLESS"),
                        text("")
                )
        );

        promoDao.getPromo(promo.getPromoId().getId());
    }

    @Test
    public void shouldGetShopPromoIds() {
        final String SHOP_PROMO_ID = "shop promo id";
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId(SHOP_PROMO_ID)
                .setAnaplanId(SHOP_PROMO_ID)
                .setCode("PROMOCODE"));

        Map<Long, PromoExternalIdentifier> promoIds = promoDao.getPromoExternalIds(CorePromoType.SMART_SHOPPING);

        assertEquals(SHOP_PROMO_ID, promoIds.get(promo.getPromoId().getId()).getShopPromoId());
        assertEquals(SHOP_PROMO_ID, promoIds.get(promo.getPromoId().getId()).getAnaplanId());
    }

    @Test
    public void shouldGetPromoClidParam() {
        Long CLID = 12345L;
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode().setCode(
                "PROMOCODE"));
        jdbcTemplate.update(
                "INSERT INTO promo_params(promo_id,name,value) VALUES(?,?,?)",
                args(
                        numeric(promo.getPromoId().getId()),
                        text(PromoParameterName.CLID.getCode()),
                        numeric(CLID)
                )
        );

        ParamsContainer<PromoParameterName<?>> promoParams = promoDao.getPromoParams(Set.of(promo.getPromoId().getId()))
                .getOrDefault(promo.getPromoId().getId(), null);

        Long actualClid = ParamsContainer.getSingleParam(promoParams, PromoParameterName.CLID)
                .map(GenericParam::value)
                .orElse(null);

        assertEquals(CLID, actualClid);
    }

    @Test
    public void shouldFindPromoByPromoStorageId() {
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setPromoStorageId("promostorageid")
                .setCode("promocode"));

        Promo foundPromo = promoDao.getPromoByPromoStorageId("promostorageid");

        assertEquals(PromoStatus.ACTIVE, promoDao.getPromo(promo.getPromoId().getId()).getStatus());
        assertEquals(promo.getPromoId().getId(), foundPromo.getPromoId().getId());
    }

    @Test
    public void shouldFindPromoByShopPromoId() {
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode"));

        Promo foundPromo = promoDao.getPromoByShopPromoId("shopPromoId");

        assertEquals(promo.getPromoId().getId(), foundPromo.getPromoId().getId());
    }

    @Test
    public void shouldGetPromoWithMaxStartDateFirstByShopPromoId() {
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode1")
                .setStartDate(new Date(clock.instant().minus(2, ChronoUnit.DAYS).toEpochMilli()))
                .setEndDate(new Date(clock.instant().minus(1, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(INACTIVE));

        Promo promo2 = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode2")
                .setStartDate(new Date(clock.instant().toEpochMilli()))
                .setEndDate(new Date(clock.instant().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(INACTIVE));

        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode3")
                .setStartDate(new Date(clock.instant().minus(3, ChronoUnit.DAYS).toEpochMilli()))
                .setEndDate(new Date(clock.instant().minus(2, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(INACTIVE));

        Promo promo = promoDao.getPromoByShopPromoId("shopPromoId");

        assertEquals(promo2.getPromoId().getId(), promo.getPromoId().getId());
    }

    @Test
    public void shouldGetActivePromoFirstByShopPromoId() {
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode1")
                .setStartDate(new Date(clock.instant().minus(2, ChronoUnit.DAYS).toEpochMilli()))
                .setEndDate(new Date(clock.instant().minus(1, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(INACTIVE));

        Promo promo2 = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode2")
                .setStartDate(new Date(clock.instant().toEpochMilli()))
                .setEndDate(new Date(clock.instant().plus(1, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(ACTIVE));

        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode3")
                .setStartDate(new Date(clock.instant().minus(3, ChronoUnit.DAYS).toEpochMilli()))
                .setEndDate(new Date(clock.instant().minus(2, ChronoUnit.DAYS).toEpochMilli()))
                .setStatus(INACTIVE));

        Promo promo = promoDao.getPromoByShopPromoId("shopPromoId");

        assertEquals(promo2.getPromoId().getId(), promo.getPromoId().getId());
    }

    @Test
    public void shouldNotDuplicateShopPromoIdWhenAnaplanIdIsSet() {
        Promo promo = promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setAnaplanId("shopPromoId")
                .setCode("promocode"));

        Promo foundPromo = promoDao.getPromo(promo.getPromoId().getId());

        assertThat(foundPromo.getShopPromoId(), is("shopPromoId"));
    }

    @Test
    public void shouldNotUpdateAnplanId() {
        PromocodePromoBuilder promoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setAnaplanId("anaplanId")
                .setShopPromoId("anaplanId")
                .setPromoSource(ANAPLAN_VALUE)
                .setCode("promocode");

        Promo promo = promoManager.createPromocodePromo(promoBuilder);

        assertEquals("anaplanId", promoDao.getPromo(promo.getPromoId().getId()).getShopPromoId());

        promoBuilder.setAnaplanId("anaplanId2");
        promoDao.update(promoBuilder.basePromo());

        assertEquals("anaplanId", promoDao.getPromo(promo.getPromoId().getId()).getPromoParamRequired(ANAPLAN_ID));
    }

    @Test
    public void shouldMapQueryToNonEntityClass() {
        Promo cashbackPromo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
                        .setBudget(BigDecimal.valueOf(1000.12))
                        .setEndDate(Date.from(clock.dateTime().plusDays(2).toInstant(ZoneOffset.UTC)))
        );
        Optional<Promo.PromoStatusWithBudget> promoStatusWithBudgetOpt = promoDao.getPromoStatusWithBudget(
                cashbackPromo.getPromoKey());
        assertThat(promoStatusWithBudgetOpt.isPresent(), equalTo(Boolean.TRUE));
        Promo.PromoStatusWithBudget promoStatusWithBudget = promoStatusWithBudgetOpt.get();
        assertThat(promoStatusWithBudget, hasProperty("currentBudget", equalTo(cashbackPromo.getCurrentBudget())));
        assertThat(promoStatusWithBudget, hasProperty("status", equalTo(cashbackPromo.getStatus())));

        List<Promo.PromoStatusWithBudget> allActiveNotEndedPromos = promoDao.findAllActiveNotEndedPromos(
                PromoStatus.ACTIVE.getCode(), clock.instant());
        assertThat(allActiveNotEndedPromos, hasSize(1));
    }

    @Test
    public void shouldReturnCustomFields() {
        @AllArgsConstructor
        class TestResult {
            private final Long id;
            private final String promoKey;
        }
        promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE)
                        .setBudget(BigDecimal.valueOf(1000.12))
                        .setEndDate(Date.from(clock.dateTime().plusDays(2).toInstant(ZoneOffset.UTC)))
        );
        QPromo promo = QPromo.promo;
        List<TestResult> collect = queryFactory.query()
                .select(promo.id, promo.promoKey)
                .from(promo)
                .fetch()
                .stream()
                .map(tuple ->
                        new TestResult(
                                tuple.get(promo.id),
                                tuple.get(promo.promoKey)
                        ))
                .collect(Collectors.toList());
        assertThat(collect, hasSize(greaterThan(0)));
    }

    @Test
    public void getAllPromoIds() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode"));
        promoManager.createCashbackPromo(PromoUtils.Cashback.defaultFixed(BigDecimal.TEN));

        assertThat(promoDao.getAllPromoIds(), hasSize(3));
    }

    @Test
    public void getPromosByTagParam() {
        promoManager.createCouponPromo(PromoUtils.Coupon.defaultSingleUse());
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode")
        );

        List<PromoLightDto> promos = promoDao.getPromosByTagParam(
                IS_PERSONAL_PROMO,
                START_DATE_FIELD.leTo(java.sql.Date.from(clock.instant())).and(PROMO_STATUS_FIELD.eqTo(ACTIVE))
        );

        assertThat(promos, hasSize(0));

        assertThrows(IllegalArgumentException.class, () ->
                promoDao.getPromosByTagParam(
                        COUPON_EMISSION_DATE_TO,
                        START_DATE_FIELD.leTo(java.sql.Date.from(clock.instant())).and(PROMO_STATUS_FIELD.eqTo(ACTIVE))
                )
        );
    }

    @Test
    public void shouldAddNewPromoKeyInTablePromoKeys() {
        PromocodePromoBuilder promoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode");

        Promo promocodePromo = promoManager.createPromocodePromo(promoBuilder);

        promoDao.updateStatusAndPromoKey(promocodePromo, PromoStatus.INACTIVE, PROMO_KEY);
        assertThat(promoDao.getCountPromoKeys(promocodePromo.getPromoId().getId()), is(2));
    }

    @Test
    public void shouldNotAddOldPromoKeyInTablePromoKeys() {
        PromocodePromoBuilder promoBuilder = PromoUtils.SmartShopping.defaultFixedPromocode()
                .setShopPromoId("shopPromoId")
                .setCode("promocode");

        Promo promocodePromo = promoManager.createPromocodePromo(promoBuilder);

        promoDao.updateStatusAndPromoKey(promocodePromo, PromoStatus.INACTIVE, promocodePromo.getPromoKey());
        assertThat(promoDao.getCountPromoKeys(promocodePromo.getPromoId().getId()), is(1));
    }
}
