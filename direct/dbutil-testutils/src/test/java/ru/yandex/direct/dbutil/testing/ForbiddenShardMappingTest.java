package ru.yandex.direct.dbutil.testing;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbutil.QueryWithForbiddenShardMapping;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardKey;

@DbUtilTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ForbiddenShardMappingTest {

    public static final int ID = 123456789;

    @Autowired
    private ShardHelper shardHelper;

    @Test(expected = QueryForbiddenShardKeyException.class)
    public void testForbiddenKey_Fails() {
        shardHelper.groupByShard(List.of(ID), ShardKey.BID);
    }

    @Test
    @QueryWithForbiddenShardMapping("Тестирование аннотации")
    public void testAnnotatedForbiddenKey_Success() {
        shardHelper.groupByShard(List.of(ID), ShardKey.BID);
    }

    @Test
    public void testAllowedKey_Succeeds() {
        shardHelper.groupByShard(List.of(ID), ShardKey.UID);
    }

}
