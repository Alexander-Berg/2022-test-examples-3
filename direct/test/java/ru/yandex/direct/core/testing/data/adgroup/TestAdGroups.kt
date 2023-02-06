package ru.yandex.direct.core.testing.data.adgroup

import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast
import ru.yandex.direct.regions.Region
import ru.yandex.direct.utils.CommonUtils
import java.time.LocalDateTime

object TestAdGroups {

    fun fillCommonClientFields(adGroup: AdGroup) {
        adGroup.withName("test group " + RandomStringUtils.randomNumeric(5))
            .withMinusKeywords(emptyList())
            .withLibraryMinusKeywordsIds(emptyList())
            .withGeo(CommonUtils.nvl(adGroup.geo, listOf(Region.RUSSIA_REGION_ID)))
            .withPageGroupTags(CommonUtils.nvl(adGroup.pageGroupTags, emptyList()))
            .withTargetTags(CommonUtils.nvl(adGroup.targetTags, emptyList()))
    }

    fun fillCommonSystemFieldsForDraftAdGroup(adGroup: AdGroup) {
        adGroup.withStatusBsSynced(StatusBsSynced.NO)
            .withStatusModerate(StatusModerate.NEW)
            .withStatusPostModerate(StatusPostModerate.NEW)
    }

    fun fillCommonSystemFieldsForActiveAdGroup(adGroup: AdGroup) {
        adGroup.withStatusBsSynced(StatusBsSynced.YES)
            .withStatusModerate(StatusModerate.YES)
            .withStatusPostModerate(StatusPostModerate.YES)
            .withStatusAutobudgetShow(true)
            .withStatusShowsForecast(StatusShowsForecast.SENDING)
            .withBsRarelyLoaded(false)
            .withPriorityId(1L)
            .withForecastDate(LocalDateTime.now().minusDays(1).withNano(0))
    }
}
