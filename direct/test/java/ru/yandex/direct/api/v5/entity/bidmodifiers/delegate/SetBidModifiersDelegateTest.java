package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierSetItem;
import com.yandex.direct.api.v5.bidmodifiers.SetRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.result.ApiMassResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGradeAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.typesupport.BidModifierTypeSupportDispatcher;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultPerformanceTgo;
import static ru.yandex.direct.utils.FunctionalUtils.flatMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@Api5Test
@RunWith(SpringRunner.class)
public class SetBidModifiersDelegateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private ResultConverter resultConverter;
    @Autowired
    private BidModifierTypeSupportDispatcher typeSupportDispatcher;
    @Autowired
    private AdGroupService adGroupService;
    @Mock
    private ApiAuthenticationSource auth;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;
    @Autowired
    private FeatureService featureService;

    private SetBidModifiersDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        delegate = new SetBidModifiersDelegate(bidModifierService,
                resultConverter,
                typeSupportDispatcher,
                auth,
                ppcPropertiesSupport,
                featureService);
    }

    @Test
    public void set_SmartBidModifier_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var campaignId = adGroupInfo.getCampaignId();
        var adGroupId = adGroupInfo.getAdGroupId();

        var adjustment = new BidModifierPerformanceTgoAdjustment().withPercent(20);
        var bidModifier = createDefaultPerformanceTgo(campaignId, adGroupId)
                .withPerformanceTgoAdjustment(adjustment);
        var bidModifierInfo = steps.bidModifierSteps().createAdGroupBidModifier(bidModifier, adGroupInfo);
        var bidModifierRealId = bidModifierInfo.getBidModifierId();
        var bidModifierExternalId = BidModifierService.getExternalId(bidModifierRealId,
                BidModifierType.PERFORMANCE_TGO_MULTIPLIER);

        var bidModifierSetItem = new BidModifierSetItem().withId(bidModifierExternalId).withBidModifier(50);
        var externalRequest = new SetRequest().withBidModifiers(bidModifierSetItem);

        List<ModelChanges<BidModifierAdjustment>> request = delegate.convertRequest(externalRequest);
        ApiMassResult<Long> apiResult = delegate.processList(request);
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        List<BidModifier> bidModifiers = bidModifierService.getByAdGroupIds(clientInfo.getClientId(),
                Set.of(adGroupId),
                Set.of(campaignId),
                Set.of(BidModifierType.PERFORMANCE_TGO_MULTIPLIER),
                Set.of(BidModifierLevel.ADGROUP),
                clientInfo.getUid());
        var modifier = (BidModifierPerformanceTgo) bidModifiers.get(0);
        assertThat(modifier.getPerformanceTgoAdjustment().getPercent()).isEqualTo(bidModifierSetItem.getBidModifier());
    }

    @Test
    public void set_IncomeGradeBidModifier_success() {
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        var campaignId = campaignInfo.getCampaignId();
        var adGroupId = adGroupInfo.getAdGroupId();

        var modifier = new BidModifierPrismaIncomeGrade()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withType(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
                .withEnabled(true)
                .withExpressionAdjustments(
                        List.of(new BidModifierPrismaIncomeGradeAdjustment()
                                        .withPercent(100)
                                        .withCondition(List.of(List.of(
                                                new BidModifierExpressionLiteral()
                                                        .withOperation(BidModifierExpressionOperator.EQ)
                                                        .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                                        .withValueString("0")
                                        ))),
                                new BidModifierPrismaIncomeGradeAdjustment()
                                        .withPercent(120)
                                        .withCondition(List.of(List.of(
                                                new BidModifierExpressionLiteral()
                                                        .withOperation(BidModifierExpressionOperator.EQ)
                                                        .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                                        .withValueString("1")
                                        ))))
                );
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(),
                FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED,
                true);
        var addResult = bidModifierService.add(List.of(modifier), clientInfo.getClientId(), clientInfo.getUid());
        var modifierIds = flatMap(addResult.getResult(), Result::getResult);
        var expectedBidModifier = 50;
        var bidModifierSetItem = mapList(modifierIds,
                mId -> new BidModifierSetItem().withId(mId).withBidModifier(expectedBidModifier));
        var externalRequest = new SetRequest().withBidModifiers(bidModifierSetItem);

        List<ModelChanges<BidModifierAdjustment>> request = delegate.convertRequest(externalRequest);
        ApiMassResult<Long> apiResult = delegate.processList(request);
        assertThat(apiResult.getErrorCount()).isEqualTo(0);
        List<BidModifier> bidModifiers = bidModifierService.getByAdGroupIds(clientInfo.getClientId(),
                Set.of(adGroupId),
                Set.of(campaignId),
                Set.of(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER),
                Set.of(BidModifierLevel.ADGROUP),
                clientInfo.getUid());
        assertThat(bidModifiers).hasSize(1);

        BidModifierPrismaIncomeGrade bidModifier = (BidModifierPrismaIncomeGrade) bidModifiers.get(0);

        assertThat(bidModifier.getExpressionAdjustments()).hasSize(2);
        bidModifier.getExpressionAdjustments().forEach(adj -> assertThat(adj.getPercent()).isEqualTo(expectedBidModifier));
    }

}
