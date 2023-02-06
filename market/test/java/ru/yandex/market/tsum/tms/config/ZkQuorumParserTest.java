package ru.yandex.market.tsum.tms.config;

import org.junit.Test;
import org.hamcrest.collection.IsMapContaining;

import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 30.03.17
 */
public class ZkQuorumParserTest {
    public static final int DEFAULT_PORT = 2181;

    @Test
    public void localhost() throws Exception {
        Map<String, Integer> map = ZkQuorumParser.getHostPortMap("localhost", DEFAULT_PORT);

        assertThat(map.size(), is(1));
        assertThat(map, IsMapContaining.hasEntry("localhost", DEFAULT_PORT));
    }

    @Test
    public void aidaCallistoDerkoat() throws Exception {
        Map<String, Integer> map = ZkQuorumParser.getHostPortMap(
            "aida.yandex.ru:2184,callisto.yandex.ru:2184,derkoat.yandex.ru:2184",
            DEFAULT_PORT
        );

        assertThat(map.size(), is(3));
        assertThat(map, IsMapContaining.hasEntry("aida.yandex.ru", 2184));
        assertThat(map, IsMapContaining.hasEntry("callisto.yandex.ru", 2184));
        assertThat(map, IsMapContaining.hasEntry("derkoat.yandex.ru", 2184));
    }

    @Test
    public void blacksmith() throws Exception {
        Map<String, Integer> map = ZkQuorumParser.getHostPortMap(
            "blacksmith01ht.market.yandex.net,blacksmith01vt.market.yandex.net,blacksmith01et.market.yandex.net",
            DEFAULT_PORT
        );

        assertThat(map.size(), is(3));
        assertThat(map, IsMapContaining.hasEntry("blacksmith01ht.market.yandex.net", DEFAULT_PORT));
        assertThat(map, IsMapContaining.hasEntry("blacksmith01vt.market.yandex.net", DEFAULT_PORT));
        assertThat(map, IsMapContaining.hasEntry("blacksmith01et.market.yandex.net", DEFAULT_PORT));
    }

    @Test
    public void ipv6() throws Exception {
        Map<String, Integer> map = ZkQuorumParser.getHostPortMap(
            "[1fff:0:a88:85a3::ac1f]:8001",
            DEFAULT_PORT
        );

        assertThat(map.size(), is(1));
        assertThat(map, IsMapContaining.hasEntry("[1fff:0:a88:85a3::ac1f]", 8001));
    }
}