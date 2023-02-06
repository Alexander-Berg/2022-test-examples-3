package ru.yandex.market.abo.core.regiongroup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailureReason;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupFailureReasonType;
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupFailureReasonRepo;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 24.10.18
 */
public class RegionGroupFailureReasonRepoTest extends EmptyTest {

    private static final Long SHOP_ID = 774L;

    @Autowired
    RegionGroupFailureReasonRepo regionGroupFailureReasonRepo;

    @Test
    public void testRepo() throws Exception {
        AboRegionGroupFailureReason regionGroupFailureReason = initRegionGroupFailureReason();
        regionGroupFailureReasonRepo.save(regionGroupFailureReason);
        AboRegionGroupFailureReason dbRegionGroupFailureReason = regionGroupFailureReasonRepo.findByIdOrNull(1L);
        assertEquals(1L, dbRegionGroupFailureReason.getRegionGroupTarifficatorId().longValue());
        assertEquals(AboRegionGroupFailureReasonType.COURIER_CARD, dbRegionGroupFailureReason.getFailureReason());
    }

    private AboRegionGroupFailureReason initRegionGroupFailureReason() {
        return new AboRegionGroupFailureReason(1L, AboRegionGroupFailureReasonType.COURIER_CARD, SHOP_ID, null);
    }
}
