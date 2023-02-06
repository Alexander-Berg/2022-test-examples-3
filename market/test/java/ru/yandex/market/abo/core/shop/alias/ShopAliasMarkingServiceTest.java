package ru.yandex.market.abo.core.shop.alias;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarking;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarkingStatus;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarkingTask;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarkingTaskStatus;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasMarkingVerdict;
import ru.yandex.market.abo.core.shop.alias.model.ShopAliasTaskSourceType;
import ru.yandex.market.abo.core.shop.alias.repo.ShopAliasMarkingRepo;
import ru.yandex.market.abo.core.shop.alias.repo.ShopAliasMarkingTaskRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.09.2020
 */
class ShopAliasMarkingServiceTest extends EmptyTest {

    private static final long SHOP_ID = 123L;
    private static final String ALIAS = "holodilnik.ru";
    private static final long SOURCE_ID = 321523456L;
    private static final long USER_ID = 12335L;

    @Autowired
    private ShopAliasMarkingService shopAliasMarkingService;
    @Autowired
    private ShopAliasMarkingRepo shopAliasMarkingRepo;
    @Autowired
    private ShopAliasMarkingTaskRepo shopAliasMarkingTaskRepo;

    @Test
    void testMarkingTaskCreation() {
        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        var savedMarking = shopAliasMarkingRepo.findAllByShopIdAndStatus(SHOP_ID, ShopAliasMarkingStatus.IN_PROGRESS).get(0);

        assertEquals(SHOP_ID, savedMarking.getShopId());
        assertEquals(ALIAS, savedMarking.getAlias());
        assertFalse(savedMarking.getTasks().isEmpty());
        assertEquals(savedMarking.getTasks().get(0).getSourceId(), SOURCE_ID);
        assertEquals(savedMarking.getTasks().get(0).getSourceType(), ShopAliasTaskSourceType.PREMOD_TICKET);
    }

    @Test
    void testMarkingTaskCreation__markingAlreadyExists() {
        shopAliasMarkingRepo.save(new ShopAliasMarking(SHOP_ID, ALIAS));
        flushAndClear();

        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        assertEquals(1, shopAliasMarkingRepo.count());
    }

    @Test
    void testMarkingTaskCreation__markingTaskAlreadyExists() {
        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);
        flushAndClear();

        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);

        assertEquals(1, shopAliasMarkingTaskRepo.count());
    }

    @Test
    void makeVerdictTest() {
        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.PREMOD_TICKET);
        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.CORE_TICKET);
        shopAliasMarkingService.createAliasMarkingTaskIfNotExists(SHOP_ID, ALIAS, SOURCE_ID, ShopAliasTaskSourceType.RECHECK_TICKET);
        flushAndClear();

        var markingBeforeVerdict = shopAliasMarkingRepo.findAllByShopIdAndStatus(SHOP_ID, ShopAliasMarkingStatus.IN_PROGRESS).get(0);
        shopAliasMarkingService.makeVerdictAboutAlias(
                markingBeforeVerdict.getTasks().get(0).getId(), ShopAliasMarkingVerdict.ASSOCIATED_WITH_SHOP, USER_ID
        );

        var markingAfterFirstVerdict = shopAliasMarkingRepo.findAllByShopIdAndStatus(SHOP_ID, ShopAliasMarkingStatus.IN_PROGRESS).get(0);
        assertEquals(ShopAliasMarkingVerdict.UNDEFINED, markingAfterFirstVerdict.getVerdict());
        assertEquals(ShopAliasMarkingStatus.IN_PROGRESS, markingAfterFirstVerdict.getStatus());

        shopAliasMarkingService.makeVerdictAboutAlias(
                markingBeforeVerdict.getTasks().get(1).getId(), ShopAliasMarkingVerdict.ASSOCIATED_WITH_SHOP, USER_ID
        );

        var markingAfterSecondVerdict = shopAliasMarkingRepo.findAllByShopIdAndStatus(SHOP_ID, ShopAliasMarkingStatus.FINISHED).get(0);
        assertEquals(ShopAliasMarkingVerdict.ASSOCIATED_WITH_SHOP, markingAfterSecondVerdict.getVerdict());
        assertEquals(1,
                StreamEx.of(markingAfterSecondVerdict.getTasks())
                        .filterBy(ShopAliasMarkingTask::getStatus, ShopAliasMarkingTaskStatus.CANCELLED).count()
        );
    }
}
