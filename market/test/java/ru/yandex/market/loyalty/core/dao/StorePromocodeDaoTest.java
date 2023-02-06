package ru.yandex.market.loyalty.core.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.model.identity.Identity;
import ru.yandex.market.loyalty.core.dao.promocode.StorePromocodeDao;
import ru.yandex.market.loyalty.core.mock.ClockForTests;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.lightweight.DateUtils;

import java.sql.Timestamp;
import java.util.Set;

import static java.sql.Timestamp.valueOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.loyalty.test.SameCollection.sameCollectionInAnyOrder;

public class StorePromocodeDaoTest extends MarketLoyaltyCoreMockedDbTestBase {

    private final Long userId = 123123L;
    private final Identity<?> userIdentinty = Identity.Type.UID.buildIdentity(String.valueOf(userId));

    public static final String PROMOCODE = "PROMOCODE";
    public static final String ANOTHER_CODE = "ANOTHER_CODE";

    @Autowired
    private StorePromocodeDao storePromocodeDao;
    @Autowired
    protected ClockForTests clock;

    private Timestamp now;

    @Before
    public void init() {
        clock.setDate(valueOf("2021-01-01 10:50:47"));
        now = Timestamp.from(clock.instant());
    }

    @Test
    public void shouldGetPromocodesFromStore() {
        var promocodes = Set.of(PROMOCODE, ANOTHER_CODE);
        storePromocodeDao.insertPromocodes(userIdentinty, promocodes, now);
        var promocodesFromDao = storePromocodeDao.getPromocodes(userIdentinty);
        assertThat(promocodesFromDao, sameCollectionInAnyOrder(promocodes));
    }

    @Test
    public void shouldNotInsertEqualsPromocodesToStore() {
        var promocodes = Set.of(PROMOCODE, ANOTHER_CODE);
        storePromocodeDao.insertPromocodes(userIdentinty, promocodes, now);
        storePromocodeDao.insertPromocodes(userIdentinty, Set.of(PROMOCODE), now);
        promocodes = storePromocodeDao.getPromocodes(userIdentinty);
        assertThat(promocodes, hasSize(2));
    }

    @Test
    public void shouldUpdateDateWhileInsert() {
        clock.setDate(valueOf("2021-01-01 10:50:47"));
        storePromocodeDao.insertPromocodes(userIdentinty, Set.of(PROMOCODE), Timestamp.from(clock.instant()));

        clock.setDate(valueOf("2025-04-28 12:15:13"));
        Timestamp lastSaveDate = Timestamp.from(clock.instant());
        storePromocodeDao.insertPromocodes(userIdentinty, Set.of(PROMOCODE), lastSaveDate);
        var promocodes = storePromocodeDao.getPromocodesWithCreationTime(userIdentinty);
        assertThat(promocodes, hasSize(1));
        assertThat(promocodes.iterator().next().getCreatingDate(), equalTo(DateUtils.fromDate(lastSaveDate)));
    }

    @Test
    public void shouldDeleteAllPromocodesForUser() {
        var promocodes = Set.of(PROMOCODE, ANOTHER_CODE);
        storePromocodeDao.insertPromocodes(userIdentinty, promocodes, now);
        storePromocodeDao.deletePromocodes(userIdentinty);
        promocodes = storePromocodeDao.getPromocodes(userIdentinty);
        assertThat(promocodes, hasSize(0));
    }

    @Test
    public void shouldDeleteSinglePromocodesForUser() {
        var promocodes = Set.of(PROMOCODE, ANOTHER_CODE);
        storePromocodeDao.insertPromocodes(userIdentinty, promocodes, now);
        storePromocodeDao.deletePromocodes(userIdentinty, Set.of(ANOTHER_CODE));
        promocodes = storePromocodeDao.getPromocodes(userIdentinty);
        assertThat(promocodes, hasSize(1));
        assertThat(promocodes, contains(PROMOCODE));
    }
}
