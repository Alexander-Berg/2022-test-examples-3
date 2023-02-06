package ru.yandex.direct.core.testing.steps.campaign

import ru.yandex.direct.core.entity.campaign.model.CommonCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter
import ru.yandex.direct.core.entity.campaign.service.type.add.CampaignAddOperationSupportFacade
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl
import ru.yandex.direct.core.testing.data.TestClients
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.info.campaign.CampaignInfo
import ru.yandex.direct.core.testing.steps.ClientSteps
import ru.yandex.direct.core.testing.steps.UserSteps
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.metrika.client.MetrikaClient
import ru.yandex.direct.rbac.RbacRole
import kotlin.reflect.KClass

abstract class CampaignSteps<C : CommonCampaign, T : CampaignInfo<C>>(
    private val dslContextProvider: DslContextProvider,
    private val campaignModifyRepository: CampaignModifyRepository,
    private val campaignAddOperationSupportFacade: CampaignAddOperationSupportFacade,
    private val clientSteps: ClientSteps,
    private val userSteps: UserSteps,
    private val metrikaClient: MetrikaClient
) {
    fun createDefaultCampaign(): T {
        return createCampaign(getCampaignInfo())
    }

    fun createDefaultCampaign(clientInfo: ClientInfo): T {
        return createDefaultCampaign(null, null, clientInfo)
    }

    fun createDefaultCampaign(managerInfo: UserInfo?, agencyInfo: UserInfo?, clientInfo: ClientInfo): T {
        val info = getCampaignInfo()
        info.managerInfo = managerInfo
        info.agencyInfo = agencyInfo
        info.withClientInfo(clientInfo) //будет заменено после удаления зависимости от старого Info
        return createCampaign(info)
    }

    fun createDefaultManagerCampaign(clientInfo: ClientInfo): T {
        return createDefaultCampaign(UserInfo(), null, clientInfo)
    }

    fun createDefaultManagerCampaign(managerInfo: UserInfo, clientInfo: ClientInfo): T {
        return createDefaultCampaign(managerInfo, null, clientInfo)
    }

    fun createDefaultAgencyCampaign(clientInfo: ClientInfo): T {
        return createDefaultCampaign(null, UserInfo(), clientInfo)
    }

    fun createDefaultAgencyCampaign(agencyInfo: UserInfo, clientInfo: ClientInfo): T {
        return createDefaultCampaign(null, agencyInfo, clientInfo)
    }

    fun createCampaign(typedCampaign: C): T {
        return createCampaign(null, null, ClientInfo(), typedCampaign)
    }

    fun createCampaign(clientInfo: ClientInfo, typedCampaign: C): T {
        return createCampaign(null, null, clientInfo, typedCampaign)
    }

    fun createCampaign(managerInfo: UserInfo?, agencyInfo: UserInfo?, clientInfo: ClientInfo,
                       typedCampaign: C): T {
        val info = getCampaignInfo()
        info.managerInfo = managerInfo
        info.agencyInfo = agencyInfo
        info.clientInfo = clientInfo
        info.typedCampaign = typedCampaign
        return createCampaign(info)
    }

    fun createCampaign(clientInfo: ClientInfo, campaignInfo: T): T {
        campaignInfo.withClientInfo(clientInfo)
        return createCampaign(campaignInfo)
    }

    fun createCampaign(campaignInfo: T): T {
        checkCampaignInfoConsistency(campaignInfo)
        val typedCampaign = campaignInfo.typedCampaign
        val clientInfo = campaignInfo.clientInfo

        //создаем менеджера, если задан managerInfo, но у менеджера нет uid-а
        val managerInfo = campaignInfo.managerInfo
        if (managerInfo != null) {
            if (managerInfo.user?.uid == null) {
                managerInfo.clientInfo = managerInfo.clientInfo
                    ?: ClientInfo(client = TestClients.defaultClient(RbacRole.MANAGER))
                userSteps.createUser(managerInfo)
            }

            //дозаполняем поля кампании
            typedCampaign.managerUid = managerInfo.uid
        }

        //создаем агенство, если задан agencyInfo, но у агенства нет uid-а
        val agencyInfo = campaignInfo.agencyInfo
        if (agencyInfo != null) {
            if (agencyInfo.user?.uid == null) {
                agencyInfo.clientInfo = agencyInfo.clientInfo
                    ?: ClientInfo(client = TestClients.defaultClient(RbacRole.AGENCY))
                userSteps.createUser(agencyInfo)
            }

            //дозаполняем поля кампании
            typedCampaign.agencyId = agencyInfo.clientId.asLong()
            typedCampaign.agencyUid = agencyInfo.uid
        }

        // если клиент не создан, создаем его
        if (campaignInfo.clientId == null) {
            if (agencyInfo != null) {
                clientSteps.createClientUnderAgency(agencyInfo, clientInfo)
            } else if (managerInfo != null) {
                clientSteps.createClientUnderManager(managerInfo.clientInfo!!, clientInfo)
            } else {
                clientSteps.createClient(clientInfo)
            }
            processClient(clientInfo, typedCampaign)
        }

        if (typedCampaign.uid == null) {
            typedCampaign.uid = clientInfo.uid
        }

        if (typedCampaign.clientId == null) {
            typedCampaign.clientId = clientInfo.clientId!!.asLong()
        }

        if (typedCampaign.fio == null) {
            typedCampaign.fio = clientInfo.chiefUserInfo!!.user!!.fio
        }

        if (typedCampaign.currency == null) {
            typedCampaign.currency = clientInfo.client!!.workCurrency
        }

        createRelations(campaignInfo)

        //временно костыль для старых кампаний
        val campaign = campaignInfo.campaign
        if (campaign != null && campaign.uid == null) {
            campaign.uid = clientInfo.uid
        }

        if (campaign != null && campaign.clientId == null) {
            campaign.clientId = clientInfo.clientId!!.asLong()
        }

        //use client uid for campaign constructor
        val operatorUid = clientInfo.uid
        val metrikaClientAdapter = RequestBasedMetrikaClientAdapter(metrikaClient, listOf(operatorUid), setOf())
        val container = RestrictedCampaignsAddOperationContainerImpl(
            clientInfo.shard,
            operatorUid, clientInfo.clientId!!, clientInfo.uid, clientInfo.uid, null,
            CampaignOptions(), metrikaClientAdapter, emptyMap()
        )

        val dsl = dslContextProvider.ppc(clientInfo.shard)
        campaignModifyRepository.addCampaigns(dsl, container, listOf(typedCampaign))
        campaignAddOperationSupportFacade.updateRelatedEntitiesInTransaction(dsl, container, listOf(typedCampaign))

        if (campaign != null) {
            campaign.id = typedCampaign.id
            campaign.orderId = typedCampaign.orderId
        }

        return campaignInfo
    }

    protected abstract fun getCampaignInfo(): T

    abstract fun getCampaignInfoClass(): KClass<T>

    protected open fun createRelations(campaignInfo: T) {
    }

    protected open fun processClient(clientInfo: ClientInfo, typedCampaign: C) = Unit

    private fun checkCampaignInfoConsistency(campaignInfo: T) {
        val campaign = campaignInfo.typedCampaign
        check(campaign.uid == null && campaign.clientId == null
            || campaign.uid != null && campaign.clientId != null) {
            "Uid and ClientId must be set together"
        }
        check(campaignInfo.agencyInfo?.user?.role == null
            || campaignInfo.agencyInfo?.user?.role == RbacRole.AGENCY) {
            "agencyInfo role should be AGENCY, but " + campaignInfo.agencyInfo?.user?.role
        }
        check(campaignInfo.managerInfo?.user?.role == null
            || campaignInfo.managerInfo?.user?.role == RbacRole.MANAGER) {
            "managerInfo role should be MANAGER, but " + campaignInfo.managerInfo?.user?.role
        }
    }
}
