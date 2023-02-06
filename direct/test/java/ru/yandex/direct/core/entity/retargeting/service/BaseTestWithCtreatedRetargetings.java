package ru.yandex.direct.core.entity.retargeting.service;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingRepository;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.core.testing.steps.RetargetingSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.util.Arrays.asList;

public class BaseTestWithCtreatedRetargetings {

    @Autowired
    protected RetargetingSteps retargetingSteps;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected RetargetingRepository retargetingRepository;

    @Autowired
    protected RetargetingService serviceUnderTest;

    @Autowired
    protected DslContextProvider dslContextProvider;

    protected RetargetingInfo retargetingInfo1;
    protected RetargetingInfo retargetingInfo2;
    protected int shard;
    protected long uid;
    protected ClientId clientId;
    protected long campaignId;
    protected long retargetingId1;
    protected long retargetingId2;
    protected List<Long> retargetingIds;

    @Before
    public void before() {
        retargetingInfo1 = retargetingSteps.createDefaultRetargeting();
        retargetingInfo2 = retargetingSteps.createDefaultRetargeting(retargetingInfo1.getAdGroupInfo());
        shard = retargetingInfo1.getShard();
        uid = retargetingInfo1.getUid();
        clientId = retargetingInfo1.getClientId();
        campaignId = retargetingInfo1.getCampaignId();
        retargetingId1 = retargetingInfo1.getRetargetingId();
        retargetingId2 = retargetingInfo2.getRetargetingId();
        retargetingIds = asList(retargetingId1, retargetingId2);
    }
}
