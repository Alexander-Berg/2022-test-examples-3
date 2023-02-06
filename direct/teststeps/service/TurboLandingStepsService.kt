package ru.yandex.direct.teststeps.service

import org.jvnet.hk2.annotations.Service
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate
import ru.yandex.direct.core.testing.info.BannerTurboLandingInfo
import ru.yandex.direct.core.testing.steps.TurboLandingSteps

@Service
class TurboLandingStepsService(
    private val turboLandingsSteps: TurboLandingSteps,
    private val infoHelper: InfoHelper
) {
    fun createDefaultTurboLanding(login: String): Long {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)
        return turboLandingsSteps.createDefaultTurboLanding(clientInfo.shard, clientInfo.clientId)
            .id
    }

    fun deleteTurboLandings(login: String, ids: List<Long>) {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)
        turboLandingsSteps.deleteTurboLandings(clientInfo.shard, ids)
    }

    fun linkBannerWithTurboLanding(login: String, turboLandingId: Long, bannerId: Long, campaignId: Long, isDisabled: Boolean, statusModerate: BannerTurboLandingStatusModerate) {
        val userInfo = infoHelper.getUserInfo(login)
        val clientInfo = infoHelper.getClientInfo(login, userInfo.uid)
        val bannerTurboLandingInfo = BannerTurboLandingInfo(
            turboLandingId = turboLandingId,
            bannerId = bannerId,
            campaignId = campaignId,
            isDisabled = isDisabled,
            statusModerate = statusModerate
        )
        turboLandingsSteps.linkBannerWithTurboLanding(clientInfo.shard, bannerTurboLandingInfo)
    }
}
