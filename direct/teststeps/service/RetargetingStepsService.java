package ru.yandex.direct.teststeps.service;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;

@Service
@ParametersAreNonnullByDefault
public class RetargetingStepsService {

    private final RetargetingRepository retargetingRepository;
    private final AdGroupRepository adGroupRepository;
    private final ShardHelper shardHelper;

    @Autowired
    public RetargetingStepsService(RetargetingRepository retargetingRepository,
                                   AdGroupRepository adGroupRepository,
                                   ShardHelper shardHelper) {
        this.retargetingRepository = retargetingRepository;
        this.adGroupRepository = adGroupRepository;
        this.shardHelper = shardHelper;
    }

    public Long createRetargeting(Long adGroupId, Long retCondId, BigDecimal priceContext) {
        int shard = shardHelper.getShardByGroupId(adGroupId);
        AdGroupSimple adGroupSimple = adGroupRepository.getAdGroupSimple(shard, null, List.of(adGroupId))
                .get(adGroupId);
        Long campaignId = adGroupSimple.getCampaignId();

        Retargeting retargeting = defaultRetargeting(campaignId, adGroupId, retCondId)
                .withPriceContext(priceContext);

        return retargetingRepository.add(shard, List.of(retargeting))
                .get(0);
    }

}
