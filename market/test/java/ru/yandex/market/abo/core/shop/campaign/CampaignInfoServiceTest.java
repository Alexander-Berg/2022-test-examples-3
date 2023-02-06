package ru.yandex.market.abo.core.shop.campaign;

import java.util.Arrays;

import com.google.common.collect.BiMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author antipov93.
 * @date 19.12.18.
 */
public class CampaignInfoServiceTest extends EmptyTest {

    @Autowired
    private CampaignInfoService campaignInfoService;

    @Autowired
    private CampaignInfoRepo campaignInfoRepo;

    @BeforeEach
    void setUp() {
        campaignInfoRepo.saveAll(Arrays.asList(
                create(1, 1000),
                create(2, 2000),
                create(3, 3000)
        ));
    }

    @Test
    void testAllShopsHaveCampaign() {
        BiMap<Long, Long> shopToCampaign = campaignInfoService.loadShopCampaignMap(Arrays.asList(1L, 2L));
        assertEquals(2, shopToCampaign.size());
        assertEquals(1000, shopToCampaign.get(1L).longValue());
        assertEquals(2000, shopToCampaign.get(2L).longValue());
    }

    @Test
    void testOneShopHasNoCampaign() {
        BiMap<Long, Long> shopToCampaign = campaignInfoService.loadShopCampaignMap(Arrays.asList(1L, 4L));
        assertEquals(1, shopToCampaign.size());
        assertFalse(shopToCampaign.containsKey(4L));
    }

    @Test
    void testTwoShopsHaveSameCampaign() {
        campaignInfoRepo.save(create(4, 3000));
        assertThrows(IllegalArgumentException.class,
                () -> campaignInfoService.loadShopCampaignMap(Arrays.asList(3L, 4L)));
    }

    private static CampaignInfo create(long shopId, long campaignId) {
        CampaignInfo info = new CampaignInfo();
        info.setShopId(shopId);
        info.setCampaignId(campaignId);
        return info;
    }
}
