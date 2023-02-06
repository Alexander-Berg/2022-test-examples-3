package ru.yandex.market.pricelabs.tms.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.CachedShopContent;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.DataSourceContent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.search.matcher.PatternMatching.matcher;
import static ru.yandex.market.pricelabs.search.matcher.PatternMatching.normalize;

@Slf4j
class CachedShopFeedDataTest {

    private static final int SHOP_ID = 1;
    private static final int FEED_ID = 2;
    private static final int REGION_ID = 3;

    private CachedShopContent content;
    private DataSourceContent dsContent;

    private CachedShopFeedData cachedShopFeedData;

    @BeforeEach
    void init() {
        this.content = TmsTestUtils.getCachedShopContent(SHOP_ID, FEED_ID, REGION_ID);
        this.dsContent = content.dsContent;

        this.initShopData();
    }

    @Test
    void isMatched() {
        var sS = matcher("s");
        var sL = matcher("S");
        var xL = matcher("X");

        log.info("sS = {}", sS);
        log.info("sL = {}", sL);
        log.info("xL = {}", xL);

        assertTrue(sS.isMatched(normalize("S1")));
        assertTrue(sL.isMatched(normalize("s1")));
        assertTrue(sL.isMatched(normalize("S1")));
        assertTrue(sS.isMatched(normalize("s1")));
        assertFalse(xL.isMatched(normalize("S1")));
    }

    private void initShopData() {
        this.cachedShopFeedData = content.newShopData();
    }
}
