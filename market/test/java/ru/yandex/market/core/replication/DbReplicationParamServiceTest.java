package ru.yandex.market.core.replication;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "DbReplicationParamServiceTest.common.before.csv")
class DbReplicationParamServiceTest extends FunctionalTest {

    public static final long ADD_ACTION_ID = 101L;

    public static final long DELETE_ACTION_ID = 102L;

    @Autowired
    ReplicationParamService replicationParamService;

    @Test
    @DisplayName("Добавление параметров для репликации магазинов")
    @DbUnitDataSet(after = "DbReplicationParamServiceTest.after.csv")
    void addParams() {
        List<Long> shopIds = new ArrayList<>();
        shopIds.add(1L);
        shopIds.add(2L);
        replicationParamService.add(shopIds, ReplicationParam.SKIP_FEED, ADD_ACTION_ID);
        shopIds.add(3L);
        replicationParamService.add(shopIds, ReplicationParam.SKIP_MODERATION, ADD_ACTION_ID);
    }

    @Test
    @DisplayName("Удаление параметров для репликации магазинов по имени параметра")
    @DbUnitDataSet(before = "DbReplicationParamServiceTest.before.csv", after =
            "DbReplicationParamServiceTestDelete.after.csv")
    void deleteByParam() {
        List<Long> shopIds = List.of(1L, 2L);
        replicationParamService.deleteByParameter(shopIds, ReplicationParam.SKIP_MODERATION, DELETE_ACTION_ID);
        replicationParamService.deleteByParameter(shopIds, ReplicationParam.SKIP_FEED, DELETE_ACTION_ID);
    }

    @Test
    @DisplayName("Получение параметров по айди магазина")
    @DbUnitDataSet(before = "DbReplicationParamServiceTest.before.csv")
    void getReplicationParam() {
        EnumSet<ReplicationParam> result = replicationParamService.getReplicationParams(2);
        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains(ReplicationParam.SKIP_FEED));
        Assertions.assertTrue(result.contains(ReplicationParam.SKIP_MODERATION));
    }
}
