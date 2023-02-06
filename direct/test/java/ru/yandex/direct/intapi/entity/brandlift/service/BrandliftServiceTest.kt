package ru.yandex.direct.intapi.entity.brandlift.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.brandSurvey.BrandSurvey
import ru.yandex.direct.core.entity.brandlift.repository.BrandSurveyRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.intapi.IntApiException
import ru.yandex.direct.intapi.configuration.IntApiTest
import java.time.LocalDate

@IntApiTest
@RunWith(SpringJUnit4ClassRunner::class)
class BrandliftServiceTest {
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var brandliftService: BrandliftService
    @Autowired
    private lateinit var brandSurveyRepository : BrandSurveyRepository

    private lateinit var clientInfo : ClientInfo
    private var clientId: Long = 0L

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId?.asLong()!!
    }

    @Test
    fun severalCampaignsDateTest() {
        //тест на брендлифте с двумя РК мы даты начала и окончания правильно считаем, название вернём
        val brandSurveyId = "4JEyHAFp1SvS312fEZKnas"
        val brandSurvey = BrandSurvey()
            .withClientId(clientId)
            .withRetargetingConditionId(65432L)
            .withBrandSurveyId(brandSurveyId)
            .withName("Brand-lift name")
        brandSurveyRepository.addBrandSurvey(clientInfo.shard, brandSurvey)
        val startDate = LocalDate.of(2020, 2, 22)
        steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(
            clientInfo, brandSurveyId, startDate, startDate.plusMonths(1))
        steps.campaignSteps().createActiveCpmBannerCampaignWithBrandLift(
            clientInfo, brandSurveyId, startDate.plusMonths(2), startDate.plusMonths(3))

        var stats = brandliftService.getStats(brandSurveyId, clientId)

        assertThat(stats.surveyId).isEqualTo(brandSurveyId)
        assertThat(stats.name).isEqualTo("Brand-lift name")
        //Даты двух кампаний проверить период размещения
        assertThat(stats.startDate).isEqualTo("2020-02-22")
        assertThat(stats.endDate).isEqualTo("2020-05-22")
    }

    @Test(expected = IntApiException::class)
    fun notFountTest() {
        //тест что на несуществующем брендлифте исключение notFound
        brandliftService.getStats("nonexistssurvey", clientId)
    }
}
