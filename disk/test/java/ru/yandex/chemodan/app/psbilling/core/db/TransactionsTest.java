package ru.yandex.chemodan.app.psbilling.core.db;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserInfoDao;
import ru.yandex.chemodan.app.psbilling.core.users.UserInfoService;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.masterSlave.MasterSlaveContextHolder;
import ru.yandex.misc.db.masterSlave.MasterSlavePolicy;
import ru.yandex.misc.db.masterSlave.MasterSlaveUnitUnavailableException;

public class TransactionsTest  extends AbstractPsBillingCoreTest {
    @Autowired
    private UserInfoDao userInfoDao;

    @Autowired
    private UserInfoService userInfoService;


    @Test(expected = MasterSlaveUnitUnavailableException.class)
    public void testUpdateFailOnSlavePolicy(){
        MasterSlaveContextHolder.withPolicy(MasterSlavePolicy.R_S, ()-> {
            userInfoDao.createOrUpdate(UserInfoDao.InsertData.builder()
                    .uid(PassportUid.cons(123)).regionId(Option.of("225")).build());
        });
    }

    @Test
    public void testAnnotationOverridePolicy(){
        MasterSlaveContextHolder.withPolicy(MasterSlavePolicy.R_S, ()-> {
            userInfoService.findOrCreateUserInfo(PassportUid.cons(3000185708L));
        });
    }
}
