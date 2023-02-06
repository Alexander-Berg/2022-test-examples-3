package ru.yandex.direct.dbutil.testing;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@DbUtilTest
@RunWith(SpringJUnit4ClassRunner.class)
public class JooqExplainExecuteListenerTest {

    public static final long ID = 123456789L;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Test(expected = QueryWithoutIndexException.class)
    public void heavyQuery_Fails() {
        dslContextProvider.ppc(1)
                .select(CAMPAIGNS.CID)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.TYPE.eq(CampaignsType.billing_aggregate))
                .fetch().intoArray(CAMPAIGNS.CID);
    }

    @Test
    public void usualQuery_Succeeds() {
        dslContextProvider.ppc(1)
                .select(CAMPAIGNS.CID)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(ID))
                .fetch().intoArray(CAMPAIGNS.CID);
    }

    @Test
    @QueryWithoutIndex("Тестирование JooqExplainExecuteListener")
    public void annotatedHeavyQuery_Succeeds() {
        dslContextProvider.ppc(1)
                .select(CAMPAIGNS.CID)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.TYPE.eq(CampaignsType.billing_aggregate))
                .fetch().intoArray(CAMPAIGNS.CID);
    }

}
