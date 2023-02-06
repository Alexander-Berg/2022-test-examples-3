package ru.yandex.market.mbo.mdm.common.masterdata.model.param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;

public class MdmParamValuePriorityTest {

    @Test
    public void testPrioritySorting() {
        Instant old = Instant.now();
        Instant recent = old.plusMillis(1);
        MdmParamValue oldLowPriority = value(MasterDataSourceType.SUPPLIER, old);
        MdmParamValue recentLowPriority = value(MasterDataSourceType.SUPPLIER, recent);
        MdmParamValue oldModeratePriority = value(MasterDataSourceType.WAREHOUSE, old);
        MdmParamValue recentModeratePriority = value(MasterDataSourceType.MDM_OPERATOR, recent);
        MdmParamValue oldHighPriority = value(MasterDataSourceType.MDM_ADMIN, old);
        MdmParamValue recentHighPriority = value(MasterDataSourceType.MDM_ADMIN, recent);
        List<MdmParamValue> values = new ArrayList<>(List.of(
            oldHighPriority,
            oldModeratePriority,
            recentLowPriority,
            oldLowPriority,
            recentHighPriority,
            recentModeratePriority
        ));
        values.sort(MdmParamValue.PRIORITY_THEN_TS_COMPARE);
        Assertions.assertThat(values).containsExactly(
            oldLowPriority,
            recentLowPriority,
            oldModeratePriority,
            recentModeratePriority,
            oldHighPriority,
            recentHighPriority
        );
    }

    private MdmParamValue value(MasterDataSourceType source, Instant ts) {
        return new MdmParamValue()
            .setMasterDataSourceType(source)
            .setUpdatedTs(ts);
    }
}
