package ru.yandex.direct.core.testing.info

import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate

data class BannerTurboLandingInfo(
    val turboLandingId: Long,
    val bannerId: Long,
    val campaignId: Long,
    val isDisabled: Boolean,
    val statusModerate: BannerTurboLandingStatusModerate
)
