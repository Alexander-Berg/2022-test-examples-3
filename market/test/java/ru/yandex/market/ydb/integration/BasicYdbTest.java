package ru.yandex.market.ydb.integration;

import com.yandex.ydb.core.Result;
import com.yandex.ydb.table.Session;
import com.yandex.ydb.table.query.DataQueryResult;
import com.yandex.ydb.table.result.ResultSetReader;
import com.yandex.ydb.table.transaction.TxControl;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ydb.integration.initialization.TableClientFactory;
import ru.yandex.market.ydb.integration.ServiceTestBase;

public class BasicYdbTest extends ServiceTestBase {

    @Autowired
    protected TableClientFactory ydbClientFactory;

    @Test
    public void basicYdbRecipeTest() {
        Session session = ydbClientFactory.client().createSession()
                .join()
                .expect("cannot create session");
        TxControl<?> txControl = TxControl.serializableRw().setCommitTx(true);
        Result<DataQueryResult> result = session.executeDataQuery("select 1;", txControl)
                .join();
        Assert.assertTrue(result.isSuccess());
        DataQueryResult dqresult = result
                .expect("cannot get result");
        ResultSetReader resultSet = dqresult.getResultSet(0);
        Assert.assertTrue(resultSet.next());
        long one = resultSet.getColumn(0).getInt32();
        Assert.assertEquals(1, one);
    }
}
