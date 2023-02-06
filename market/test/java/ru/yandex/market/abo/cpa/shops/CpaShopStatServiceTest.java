package ru.yandex.market.abo.cpa.shops;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.shops.model.CpaShopSearch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CpaShopStatServiceTest extends EmptyTest {
    @Autowired
    private CpaShopStatService cpaShopStatService;

    @Test
    public void testGetShops() throws Exception {
        List<CpaShopStatInfo> infos = cpaShopStatService.getShops(
                CpaShopSearch.newBuilder()
                        .enabled(true)
                        .partnerInterface(false)
                        .limitedRegions(false)
                        .build()
        );
        assertNotNull(infos);
    }
}
