package ru.yandex.direct.core.entity.strategy.type.withcampaignids;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpi;
import ru.yandex.direct.core.entity.strategy.model.StrategyAttributionModel;
import ru.yandex.direct.core.entity.strategy.model.StrategyName;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithCampaignIds;
import ru.yandex.direct.core.entity.strategy.repository.StrategyRepositoryTypeSupportFacade;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelperAggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;

@CoreTest
@RunWith(SpringRunner.class)
public class StrategyWithCampaignIdsRepositoryTypeSupportTest {

    @Autowired
    private StrategyRepositoryTypeSupportFacade strategyRepositoryTypeSupportFacade;

    @Autowired
    private DslContextProvider ppcDslContextProvider;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private ShardHelper shardHelper;

    private ClientInfo clientInfo;

    @Before
    public void init() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void readStrategyWithOneLinkedCampaign() {
        var campaign = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var strategy = strategy();
        var insertHelperAggregator = new InsertHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        strategyRepositoryTypeSupportFacade.pushToInsert(insertHelperAggregator, List.of(strategy));
        insertHelperAggregator.executeIfRecordsAdded();
        setStrategyId(List.of(campaign.getCampaignId()), strategy);

        var expectedStrategy = strategy.withCids(List.of(campaign.getCampaignId()));

        var actualStrategy =
                strategyTypedRepository.getIdToModelTyped(clientInfo.getShard(), List.of(strategy.getId())).get(strategy.getId());

        assertThat(actualStrategy).isEqualTo(expectedStrategy);
    }

    @Test
    public void readStrategyWithFewLinkedCampaign() {
        var campaign1 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var campaign2 = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        var strategy = strategy();
        var insertHelperAggregator = new InsertHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        strategyRepositoryTypeSupportFacade.pushToInsert(insertHelperAggregator, List.of(strategy));
        insertHelperAggregator.executeIfRecordsAdded();
        setStrategyId(List.of(campaign1.getCampaignId(), campaign2.getCampaignId()), strategy);

        var expectedStrategy = strategy.withCids(List.of(campaign1.getCampaignId(), campaign2.getCampaignId()));

        var actualStrategy =
                strategyTypedRepository.getIdToModelTyped(clientInfo.getShard(), List.of(strategy.getId())).get(strategy.getId());

        assertThat(actualStrategy).isEqualTo(expectedStrategy);
    }

    @Test
    public void readStrategyWithoutLinkedCampaign() {
        var strategy = strategy();
        var insertHelperAggregator = new InsertHelperAggregator(ppcDslContextProvider.ppc(clientInfo.getShard()));
        strategyRepositoryTypeSupportFacade.pushToInsert(insertHelperAggregator, List.of(strategy));
        insertHelperAggregator.executeIfRecordsAdded();

        var actualStrategy =
                strategyTypedRepository.getIdToModelTyped(clientInfo.getShard(), List.of(strategy.getId())).get(strategy.getId());

        assertThat(actualStrategy).isEqualTo(strategy);
        assertThat(strategy.getCids()).isNull();
    }

    private void setStrategyId(List<Long> cids, StrategyWithCampaignIds strategy) {
        ppcDslContextProvider.ppc(clientInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_ID, strategy.getId())
                .where(CAMPAIGNS.CID.in(cids))
                .execute();
    }

    private StrategyWithCampaignIds strategy() {
        return new AutobudgetAvgCpi()
                .withType(StrategyName.AUTOBUDGET_AVG_CPI)
                .withClientId(1L)
                .withWalletId(2L)
                .withAttributionModel(StrategyAttributionModel.FIRST_CLICK)
                .withStatusArchived(false)
                .withIsPublic(false)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES))
                .withId(nextStrategyId())
                .withGoalId(15L)
                .withBid(BigDecimal.valueOf(132.5))
                .withAvgCpi(BigDecimal.valueOf(13.6))
                .withLastBidderRestartTime(LocalDate.now().atStartOfDay())
                .withSum(BigDecimal.valueOf(9999.99))
                .withIsPayForConversionEnabled(true);
    }

    private Long nextStrategyId() {
        return shardHelper.generateStrategyIds(clientInfo.getClientId().asLong(), 1).get(0);
    }
}
