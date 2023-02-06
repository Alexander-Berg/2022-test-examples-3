package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.bids.service.CommonSetBidsValidationService;
import ru.yandex.direct.core.entity.campaign.container.AffectedCampaignIdsContainer;
import ru.yandex.direct.core.entity.campaign.repository.CampaignAccessCheckRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetSetBidsService;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.RequestSetBidType;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicTextAdTargetSetBidsValidationServicePositiveTest {

    private DynamicTextAdTargetSetBidsValidationService setBidsValidationService;

    @Autowired
    private ShardHelper shardHelper;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignAccessCheckRepository campaignAccessCheckRepository;

    @Autowired
    private ClientService clientService;

    @Autowired
    private DynamicTextAdTargetSetBidsService dynamicTextAdTargetSetBidsService;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private RequestCampaignAccessibilityCheckerProvider requestAccessibleCampaignTypes;

    @Autowired
    private FeatureService featureService;
    private long operatorUid;
    private ClientId clientId;
    private int shard;
    private DynamicTextAdTargetInfo dynamicTextAdTarget;
    private AdGroupInfo defaultAdGroup;

    @Before
    public void before() {
        defaultAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();
        operatorUid = defaultAdGroup.getUid();
        clientId = defaultAdGroup.getClientId();
        dynamicTextAdTarget = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(defaultAdGroup);
        shard = defaultAdGroup.getShard();

        setBidsValidationService = new DynamicTextAdTargetSetBidsValidationService(
                new CommonSetBidsValidationService(),
                new CampaignSubObjectAccessCheckerFactory(
                        shardHelper, rbacService, campaignAccessCheckRepository, new AffectedCampaignIdsContainer(),
                        requestAccessibleCampaignTypes, featureService),
                clientService,
                campaignRepository
        );
    }

    private ValidationResult<List<SetBidItem>, Defect> validate(List<SetBidItem> setBids,
                                                                RequestSetBidType requestType) {
        List<DynamicTextAdTarget> dynamicTextAdTargets =
                dynamicTextAdTargetSetBidsService.getDynamicTextAdTargetsByBid(shard, clientId, setBids, requestType);

        return setBidsValidationService
                .validateForTextAdTargets(shard, operatorUid, clientId, setBids, requestType, dynamicTextAdTargets);
    }

    @Test
    public void validate_AdGroupIdNegative() {
        ValidationResult<List<SetBidItem>, Defect> validationResult =
                validate(singletonList(new SetBidItem()
                        .withId(dynamicTextAdTarget.getDynamicConditionId())
                        .withPriceSearch(BigDecimal.valueOf(123L))), RequestSetBidType.ID);

        assertThat(validationResult.hasAnyErrors()).isEqualTo(false);
    }
}
