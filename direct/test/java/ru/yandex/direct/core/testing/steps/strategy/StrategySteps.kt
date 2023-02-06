package ru.yandex.direct.core.testing.steps.strategy

import ru.yandex.direct.core.entity.campaign.model.CampaignWithPackageStrategy
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.WalletService
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyAddOperationContainer
import ru.yandex.direct.core.entity.strategy.container.StrategyOperationOptions
import ru.yandex.direct.core.entity.strategy.container.StrategyUpdateOperationContainer
import ru.yandex.direct.core.entity.strategy.model.CommonStrategy
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository
import ru.yandex.direct.core.entity.strategy.service.add.StrategyAddOperationService
import ru.yandex.direct.core.entity.strategy.service.update.StrategyUpdateOperationService
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.info.strategy.StrategyInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.campaign.TextCampaignSteps
import ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS
import ru.yandex.direct.dbutil.sharding.ShardHelper
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges

abstract class StrategySteps<S1 : CommonStrategy, S2 : StrategyInfo<S1>>(
    private val dslContextProvider: DslContextProvider,
    private val shardHelper: ShardHelper,
    private val campaignModifyRepository: CampaignModifyRepository,
    private val strategyTypedRepository: StrategyTypedRepository,
    private val strategyUpdateOperationService: StrategyUpdateOperationService,
    private val strategyAddOperationService: StrategyAddOperationService,
    private val textCampaignSteps: TextCampaignSteps,
    private val walletService: WalletService,
    private val clientSteps: ClientSteps
) {

    /**
     * Создает дефолтную стратегию без кампании и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @return инфо стратегии без кампании
     */
    fun createDefaultStrategy() =
        createStrategyBase<CampaignWithPackageStrategy, CampaignInfo<CampaignWithPackageStrategy>>(
            defaultStrategyInfo(ClientInfo()),
            null
        )

    /**
     * Создает дефолтную стратегию без кампании и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param clientInfo информация о клиенте
     * @return инфо стратегии без кампании
     */
    fun createDefaultStrategy(clientInfo: ClientInfo) =
        createStrategyBase<CampaignWithPackageStrategy, CampaignInfo<CampaignWithPackageStrategy>>(
            defaultStrategyInfo(clientInfo),
            null
        )


    /**
     * Создает дефолтную стратегию с кампанией и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @return инфо стратегии
     */
    fun createDefaultStrategyWithCampaign() = createDefaultStrategyWithCampaign(createDefaultCampaign())

    /**
     * Создает дефолтную стратегию с кампанией и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param clientInfo информация о клиенте
     * @return инфо стратегии
     */
    fun createDefaultStrategyWithCampaign(clientInfo: ClientInfo) =
        createDefaultStrategyWithCampaign(createDefaultCampaign(clientInfo))

    /**
     * Создает дефолтную стратегию с кампании и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param campaignInfo информация о кампании
     * @return инфо стратегии
     */
    fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        createDefaultStrategyWithCampaign(campaignInfo: C2) =
        createStrategyWithCampaign(defaultStrategyInfo(campaignInfo.clientInfo), campaignInfo)


    /**
     * Создает стратегию без кампании и отдает инфо с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param typedStrategy объект стратегии с частично заполненными значениями
     * @return инфо стратегии без кампании
     */
    fun createStrategy(typedStrategy: S1): S2 {
        val strategyInfo = defaultStrategyInfo(ClientInfo())

        strategyInfo.typedStrategy = typedStrategy

        return createStrategyBase<CampaignWithPackageStrategy, CampaignInfo<CampaignWithPackageStrategy>>(
            strategyInfo,
            null
        )
    }

    /**
     * Создает стратегию без кампании и отдает ифно с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param strategyInfo инфо стратегии с частично заполненными значениями
     * @return инфо стратегии без кампании
     */
    fun createStrategy(strategyInfo: S2) =
        createStrategyBase<CampaignWithPackageStrategy, CampaignInfo<CampaignWithPackageStrategy>>(
            strategyInfo,
            null
        )

    /**
     * Создает стратегию с кампанией и отдает ифно с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param typedStrategy объект стратегии с частично заполненными значениями
     * @return инфо стратегии
     */
    fun createStrategyWithCampaign(typedStrategy: S1): S2 {
        val campaignInfo = createDefaultCampaign()
        val strategyInfo = defaultStrategyInfo(campaignInfo.clientInfo)

        strategyInfo.typedStrategy = typedStrategy

        return createStrategyWithCampaign(strategyInfo, campaignInfo)
    }

    /**
     * Создает стратегию с кампанией и отдает ифно с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param strategyInfo инфо стратегии с частично заполненными значениями
     * @return инфо стратегии
     */
    fun createStrategyWithCampaign(strategyInfo: S2) =
        createStrategyWithCampaign(strategyInfo, createDefaultCampaign(strategyInfo.clientInfo))

    /**
     * Создает стратегию с кампанией и отдает ифно с ней,
     * также создавая пользователя и кошельком по необходимости
     *
     * @param strategyInfo инфо стратегии с частично заполненными значениями
     * @param campaignInfo инфо кампании с частично заполненными значениями
     * @return инфо стратегии
     */
    fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        createStrategyWithCampaign(strategyInfo: S2, campaignInfo: C2): S2 =
        createStrategyBase(strategyInfo, campaignInfo)


    /**
     * Обновляет объект стратегии в переданной инфо, заменяя на актуальный объект из базы
     *
     * @param strategyInfo инфо стратегии
     */
    fun updateToActualStrategy(strategyInfo: S2) {

        val strategyIdToStrategy = strategyTypedRepository.getIdToModelTyped(
            strategyInfo.shard,
            strategyInfo.clientInfo.clientId!!,
            listOf(strategyInfo.strategyId)
        )

        strategyInfo.typedStrategy = strategyIdToStrategy[strategyInfo.strategyId] as S1
    }

    /**
     * Создает связь стратегию и кампанию. Если они уже связаны, то ничего не делает и
     * возвращает переданное инфо стратегии.
     *
     * @param strategyInfo инфо стратегии, которую связывают
     * @param campaignInfo инфо кампании, которую связывают
     * @return обновленное инфо стратегии
     */
    fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        bindStrategyAndCampaign(strategyInfo: S2, campaignInfo: C2): S2 {

        if (campaignInfo.campaignId in strategyInfo.typedStrategy.cids) return strategyInfo

        val newCids = strategyInfo.typedStrategy.cids.toMutableList().apply { add(campaignInfo.campaignId) }
        val modelChanges: ModelChanges<S1> =
            ModelChanges.build(strategyInfo.typedStrategy, CommonStrategy.CIDS, newCids)

        strategyUpdateOperationService.execute(
            StrategyUpdateOperationContainer(
                strategyInfo.shard,
                strategyInfo.clientInfo.clientId!!,
                strategyInfo.clientInfo.uid,
                strategyInfo.uid
            ),
            mapOf(0 to modelChanges),
            mapOf(strategyInfo.strategyId to strategyInfo.typedStrategy),
            listOf(modelChanges.applyTo(strategyInfo.typedStrategy))
        )

        updateCampaignBinding(campaignInfo, strategyInfo.strategyId)

        return strategyInfo
    }

    /**
     * Убирает связь между стратегией и кампанией. Если они уже не связаны,
     * то ничего не делает и возвращает переданное инфо стратегии. Для кампании создает
     * дефолтную стратегию
     *
     * @param strategyInfo инфо стратегии, которую связывают
     * @param campaignInfo инфо кампании, которую связывают
     * @return обновленное инфо стратегии
     */
    fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        unbindStrategyAndCampaign(strategyInfo: S2, campaignInfo: C2): S2 {

        if (campaignInfo.campaignId !in strategyInfo.typedStrategy.cids) return strategyInfo

        val newCids = strategyInfo.typedStrategy.cids.toMutableList().apply { remove(campaignInfo.campaignId) }
        val modelChanges: ModelChanges<S1> =
            ModelChanges.build(strategyInfo.typedStrategy, CommonStrategy.CIDS, newCids)

        strategyUpdateOperationService.execute(
            StrategyUpdateOperationContainer(
                strategyInfo.shard,
                strategyInfo.clientInfo.clientId!!,
                strategyInfo.clientInfo.uid,
                strategyInfo.uid
            ),
            mapOf(0 to modelChanges),
            mapOf(strategyInfo.strategyId to strategyInfo.typedStrategy),
            listOf(modelChanges.applyTo(strategyInfo.typedStrategy))
        )

        val newStrategyInfo = createDefaultStrategyWithCampaign(campaignInfo)

        updateCampaignBinding(campaignInfo, newStrategyInfo.typedStrategy.id)

        return strategyInfo
    }

    abstract fun defaultStrategyInfo(clientInfo: ClientInfo): S2

    private fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        createStrategyBase(strategyInfo: S2, campaignInfo: C2?): S2 {

        if (strategyInfo.typedStrategy.cids == null) {
            strategyInfo.typedStrategy.cids = mutableListOf()
        }

        if (strategyInfo.clientInfo.clientId == null) {
            clientSteps.createClient(strategyInfo.clientInfo)
        }

        strategyInfo.typedStrategy.clientId = strategyInfo.clientInfo.clientId!!.asLong()

        var walletId: Long? = null

        if (campaignInfo != null) {

            strategyInfo.typedStrategy.cids.add(campaignInfo.campaignId)

            if (strategyInfo.typedStrategy.walletId == null) {
                walletId = campaignInfo.typedCampaign.walletId
            }
        }

        if (strategyInfo.typedStrategy.walletId == null) {
            if (walletId == null) {
                walletId = walletService.createWalletForNewClient(
                    strategyInfo.clientInfo.clientId,
                    strategyInfo.clientInfo.uid
                )
            }
            strategyInfo.typedStrategy.walletId = walletId
        }

        val shard = shardHelper.getShardByClientId(strategyInfo.clientInfo.clientId!!)

        val strategyIds = strategyAddOperationService.execute(
            StrategyAddOperationContainer(
                shard,
                strategyInfo.clientInfo.clientId!!,
                strategyInfo.clientInfo.uid,
                strategyInfo.uid,
                StrategyOperationOptions()
            ),
            listOf(strategyInfo.typedStrategy)
        )

        strategyInfo.typedStrategy.id = strategyIds[0]

        if (campaignInfo != null) {
            updateCampaignBinding(campaignInfo, strategyInfo.typedStrategy.id)
        }

        return strategyInfo
    }

    private fun createDefaultCampaign() = createDefaultCampaign(clientSteps.createDefaultClient())

    private fun createDefaultCampaign(clientInfo: ClientInfo): TextCampaignInfo {
        val walletId = walletService.createWalletForNewClient(clientInfo.clientId, clientInfo.uid)

        val campaignInfo = textCampaignSteps.createDefaultCampaign(clientInfo)

        campaignInfo.typedCampaign.walletId = walletId

        campaignModifyRepository.updateCampaigns(
            RestrictedCampaignsUpdateOperationContainer.create(
                campaignInfo.shard,
                campaignInfo.clientInfo.uid,
                campaignInfo.clientId,
                campaignInfo.uid,
                campaignInfo.uid
            ),
            mutableListOf(
                ModelChanges.build(
                    campaignInfo.typedCampaign,
                    CampaignWithPackageStrategy.WALLET_ID,
                    walletId
                ).applyTo(campaignInfo.typedCampaign)
            )
        )

        return campaignInfo
    }

    private fun <C1 : CampaignWithPackageStrategy, C2 : CampaignInfo<C1>>
        updateCampaignBinding(campaignInfo: C2, strategyId: Long): C2 {
        campaignInfo.typedCampaign.strategyId = strategyId

        dslContextProvider.ppc(campaignInfo.shard)
            .update(CAMPAIGNS)
            .set(CAMPAIGNS.STRATEGY_ID, strategyId)
            .where(CAMPAIGNS.CID.eq(campaignInfo.campaignId))
            .execute()

        return campaignInfo
    }
}
