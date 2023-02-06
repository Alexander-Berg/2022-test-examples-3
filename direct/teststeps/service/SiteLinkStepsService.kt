package ru.yandex.direct.teststeps.service

import org.jvnet.hk2.annotations.Service
import ru.yandex.direct.core.entity.sitelink.model.Sitelink
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.testing.info.SitelinkSetInfo
import ru.yandex.direct.core.testing.steps.SitelinkSetSteps

@Service
class SiteLinkStepsService(
    private val siteLinkSetSteps: SitelinkSetSteps,
    private val infoHelper: InfoHelper
) {
    fun createDefaultSiteLinkSet(login: String): Long {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)

        return siteLinkSetSteps.createDefaultSitelinkSet(clientInfo).sitelinkSetId
    }

    fun createSiteLinkSet(login: String, siteLinks: List<Sitelink>): Long {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)
        val siteLinkSetInfo = SitelinkSetInfo()
            .withClientInfo(clientInfo)
            .withSitelinkSet(SitelinkSet().withSitelinks(siteLinks))

        return siteLinkSetSteps.createSitelinkSet(siteLinkSetInfo).sitelinkSetId
    }

    fun linkBannerWithSiteLinkSet(login: String, campaignId: Long, bannerId: Long, siteLinkSetId: Long) {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)

        siteLinkSetSteps.linkBannerWithSitelinkSet(clientInfo.shard, campaignId, bannerId, siteLinkSetId)
    }
}
