package ru.yandex.market.abo.core.pinger.yt;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author artemmz
 * @date 22/01/19.
 */
class YtUrlLoaderDbTest extends EmptyTest {
    private static final long SHOP_ID = 774L;
    private static final String URL = "url";
    private static final long GEN_ID = 202201250234L;

    @Autowired
    private YtUrlLoader ytUrlLoader;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("insert into shop (id, is_offline) values(?, false)", SHOP_ID);
        PingerUrl url = new PingerUrl(SHOP_ID, URL, "ware");
        url.setGeneration(GEN_ID);
        ytUrlLoader.flush(IdxCluster.MARKET, Lists.newArrayList(url));
    }

    @Test
    void merge() {
        assertEquals(GEN_ID, ytUrlLoader.lastPingerDbGen(IdxCluster.MARKET));

        long newGen = GEN_ID + 1;
        PingerUrl url = new PingerUrl(SHOP_ID, URL, "ware");
        url.setGeneration(newGen);
        ytUrlLoader.flush(IdxCluster.MARKET, Lists.newArrayList(url));

        long lastPingerDbGen = ytUrlLoader.lastPingerDbGen(IdxCluster.MARKET);
        assertEquals(newGen, lastPingerDbGen);
        assertFalse(hasOldGenEntries());
    }

    @Test
    void doNotRemoveAll() {
        ytUrlLoader.removeOld(IdxCluster.MARKET, GEN_ID + 100);
        assertEquals(GEN_ID, ytUrlLoader.lastPingerDbGen(IdxCluster.MARKET));
        assertTrue(hasOldGenEntries());
    }

    @Test
    void remove() {
        long newGen = GEN_ID + 1;
        PingerUrl url = new PingerUrl(SHOP_ID, "other" + URL, "ware");
        url.setGeneration(newGen);
        ytUrlLoader.flush(IdxCluster.MARKET, Lists.newArrayList(url));
        long lastPingerDbGen = ytUrlLoader.lastPingerDbGen(IdxCluster.MARKET);
        assertEquals(newGen, lastPingerDbGen);
        assertTrue(hasOldGenEntries());

        ytUrlLoader.removeOld(IdxCluster.MARKET, newGen);
        assertEquals(newGen, lastPingerDbGen);
        assertFalse(hasOldGenEntries());
    }

    private Boolean hasOldGenEntries() {
        return jdbcTemplate.queryForObject("select count(*) > 0 from mp_url where generation_id = ?",
                Boolean.class, GEN_ID);
    }
}
