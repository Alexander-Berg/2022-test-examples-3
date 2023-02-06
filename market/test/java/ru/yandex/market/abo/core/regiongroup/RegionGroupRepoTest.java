package ru.yandex.market.abo.core.regiongroup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroup;
import ru.yandex.market.abo.core.regiongroup.model.AboRegionGroupStatus;
import ru.yandex.market.abo.core.regiongroup.service.RegionGroupRepo;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author agavrikov
 * @date 20.03.18
 */
class RegionGroupRepoTest extends EmptyTest {
    private static final long SHOP_ID_1 = 774L;
    private static final long SHOP_ID_2 = 10233862L;

    @Autowired
    RegionGroupRepo regionGroupRepo;

    @Test
    void testRepo() {
        AboRegionGroup regionGroup = initRegionGroup();
        regionGroupRepo.save(regionGroup);
        AboRegionGroup dbRegionGroup = regionGroupRepo.findByIdOrNull(regionGroup.getTarifficatorId());
        assertEquals(regionGroup, dbRegionGroup);
    }

    @Test
    void testUpdateStatus() {
        List<AboRegionGroup> regionGroups = initRegionGroupList();
        regionGroupRepo.saveAll(regionGroups);
        regionGroupRepo.updateStatusByTariff(AboRegionGroupStatus.FAIL, List.of(1L));
        flushAndClear();
        AboRegionGroup dbRegionGroup = regionGroupRepo.findByIdOrNull(1L);
        assertEquals(AboRegionGroupStatus.FAIL, dbRegionGroup.getRegionGroupStatus());

        var cancelledIds = List.of(1L, 3L);
        regionGroupRepo.updateStatusByTariff(AboRegionGroupStatus.CANCELLED, cancelledIds);
        flushAndClear();
        assertEquals(
                Set.copyOf(cancelledIds),
                StreamEx.of(regionGroupRepo.findAll())
                        .filter(group -> group.getRegionGroupStatus() == AboRegionGroupStatus.CANCELLED)
                        .map(AboRegionGroup::getTarifficatorId)
                        .toSet()
        );
    }

    @Test
    void testUpdateRegionGroupTicket() {
        List<AboRegionGroup> regionGroups = initRegionGroupList();
        regionGroupRepo.saveAll(regionGroups);
        List<Long> regionGroupIds = regionGroups.stream().map(AboRegionGroup::getTarifficatorId).collect(toList());
        List<AboRegionGroup> dbRegionGroups = regionGroupRepo.findAllById(regionGroupIds);
        for (AboRegionGroup dbRegionGroup : dbRegionGroups) {
            dbRegionGroup.setTicketId(1L);
            dbRegionGroup.setRegionGroupStatus(AboRegionGroupStatus.IN_PROGRESS);
        }
        regionGroupRepo.saveAll(dbRegionGroups);
        List<AboRegionGroup> updateRegionGroups = regionGroupRepo.findAllById(regionGroupIds);
        assertEquals(dbRegionGroups, updateRegionGroups);
    }

    @Test
    void testFindAllByShopIdIn() {
        List<AboRegionGroup> regionGroups = initRegionGroupList();
        regionGroupRepo.saveAll(regionGroups);
        Set<Long> ticketShops = regionGroupRepo.findTicketShops();
        assertEquals(Stream.of(SHOP_ID_1, SHOP_ID_2).collect(toSet()), ticketShops);
    }

    private AboRegionGroup initRegionGroup() {
        return new AboRegionGroup(1L, SHOP_ID_1, "TestRegionGroup1", false, new Long[]{1L, 2L});
    }

    private List<AboRegionGroup> initRegionGroupList() {
        List<AboRegionGroup> regionGroupList = new ArrayList<>();

        regionGroupList.add(new AboRegionGroup(1L, SHOP_ID_1, "TestRegionGroup1", true, new Long[]{1L, 2L}));
        regionGroupList.add(new AboRegionGroup(2L, SHOP_ID_1, "TestRegionGroup2", false, new Long[]{1L, 2L}));
        regionGroupList.add(createRegionGroup(3L, SHOP_ID_1, "TestRegionGroup3",
                new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))));
        regionGroupList.add(createRegionGroup(4L, SHOP_ID_1, "TestRegionGroup4",
                new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3))));
        regionGroupList.add(new AboRegionGroup(5L, SHOP_ID_2, "TestRegionGroup5", false, new Long[]{1L, 2L}));
        regionGroupList.add(createRegionGroup(6L, SHOP_ID_2, "TestRegionGroup6",
                new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1))));
        regionGroupList.add(createRegionGroup(7L, SHOP_ID_2, "TestRegionGroup7",
                new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(5))));

        return regionGroupList;
    }


    private static AboRegionGroup createRegionGroup(long id, long shopId, String name, Date creationTime) {
        AboRegionGroup regionGroup = new AboRegionGroup(id, shopId, name, false, new Long[]{1L, 2L});
        regionGroup.setCreationTime(creationTime);
        regionGroup.setModificationTime(creationTime);
        return regionGroup;
    }
}
