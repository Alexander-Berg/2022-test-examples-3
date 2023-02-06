package ru.yandex.direct.jobs.uac.service

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.uac.service.CampaignContentUpdateService
import ru.yandex.direct.jobs.configuration.GrutJobsTest

/**
 * Проверяем обновление статуса у контента кампании в груте через вызов
 * функции updateCampaignContentsAndCampaignFlags в [CampaignContentUpdateService]
 * при групповых заявках
 */
class GrutCampaignContentUpdateServiceUpdateCampaignContentStatusWithAdGroupBriefTest
    : GrutCampaignContentUpdateServiceUpdateCampaignContentStatusTest() {
    override var adGroupBriefEnabled = true
}
