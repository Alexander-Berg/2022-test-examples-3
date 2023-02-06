package ru.yandex.market.tsum.clients.gencfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 08/08/2017
 */
public class GenCfgUtilsTest {
    @Test
    public void getGroupName() throws Exception {
        assertEquals(
            "SAS_MARKET_TEST_MY_SERVICE",
            GenCfgUtils.getGroupName("my_service", GenCfgLocation.SAS, GenCfgCType.TESTING)
        );
        assertEquals(
            "VLA_MARKET_PREP_MY_SERVICE",
            GenCfgUtils.getGroupName("my_service", GenCfgLocation.VLA, GenCfgCType.PRESTABLE)
        );
        assertEquals(
            "IVA_MARKET_PROD_MY_SERVICE",
            GenCfgUtils.getGroupName("my_service", GenCfgLocation.IVA, GenCfgCType.PRODUCTION)
        );
    }
}
