package ru.yandex.market.hrms.tms.executor.jobstate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

@DbUnitDataSet(before = "JobStateTest.before.csv")
public class JobStateTest extends AbstractTmsTest {

    @Autowired
    private JobStateTestExecutor executor;

    @Test
    @DbUnitDataSet(after = "JobStateTestShouldExecute.after.csv")
    public void jobShouldBeExecutedIsNoState() {
        executor.doRealJob(null);
    }

    @Test
    @DbUnitDataSet(before = "JobStateTestWithState.before.csv",
            after = "JobStateTestWithState.before.csv")
    public void jobShouldNotBeExecutedIfDisabled() {
        executor.doRealJob(null);
    }
}

