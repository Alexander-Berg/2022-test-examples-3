package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.MdmSskuKeyGroup;
import ru.yandex.market.mbo.mdm.common.service.queue.ProcessSskuToRefreshQueueService;
import ru.yandex.market.mbo.mdm.common.utils.MdmDbWithCleaningTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author dmserebr
 * @date 17/11/2020
 */
public class ProcessSskuToRefreshQueueExecutorTest extends MdmDbWithCleaningTestClass {
    public static final long SEED = 1006078L;

    private final EnhancedRandom random = TestDataUtils.defaultRandom(SEED);

    @Test
    public void testParallelSubdivisionHappyPath() {
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(4),
            groupOf(8),
            groupOf(15),
            groupOf(1),
            groupOf(6),
            groupOf(2),
            groupOf(4),
            groupOf(7)
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .map(MdmSskuKeyGroup::getAll)
            .flatMap(List::stream)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        var result = ProcessSskuToRefreshQueueService.subdivideByBusinessGroups(infos, groups, 3);

        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0)).hasSize(15);
        Assertions.assertThat(result.get(1)).hasSize(16);
        Assertions.assertThat(result.get(2)).hasSize(16);
    }

    @Test
    public void testFewGroupsManyBuckets() {
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(100),
            groupOf(99),
            groupOf(12)
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .map(MdmSskuKeyGroup::getAll)
            .flatMap(List::stream)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        var result = ProcessSskuToRefreshQueueService.subdivideByBusinessGroups(infos, groups, 7);

        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0)).hasSize(12);
        Assertions.assertThat(result.get(1)).hasSize(99);
        Assertions.assertThat(result.get(2)).hasSize(100);
    }

    @Test
    public void testParallelSubdivisionGroupsMoreThanInfos() {
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(4),
            groupOf(8),
            groupOf(15),
            groupOf(1),
            groupOf(6),
            groupOf(2),
            groupOf(4),
            groupOf(7)
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .skip(3)
            .map(MdmSskuKeyGroup::getAll)
            .flatMap(List::stream)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        groups = groups.stream().skip(3).collect(Collectors.toList());

        var result = ProcessSskuToRefreshQueueService.subdivideByBusinessGroups(infos, groups, 3);

        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0)).hasSize(6);
        Assertions.assertThat(result.get(1)).hasSize(7);
        Assertions.assertThat(result.get(2)).hasSize(7);
    }

    @Test
    public void testParallelSubdivisionGroupsLessThanInfos() {
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(4),
            groupOf(8),
            groupOf(15),
            groupOf(1),
            groupOf(6),
            groupOf(2),
            groupOf(4),
            groupOf(7)
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .map(MdmSskuKeyGroup::getAll)
            .flatMap(List::stream)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        groups = groups.stream().skip(3).collect(Collectors.toList());

        var result = ProcessSskuToRefreshQueueService.subdivideByBusinessGroups(infos, groups, 3);

        Assertions.assertThat(result).hasSize(3);
        Assertions.assertThat(result.get(0)).hasSize(15);
        Assertions.assertThat(result.get(1)).hasSize(16);
        Assertions.assertThat(result.get(2)).hasSize(16);
    }

    @Test
    public void testByGroupSizeBatching() {
        // given
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(4), // 1
            groupOf(8), // 1
            groupOf(15), // 2
            groupOf(1), // 3
            groupOf(6), // 3
            groupOf(2), // 3
            groupOf(4), // 3
            groupOf(7), // 4
            groupOf(8), // 4
            groupOf(10), // 5
            groupOf(15) // 6
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .map(MdmSskuKeyGroup::getAll)
            .flatMap(List::stream)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        // when
        List<List<SskuToRefreshInfo>> batches = ProcessSskuToRefreshQueueService.splitIntoBatches(infos, groups, 15);

        Assertions.assertThat(batches)
            .map(List::size)
            .containsExactly(
                12,
                15,
                13,
                15,
                10,
                15
            );
    }

    @Test
    public void whenThereOnlyOneInfoFromGroupUseAllGroupSize() {
        // given
        List<MdmSskuKeyGroup> groups = List.of(
            groupOf(4), // 1
            groupOf(8), // 1
            groupOf(15), // 2
            groupOf(1), // 3
            groupOf(6), // 3
            groupOf(2), // 3
            groupOf(4), // 3
            groupOf(7), // 4
            groupOf(8), // 4
            groupOf(10), // 5
            groupOf(15) // 6
        );
        List<SskuToRefreshInfo> infos = groups.stream()
            .map(MdmSskuKeyGroup::getRootKey)
            .map(ProcessSskuToRefreshQueueExecutorTest::info)
            .collect(Collectors.toList());

        // when
        List<List<SskuToRefreshInfo>> batches = ProcessSskuToRefreshQueueService.splitIntoBatches(infos, groups, 15);

        Assertions.assertThat(batches)
            .map(List::size)
            .containsExactly(
                2,
                1,
                4,
                2,
                1,
                1
            );
    }

    private static SskuToRefreshInfo info(ShopSkuKey key, MdmEnqueueReason... reasons) {
        var info = new SskuToRefreshInfo();
        info.setEntityKey(key);
        for (MdmEnqueueReason reason : reasons) {
            info.addRefreshReason(reason);
        }
        return info;
    }

    private static SskuSilverParamValue createSskuSilverParamValue(ShopSkuKey key, long paramId, String strValue,
                                                                   Long masterDataVersion) {
        var paramValue = new SskuSilverParamValue();
        paramValue.setShopSkuKey(key);
        paramValue.setMasterDataSourceId(String.valueOf(key.getSupplierId()));
        paramValue.setMasterDataSourceType(MasterDataSourceType.SUPPLIER);
        paramValue.setMdmParamId(paramId);
        paramValue.setString(strValue);
        paramValue.setDatacampMasterDataVersion(masterDataVersion);
        return paramValue;
    }

    private static SskuSilverParamValue createWrongSilverParamValue(ShopSkuKey key, long paramId, String strValue,
                                                                    Long masterDataVersion) {
        var paramValue = new SskuSilverParamValue();
        paramValue.setShopSkuKey(key);
        paramValue.setMasterDataSourceId(String.valueOf(key.getSupplierId()));
        paramValue.setMasterDataSourceType(MasterDataSourceType.WAREHOUSE);
        paramValue.setMdmParamId(paramId);
        paramValue.setString(strValue);
        paramValue.setDatacampMasterDataVersion(masterDataVersion);
        return paramValue;
    }

    private MdmSskuKeyGroup groupOf(int count) {
        if (count == 1) {
            return MdmSskuKeyGroup.createNoBusinessGroup(random.nextObject(ShopSkuKey.class));
        } else {
            List<ShopSkuKey> keys = new ArrayList<>();
            for (int i = 0; i < count - 1; i++) {
                keys.add(random.nextObject(ShopSkuKey.class));
            }
            return MdmSskuKeyGroup.createBusinessGroup(random.nextObject(ShopSkuKey.class), keys);
        }
    }
}
