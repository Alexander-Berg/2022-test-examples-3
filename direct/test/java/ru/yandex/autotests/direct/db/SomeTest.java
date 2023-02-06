package ru.yandex.autotests.direct.db;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.autotests.direct.db.steps.DirectJooqDbSteps;
import ru.yandex.autotests.direct.utils.config.StageType;

@Ignore
public class SomeTest {
    DirectJooqDbSteps dbSteps;

    @Before
    public void init() {
        dbSteps = new DirectJooqDbSteps(StageType.TC, 0);
    }

    @Test
    public void someTestSteps() {
        dbSteps.useShardForLogin("asdf");
        dbSteps.someSteps().getUserFromShard();
        dbSteps.someSteps().updateUser();

    }

    @Test
    public void testShard() {
        //1
        System.out.println(dbSteps.shardingSteps().getShardByCid(8633435L));
        //2
        System.out.println(dbSteps.shardingSteps().getShardByCid(5469796L));

        //1
        System.out.println(dbSteps.shardingSteps().getShardByClientID(2283785L));
        //2
        System.out.println(dbSteps.shardingSteps().getShardByClientID(545237L));

        //1
        System.out.println(dbSteps.shardingSteps().getShardByPid(258474694L));
        //2
        System.out.println(dbSteps.shardingSteps().getShardByPid(10661435L));

        //1
        System.out.println(dbSteps.shardingSteps().getShardByOrderId(3073759L));
        //2
        System.out.println(dbSteps.shardingSteps().getShardByOrderId(1339700L));

        //1
        System.out.println(dbSteps.shardingSteps().getShardByCidRaw(8633435L));
        System.out.println(dbSteps.shardingSteps().getShardByClientIDRaw(2283785L));
    }

    @Test
    public void baTest() {
        dbSteps.someSteps().callouts(1);
        dbSteps.bannerAdditionsSteps().clearCalloutsForClient(2283785L);
    }
}
