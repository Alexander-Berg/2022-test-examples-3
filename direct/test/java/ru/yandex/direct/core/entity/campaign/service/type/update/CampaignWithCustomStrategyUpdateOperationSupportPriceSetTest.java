package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.service.BidBsStatisticFacade;
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithDefaultPriceRecalculation;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.keyword.repository.KeywordRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualSearchStrategy;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.core.testing.data.campaign.TestCampaignsStrategy.averageBidStrategy;
import static ru.yandex.direct.core.testing.data.campaign.TestContentPromotionCampaigns.fullContentPromotionCampaign;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignWithCustomStrategyUpdateOperationSupportPriceSetTest {

    private static final BigDecimal FIRST_PLACE_PRICE = BigDecimal.valueOf(RandomUtils.nextLong(1000, 10000), 2);
    private static final BigDecimal MULTIPLIED_FIRST_PLACE_PRICE = FIRST_PLACE_PRICE.multiply(BigDecimal.valueOf(1.3))
            .setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(300, 2);     // 3.00

    @Autowired
    public Steps steps;
    @Autowired
    CampaignWithDefaultPriceRecalculationUpdateOperationSupport typeSupport;
    @Autowired
    BidBsStatisticFacade bidBsStatisticFacadeMock;
    @Autowired
    CampaignTypedRepository campaignTypedRepository;
    @Autowired
    KeywordRepository keywordRepository;

    private ClientInfo clientInfo;
    private ClientId clientId;
    private int shard;
    private Long textCampaignId;
    private Long contentPromotionCampaignId;

    @Before
    public void before() {
        var contentPromotionCampaignInfo =
                steps.contentPromotionCampaignSteps().createCampaign(fullContentPromotionCampaign()
                .withStrategy(averageBidStrategy()));
        contentPromotionCampaignId = contentPromotionCampaignInfo.getCampaignId();

        clientInfo = contentPromotionCampaignInfo.getClientInfo();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        CampaignInfo textCampaignInfo = steps.campaignSteps().createActiveTextCampaignAutoStrategy(clientInfo);
        textCampaignId = textCampaignInfo.getCampaignId();

        // для всех кейвордов клиента возвращаем ставку FIRST_PLACE_PRICE
        when(bidBsStatisticFacadeMock.bidBsStatisticFirstPosition(eq(clientId), anyList()))
                .thenAnswer(invocation -> {
                    List<Bid> bids = invocation.getArgument(1);
                    return StreamEx.of(bids)
                            .mapToEntry(Bid::getId, b -> Money.valueOf(FIRST_PLACE_PRICE, CurrencyCode.RUB))
                            .toMap();
                });

        AdGroupInfo textAdGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(textCampaignInfo);
        steps.keywordSteps().createKeyword(textAdGroupInfo, defaultKeyword()
                .withPrice(BigDecimal.ZERO)
                .withPriceContext(BigDecimal.ZERO)
        );

        var contentPromotionAdGroupInfo = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(contentPromotionCampaignInfo, ContentPromotionAdgroupType.VIDEO);
        steps.newKeywordSteps().createKeyword(contentPromotionAdGroupInfo, defaultKeyword()
                .withPrice(BigDecimal.ZERO)
                .withPriceContext(BigDecimal.ZERO)
        );
    }

    @Test
    public void textCampaign_ChangeAutobudgetStrategyToManual_FirstPlacePriceSet() {
        AppliedChanges<CampaignWithDefaultPriceRecalculation> campaignAppliedChanges =
                applyChangingStrategyToManual(textCampaignId);
        var parametersContainer = getUpdateCampaignParametersContainer();
        typeSupport.updateRelatedEntitiesOutOfTransaction(parametersContainer, List.of(campaignAppliedChanges));

        List<Keyword> keywordsAfter = keywordRepository.getKeywordsByCampaignId(shard, textCampaignId);
        assertThat(keywordsAfter).hasSize(1);
        assertThat(keywordsAfter.get(0).getPrice()).isEqualTo(MULTIPLIED_FIRST_PLACE_PRICE);
    }

    @Test
    public void contentPromotionCampaign_ChangeAutobudgetStrategyToManual_DefaultPriceSet() {
        AppliedChanges<CampaignWithDefaultPriceRecalculation> campaignAppliedChanges =
                applyChangingStrategyToManual(contentPromotionCampaignId);
        var parametersContainer = getUpdateCampaignParametersContainer();
        typeSupport.updateRelatedEntitiesOutOfTransaction(parametersContainer, List.of(campaignAppliedChanges));

        List<Keyword> keywordsAfter = keywordRepository.getKeywordsByCampaignId(shard, contentPromotionCampaignId);
        assertThat(keywordsAfter).hasSize(1);
        assertThat(keywordsAfter.get(0).getPrice()).isEqualTo(DEFAULT_PRICE);
    }

    private AppliedChanges<CampaignWithDefaultPriceRecalculation> applyChangingStrategyToManual(Long campaignId) {
        List<? extends BaseCampaign> campaigns = campaignTypedRepository
                .getTypedCampaigns(shard, List.of(campaignId));
        assertThat(campaigns).hasSize(1);
        CampaignWithDefaultPriceRecalculation campaign = (CampaignWithDefaultPriceRecalculation) campaigns.get(0);

        ModelChanges<CampaignWithDefaultPriceRecalculation> campaignModelChanges =
                ModelChanges.build(campaign, CampaignWithDefaultPriceRecalculation.STRATEGY, manualSearchStrategy());
        return campaignModelChanges.applyTo(campaign);
    }

    private RestrictedCampaignsUpdateOperationContainer getUpdateCampaignParametersContainer() {
        return RestrictedCampaignsUpdateOperationContainer.create(
                clientInfo.getShard(),
                clientInfo.getUid(),
                clientInfo.getClientId(),
                clientInfo.getUid(),
                clientInfo.getUid());
    }
}
