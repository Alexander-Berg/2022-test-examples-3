package ru.yandex.market.psku.postprocessor.common.db.dao;

import java.sql.Timestamp;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuConvertStatus;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.ConvertedPskuGroup;

public class ConvertedPskuGroupDaoTest extends BaseDBTest {

    @Autowired
    ConvertedPskuGroupDao dao;

    @Test
    public void insertWithCurrentTimeStamp() {
        ConvertedPskuGroup group1 = makeGroup(10, 1, PskuConvertStatus.FAILED, "ex");
        ConvertedPskuGroup group2 = makeGroup(11, 3, PskuConvertStatus.OK, null);
        ConvertedPskuGroup group3 = makeGroup(12, 3, PskuConvertStatus.OK, null);
        ConvertedPskuGroup group4 = makeGroup(13, 4, PskuConvertStatus.FAILED, "some");
        // insert first 2
        dao.insert(group1, group2);
        // change first 2
        group1.setConvertStatus(PskuConvertStatus.OK);
        group1.setFailedReason(null);
        group1.setGroupId(100);
        group2.setFailedReason("updated");
        group2.setGroupId(200);

        // now insert with update all
        dao.insertOrUpdateWithCurrentTimeStamp(ImmutableList.of(group1, group2, group3, group4));

        Map<Long, ConvertedPskuGroup> pskuIdToConvertedGroup = dao.fetchByPskuId(10L, 11L, 12L, 13L).stream()
            .collect(Collectors.toMap(ConvertedPskuGroup::getPskuId, Function.identity()));

        Assertions.assertThat(pskuIdToConvertedGroup.keySet()).containsExactlyInAnyOrder(10L, 11L, 12L, 13L);

        // check updated
        Assertions.assertThat(pskuIdToConvertedGroup.get(group1.getPskuId()))
            .isEqualToIgnoringGivenFields(group1, "convertedOnTs");
        Assertions.assertThat(pskuIdToConvertedGroup.get(group2.getPskuId()))
            .isEqualToIgnoringGivenFields(group2, "convertedOnTs");

        // check inserted
        Assertions.assertThat(pskuIdToConvertedGroup.get(group3.getPskuId()))
            .isEqualToIgnoringGivenFields(group3, "convertedOnTs");
        Assertions.assertThat(pskuIdToConvertedGroup.get(group4.getPskuId()))
            .isEqualToIgnoringGivenFields(group4, "convertedOnTs");
    }

    private static ConvertedPskuGroup makeGroup(long pskuId, int groupId,
                                                PskuConvertStatus convertStatus, String failedReason) {
        ConvertedPskuGroup group = new ConvertedPskuGroup();
        group.setPskuId(pskuId);
        group.setGroupId(groupId);
        group.setOfferId(null);
        group.setBusinessId(null);
        group.setConvertStatus(convertStatus);
        group.setFailedReason(failedReason);
        group.setConvertedOnTs(new Timestamp(System.currentTimeMillis()));
        return group;
    }

}
