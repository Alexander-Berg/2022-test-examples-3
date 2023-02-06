package ru.yandex.market.loyalty.back.controller;

import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.api.model.promocode.StorePromocodeRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.loyalty.core.dao.promocode.StorePromocodeDao;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@TestFor(StorePromocodeController.class)
public class StorePromocodeControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    private static final String PROMOCODE = "PROMOCODE";
    private static final String ANOTHER_PROMOCODE = "ANOTHER_PROMOCODE";

    @Autowired
    private MarketLoyaltyClient client;

    @Autowired
    private PromoManager promoManager;
    @Autowired
    private StorePromocodeDao storePromocodeDao;

    @Before
    public void configure() {
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(PROMOCODE));
        promoManager.createPromocodePromo(PromoUtils.SmartShopping.defaultFixedPromocode()
                .setCode(ANOTHER_PROMOCODE));
    }

    @Test
    public void shouldSavePromocodes() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE))
                        .build()
        );

        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(1));
        assertThat(storePromocodeDao.getPromocodes(identity), hasItem(PROMOCODE));

        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(ANOTHER_PROMOCODE))
                        .build()
        );

        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(2));
        assertThat(
                storePromocodeDao.getPromocodes(identity),
                containsInAnyOrder(
                        allOf(
                                equalTo(PROMOCODE)
                        ),
                        allOf(
                                equalTo(ANOTHER_PROMOCODE)
                        )
                )
        );
    }

    @Test
    public void shouldSavePromocodesBatch() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE, ANOTHER_PROMOCODE))
                        .build()
        );
        assertThat(storePromocodeDao.getPromocodes(identity), notNullValue());
        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(2));
        assertThat(
                storePromocodeDao.getPromocodes(identity),
                containsInAnyOrder(
                        allOf(
                                equalTo(PROMOCODE)
                        ),
                        allOf(
                                equalTo(ANOTHER_PROMOCODE)
                        )
                )
        );
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotSaveNotExistedPromocode() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE, "NOT_CREATED_PROMO"))
                        .build()
        );
        assertThat(storePromocodeDao.getPromocodes(identity), notNullValue());
        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(1));
        assertThat(storePromocodeDao.getPromocodes(identity), hasItem(PROMOCODE));
    }

    @Test
    public void shouldRemovePromocode() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE, ANOTHER_PROMOCODE))
                        .build()
        );
        assertThat(storePromocodeDao.getPromocodes(identity), notNullValue());
        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(2));

        client.removeStoredPromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE))
                        .build()
        );

        assertThat(storePromocodeDao.getPromocodes(identity), notNullValue());
        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(1));
        assertThat(storePromocodeDao.getPromocodes(identity), hasItem(ANOTHER_PROMOCODE));

    }

    @Test
    public void shouldRemoveAllPromocodes() {
        Identity<?> identity = Identity.Type.UID.buildIdentity("123");
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE, ANOTHER_PROMOCODE))
                        .build()
        );
        assertThat(storePromocodeDao.getPromocodes(identity), notNullValue());
        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(2));

        client.removeStoredPromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .build()
        );

        assertThat(storePromocodeDao.getPromocodes(identity), hasSize(0));
    }

    @Test
    public void shouldBlockTooManyRequests() throws InterruptedException {
        int rpsPerUserLimit = 2;
        configurationService.set(ConfigurationService.API_RPS_LIMIT_PER_USER, rpsPerUserLimit);
        configurationService.set(ConfigurationService.API_RPS_LIMIT_PER_USER_ENABLED, true);

        Stream.iterate(0, i -> i + 1)
                .limit(100)
                .forEach(uid -> {
                    Identity<?> identity = Identity.Type.UID.buildIdentity(String.valueOf(uid));
                    storePromoCodes(identity);
                    assertThat(storePromocodeDao.getPromocodes(identity), hasSize(1));
                });

        Identity<?> sameUser = Identity.Type.UID.buildIdentity("111");
        for (int i = 0; i < rpsPerUserLimit; i++) {
            storePromoCodes(sameUser);
        }

        try {
            storePromoCodes(sameUser);
            fail("should not store promo faster than rate limit");
        } catch (MarketLoyaltyException e) {
            assertEquals(MarketLoyaltyErrorCode.SERVICE_IS_OVERLOADED, e.getMarketLoyaltyErrorCode());
        }

        Thread.sleep(1_000);
        storePromoCodes(sameUser);
        assertThat(storePromocodeDao.getPromocodes(sameUser), hasSize(1));

        configurationService.set(ConfigurationService.API_RPS_LIMIT_PER_USER_ENABLED, false);
    }

    private void storePromoCodes(Identity<?> identity) {
        client.storePromocodes(
                identity,
                StorePromocodeRequest.builder()
                        .setCodes(Set.of(PROMOCODE))
                        .build()
        );
    }
}
