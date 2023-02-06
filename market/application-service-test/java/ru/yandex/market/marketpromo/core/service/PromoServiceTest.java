package ru.yandex.market.marketpromo.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.core.test.utils.PromoTestHelper;
import ru.yandex.market.marketpromo.filter.PromoFilter;
import ru.yandex.market.marketpromo.filter.PromoRequest;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoKey;
import ru.yandex.market.marketpromo.model.PromoStatus;
import ru.yandex.market.marketpromo.service.PromoService;
import ru.yandex.market.marketpromo.utils.IdentityUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;


public class PromoServiceTest extends ServiceTestBase {

    private static final PromoKey DD_PROMO_KEY =
            IdentityUtils.decodePromoId("direct-discount$83972ad2-da80-11ea-87d0-0242ac130003");
    @Autowired
    private PromoService promoService;
    @Autowired
    private PromoDao promoDao;

    @BeforeEach
    void init() {
        promoDao.replace(PromoTestHelper.defaultPromo(DD_PROMO_KEY));
    }

    @Test
    void shouldFindByCategory() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filterList(PromoFilter.CATEGORY_ID, Set.of(123L, 1234L, 123456L))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
        assertThat(promos.get(0), hasProperty("id", is(DD_PROMO_KEY.getId())));
    }

    @Test
    void shouldFindAfterStartDate() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.START_DATE, LocalDateTime.now().minusDays(1))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
        assertThat(promos.get(0), hasProperty("id", is(DD_PROMO_KEY.getId())));
    }

    @Test
    void shouldFindBeforeEndDate() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.END_DATE, LocalDateTime.now().plusDays(11))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
        assertThat(promos.get(0), hasProperty("id", is(DD_PROMO_KEY.getId())));
    }

    @Test
    void shouldFindBetweenStartAndEndDate() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.END_DATE, LocalDateTime.now().plusDays(11))
                .filter(PromoFilter.START_DATE, LocalDateTime.now().minusDays(1))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
        assertThat(promos.get(0), hasProperty("id", is(DD_PROMO_KEY.getId())));
    }

    @Test
    void shouldNotFindBetweenStartAndEndDate() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.END_DATE, LocalDateTime.now().minusDays(1))
                .filter(PromoFilter.START_DATE, LocalDateTime.now().minusDays(5))
                .limit(50)
                .build());

        assertThat(promos, hasSize(0));
    }

    @Test
    void shouldFindWithErrors() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.HAS_ERRORS, Boolean.TRUE)
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
    }

    @Test
    void shouldFindInPromoIdList() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                        .filter(PromoFilter.ANAPLAN_ID, List.of("#21098", "#21056"))
                        .limit(50)
                        .build());

        assertThat(promos, hasSize(1));
    }

    @Test
    void shouldFindInPromoIdListByRegexp() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.ANAPLAN_ID, List.of("21098", "#2105"))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
    }

    @Test
    void shouldFindInTradesList() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                        .filter(PromoFilter.TRADE_LOGIN, List.of(Promos.LOGIN_1, Promos.LOGIN_2))
                        .limit(50)
                        .build());

        assertThat(promos, hasSize(1));
    }

    @Test
    void shouldNotFindInTradesList() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                        .filter(PromoFilter.TRADE_LOGIN, List.of(Promos.LOGIN_2))
                        .limit(50)
                        .build());

        assertThat(promos, hasSize(0));
    }


    @Test
    void shouldFindInStatusesList() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.STATUS, List.of(PromoStatus.CREATED, PromoStatus.READY))
                .limit(50)
                .build());

        assertThat(promos, hasSize(1));
    }

    @Test
    void shouldNotFindInStatusesList() {
        List<Promo> promos = promoService.searchPromos(PromoRequest.builder()
                .filter(PromoFilter.STATUS, List.of( PromoStatus.READY))
                .limit(50)
                .build());

        assertThat(promos, hasSize(0));
    }
}
