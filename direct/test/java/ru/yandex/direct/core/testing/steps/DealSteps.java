package ru.yandex.direct.core.testing.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.deal.container.CampaignDeal;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.repository.TestDealRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * todo javadoc
 */
public class DealSteps {
    private final DatabaseWrapperProvider databaseWrapperProvider;
    private final DealRepository dealRepository;
    private final TestDealRepository testDealRepository;
    private final ClientSteps clientSteps;
    private final ShardHelper shardHelper;

    @Autowired
    public DealSteps(DatabaseWrapperProvider databaseWrapperProvider,
                     DealRepository dealRepository,
                     TestDealRepository testDealRepository,
                     ClientSteps clientSteps,
                     ShardHelper shardHelper) {
        this.databaseWrapperProvider = databaseWrapperProvider;
        this.dealRepository = dealRepository;
        this.testDealRepository = testDealRepository;
        this.clientSteps = clientSteps;
        this.shardHelper = shardHelper;
    }

    public List<DealInfo> addRandomDeals(ClientInfo clientInfo, int num) {
        clientSteps.createClient(clientInfo);
        List<Deal> deals = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            deals.add(TestDeals.defaultPrivateDeal(clientInfo.getClientId()));
        }
        return addDeals(deals, clientInfo);
    }

    public List<DealInfo> addDeals(List<Deal> deals, ClientInfo clientInfo) {
        dealRepository.addDealsPpcDict(deals);
        dealRepository.addDealsPpc(clientInfo.getShard(), deals);

        return StreamEx.of(deals)
                .map(t -> new DealInfo().withDeal(t).withClientInfo(clientInfo))
                .toList();
    }

    public void deleteDeals(Collection<Deal> deals, ClientInfo clientInfo) {
        List<Long> dealIds = mapList(deals, Deal::getId);
        Integer shard = clientInfo.getShard();
        testDealRepository.deleteDealsPpcDict(dealIds);
        testDealRepository.deleteDealsPpc(shard, dealIds);
    }

    public void linkDealWithCampaign(Long dealId, Long campaignId) {
        CampaignDeal campaignDeal = new CampaignDeal().withCampaignId(campaignId).withDealId(dealId);
        dealRepository
                .linkCampaigns(shardHelper.getShardByCampaignId(campaignId), Collections.singletonList(campaignDeal));
    }

    public void unlinkDeals(int shard, List<Long> dealIds) {
        Map<Long, List<Long>> campaignByDealIds = dealRepository.getAllCampaignsDealsByDealIds(shard, dealIds);
        List<CampaignDeal> campaignDeals = StreamEx.of(campaignByDealIds.entrySet())
                .map(t -> StreamEx.of(t.getValue())
                        .mapToEntry(s -> s, s -> t.getKey())
                        .toList())
                .flatMap(s -> StreamEx.of(s))
                .map(t -> new CampaignDeal().withCampaignId(t.getKey()).withDealId(t.getValue()))
                .toList();
        dealRepository.unlinkCampaigns(shard, campaignDeals);
    }
}
