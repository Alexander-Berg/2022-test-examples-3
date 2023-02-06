package ru.yandex.market.loyalty.core.service.perks;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.perk.StaticPerkStatus;
import ru.yandex.market.loyalty.core.dao.ydb.StaticPerkDao;
import ru.yandex.market.loyalty.core.stub.YdbStaticPerkDaoStub;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestFor(StaticPerkService.class)
public class StaticPerkServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final long UID = 123456;
    private static final String TEST_PERK_1 = "SOME_TEST_PERK";
    private static final String TEST_PERK_2 = "ANOTHER_TEST_PERK";
    private static final String TEST_PERK_3 = "ANOTHER_ONE_TEST_PERK";

    @Autowired
    private StaticPerkService staticPerkService;
    @Autowired
    private StaticPerkDao staticPerkDao;

    @Test
    public void getPerksForUserTest() {
        staticPerkDao.upsert(UID, TEST_PERK_1, StaticPerkStatus.PROVIDED);

        assertThat(staticPerkService.getPerksForUser(UID), allOf(
                hasSize(1),
                everyItem(allOf(
                        hasProperty("uid", equalTo(UID)),
                        hasProperty("perkName", equalTo(TEST_PERK_1)),
                        hasProperty("status", equalTo(StaticPerkStatus.PROVIDED))
                ))
        ));
    }

    @Test
    public void providePerkToUserTest() {
        assertThat(((YdbStaticPerkDaoStub) staticPerkDao).getTable().entrySet(), hasSize(0));

        staticPerkService.providePerkToUser(UID, TEST_PERK_1);
        staticPerkService.revokePerkFromUser(UID, TEST_PERK_2);

        var perks = staticPerkService.getPerksForUser(UID);
        assertThat(perks, hasSize(2));
        assertThat(perks, hasItem(allOf(
                hasProperty("uid", equalTo(UID)),
                hasProperty("perkName", equalTo(TEST_PERK_1)),
                hasProperty("status", equalTo(StaticPerkStatus.PROVIDED))
        )));
    }

    @Test
    public void revokePerkFromUserTest() {
        assertThat(((YdbStaticPerkDaoStub) staticPerkDao).getTable().entrySet(), hasSize(0));

        staticPerkService.revokePerkFromUser(UID, TEST_PERK_1);
        staticPerkService.providePerkToUser(UID, TEST_PERK_2);

        var perks = staticPerkService.getPerksForUser(UID);
        assertThat(perks, hasSize(2));
        assertThat(perks, hasItem(allOf(
                hasProperty("uid", equalTo(UID)),
                hasProperty("perkName", equalTo(TEST_PERK_1)),
                hasProperty("status", equalTo(StaticPerkStatus.REVOKED))
        )));
    }

    @Test
    public void updatePerkStatusIfExistsTest() {
        assertThat(((YdbStaticPerkDaoStub) staticPerkDao).getTable().entrySet(), hasSize(0));
        staticPerkService.updatePerkStatusIfExists(UID, TEST_PERK_1, StaticPerkStatus.PROVIDED);
        assertThat(((YdbStaticPerkDaoStub) staticPerkDao).getTable().entrySet(), hasSize(0));

        staticPerkService.revokePerkFromUser(UID, TEST_PERK_1);
        staticPerkService.updatePerkStatusIfExists(UID, TEST_PERK_1, StaticPerkStatus.PROVIDED);

        var perks = staticPerkService.getPerksForUser(UID);
        assertThat(perks, hasSize(1));
        assertThat(perks, hasItem(allOf(
                hasProperty("uid", equalTo(UID)),
                hasProperty("perkName", equalTo(TEST_PERK_1)),
                hasProperty("status", equalTo(StaticPerkStatus.PROVIDED))
        )));
    }

    @Test
    public void userHasPerkTest() {
        assertFalse(staticPerkService.userHasPerk(UID, TEST_PERK_1));

        staticPerkService.providePerkToUser(UID, TEST_PERK_1);
        staticPerkService.revokePerkFromUser(UID, TEST_PERK_1);

        assertTrue(staticPerkService.userHasPerk(UID, TEST_PERK_1));
    }

    @Test
    public void userHasPerkWithStatusTest() {
        assertFalse(staticPerkService.userHasPerk(UID, TEST_PERK_1));

        staticPerkService.providePerkToUser(UID, TEST_PERK_1);
        assertFalse(staticPerkService.userHasPerkWithStatus(UID, TEST_PERK_1, StaticPerkStatus.REVOKED));

        staticPerkService.updatePerkStatusIfExists(UID, TEST_PERK_1, StaticPerkStatus.REVOKED);
        assertTrue(staticPerkService.userHasPerkWithStatus(UID, TEST_PERK_1, StaticPerkStatus.REVOKED));
    }

    @Test
    public void shouldThrewExceptionOnEmptyPerkName() {
        assertThrows(MarketLoyaltyException.class, () -> staticPerkService.providePerkToUser(UID, ""));
    }
}
