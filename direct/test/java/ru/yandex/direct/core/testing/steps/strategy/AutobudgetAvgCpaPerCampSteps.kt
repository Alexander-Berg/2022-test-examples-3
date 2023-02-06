package ru.yandex.direct.core.testing.steps.strategy

import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.strategy.model.AutobudgetAvgCpaPerCamp
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationService
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.strategy.AutobudgetAvgCpaPerCampInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.campaign.TextCampaignSteps
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider

@Component
class AutobudgetAvgCpaPerCampSteps(
    dslContextProvider: DslContextProvider,
    shardHelper: ShardHelper,
    campaignModifyRepository: CampaignModifyRepository,
    strategyTypedRepository: StrategyTypedRepository,
    strategyAddOperationService: StrategyAddOperationService,
    strategyUpdateOperationService: StrategyUpdateOperationService,
    walletService: WalletService,
    textCampaignSteps: TextCampaignSteps,
    clientSteps: ClientSteps
) : StrategySteps<AutobudgetAvgCpaPerCamp, AutobudgetAvgCpaPerCampInfo>(
    dslContextProvider,
    shardHelper,
    campaignModifyRepository,
    strategyTypedRepository,
    strategyUpdateOperationService,
    strategyAddOperationService,
    textCampaignSteps,
    walletService,
    clientSteps
) {
    override fun defaultStrategyInfo(clientInfo: ClientInfo): AutobudgetAvgCpaPerCampInfo {
        return AutobudgetAvgCpaPerCampInfo(clientInfo)
    }
}
