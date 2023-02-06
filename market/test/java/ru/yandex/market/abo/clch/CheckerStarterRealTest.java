package ru.yandex.market.abo.clch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.clch.checker.CheckerDescriptor;
import ru.yandex.market.abo.clch.checker.CheckerLoader;
import ru.yandex.market.abo.clch.model.TimeLimits;
import ru.yandex.market.abo.core.shopdata.ShopDataService;
import ru.yandex.market.abo.core.shopdata.model.ShopData;
import ru.yandex.market.abo.core.shopdata.model.ShopDataType;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Olga Bolshakova
 * @date 06.11.2007
 * Time: 18:10:16
 */
class CheckerStarterRealTest extends ClchTest {

    private static final ClchSourceWithUser CLCH_SOURCE = ClchSessionSource.WHITE_PREMOD;
    private static final Set<Long> SHOP_IDS = new HashSet<>(Arrays.asList(303L, 705L, 801L, 310L));

    @Autowired
    private CheckerStarter checkerStarter;
    @Autowired
    private CheckerAnalyzer checkerAnalyzer;
    @Autowired
    private ClchService clchService;
    @Autowired
    private CheckerManager checkerManager;
    @Autowired
    private ShopDataService shopDataService;
    @Autowired
    private CheckerLoader checkerLoader;

    @Test
    void testAnalyzer() {
        long shopSetId = checkerManager.getOrCreateShopSet(SHOP_IDS);
        long session = clchService.saveSession(CLCH_SOURCE, shopSetId, TimeLimits.getDefault());
        checkerAnalyzer.analyzeResults(session);
    }

    @Test
    void testStart() {
        checkerStarter.startCheckers(SHOP_IDS, CLCH_SOURCE);
    }

    /**
     * can't test checker queries via {@link CheckerStarter#startCheckers(Set, ClchSourceWithUser)} cause there checkers are
     * created from app context and for some reason can't see changes in db =(.
     */
    @Test
    void testCheckerQueries() {
        List<ShopData> shopData = SHOP_IDS.stream().flatMap(shopId ->
                Arrays.stream(ShopDataType.values()).map(type -> new ShopData(shopId, shopId + type.name(), type))
        ).collect(Collectors.toList());
        shopDataService.insertNewValues(shopData);

        checkerLoader.loadCheckers().forEach(ch -> {
            ch.configure(new CheckerDescriptor(1, ""));
            ch.warmUpCache(SHOP_IDS);
            Lists.partition(new ArrayList<>(SHOP_IDS), 2).forEach(shopPair ->
                    assertNotNull(ch.checkShops(shopPair.get(0), shopPair.get(1))));
            ch.done();
        });
    }

}
