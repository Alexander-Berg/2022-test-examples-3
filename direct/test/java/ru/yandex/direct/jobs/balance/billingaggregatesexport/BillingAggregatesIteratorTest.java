package ru.yandex.direct.jobs.balance.billingaggregatesexport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.test.utils.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;

@JobsTest
@ExtendWith(SpringExtension.class)
class BillingAggregatesIteratorTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardSupport shardSupport;

    @Autowired
    private Steps steps;

    private int shardCount;

    @BeforeEach
    void setUp() {
        shardCount = shardSupport.getAvailablePpcShards().size();
        TestUtils.assumeThat("В тестовой базе не менее двух шардов", shardCount, is(greaterThanOrEqualTo(2)));

        addBillingAggregatesToShard(1, 2);
        addBillingAggregatesToShard(2, 3);
    }

    @Test
    void testIteratorWithSmallChunks() {
        List<Long> ids = getIdsByIterator(1);
        List<Long> idsFromDb = getIdsDirectly();

        assertThat("Выбраны все id биллинговых агрегатов", ids, is(equalTo(idsFromDb)));
    }

    @Test
    void testIteratorWithBigChunks() {
        List<Long> ids = getIdsByIterator(100_000);
        List<Long> idsFromDb = getIdsDirectly();

        assertThat("Выбраны все id биллинговых агрегатов", ids, is(equalTo(idsFromDb)));
    }

    private List<Long> getIdsByIterator(int chunkSize) {
        BillingAggregatesUploader.BillingAggregatesIterator it =
                new BillingAggregatesUploader.BillingAggregatesIterator(dslContextProvider, shardCount, null);
        it.setChunkSize(chunkSize);

        List<Long> ids = new ArrayList<>();
        while (it.hasNext()) {
            ids.add(it.next().cid);
        }

        return ids;
    }

    private void addBillingAggregatesToShard(int shard, int count) {
        IntStream.range(0, count).forEach(n -> {
            ClientInfo client = steps.clientSteps().createClient(new ClientInfo().withShard(shard));
            CampaignInfo camp = steps.campaignSteps().createActiveCampaignUnderWallet(client);
            steps.campaignSteps().createBillingAggregate(client, camp.getCampaign().getBalanceInfo());
        });
    }

    @QueryWithoutIndex("Тест")
    private List<Long> getIdsDirectly() {
        List<Long> ids = new ArrayList<>();
        IntStream.rangeClosed(1, shardCount).forEach(shardId -> {
            Long[] shardIds = dslContextProvider.ppc(shardId)
                    .select(CAMPAIGNS.CID)
                    .from(CAMPAIGNS)
                    .where(CAMPAIGNS.TYPE.eq(CampaignsType.billing_aggregate))
                    .fetch().intoArray(CAMPAIGNS.CID);
            ids.addAll(Arrays.asList(shardIds));
        });

        return ids;
    }
}
