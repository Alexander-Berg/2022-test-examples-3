package ru.yandex.direct.grid.processing.data

import java.math.BigDecimal
import ru.yandex.direct.grid.model.campaign.facelift.GdBudgetDisplayFormat
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateUcCampaignInput
import ru.yandex.direct.grid.processing.model.forecast.GdDeviceType
import ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemAgePoint
import ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemGender
import ru.yandex.direct.grid.processing.model.touchsocdem.GdTouchSocdemInput

fun defaultGdUpdateUcCampaignInput(
    campaignId: Long,
    hyperGeoId: Long? = null,
): GdUpdateUcCampaignInput = GdUpdateUcCampaignInput()
    .withId(campaignId)
    .withName("name")
    .withAdText("adText")
    .withAdTitle("adTitle")
    .withHref("https://ya.ru")
    .withBusinessCategory("business category")
    .withHyperGeoId(hyperGeoId)
    .withKeywords(listOf("keyword1", "keyword2"))
    .withDeviceTypes(setOf(GdDeviceType.ALL))
    .withBudgetDisplayFormat(GdBudgetDisplayFormat.DAILY)
    .withMetrikaCounters(listOf(69087046))
    .withBudget(BigDecimal.valueOf(900L))
    .withIsDraft(true)
    .withSocdem(GdTouchSocdemInput()
        .withGenders(listOf(GdTouchSocdemGender.MALE))
        .withAgeUpper(GdTouchSocdemAgePoint.AGE_25)
        .withAgeLower(GdTouchSocdemAgePoint.AGE_18)
    )
