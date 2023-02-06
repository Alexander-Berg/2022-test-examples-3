package ru.yandex.direct.core.copyentity.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.LocalDateAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import java.time.LocalDate
import java.time.Period

@CoreTest
@RunWith(JUnitParamsRunner::class)
class CopyCampaignStartEndTimeTest : BaseCopyCampaignTest() {

    private val todayPlaceholder = LocalDate.MIN

    private val today = LocalDate.now()

    @Before
    fun before() {
        client = steps.clientSteps().createDefaultClient()
    }

    fun startEndDateParams() = arrayOf(
        arrayOf(
            today - Period.ofDays(30), null,
            todayPlaceholder, null,
            true,
        ),
        arrayOf(
            today + Period.ofDays(30), null,
            today + Period.ofDays(30), null,
            true,
        ),

        arrayOf(
            today - Period.ofDays(30), today - Period.ofDays(30),
            todayPlaceholder, todayPlaceholder,
            false,
        ),
        arrayOf(
            today - Period.ofDays(30), today + Period.ofDays(30),
            todayPlaceholder, today + Period.ofDays(30),
            true,
        ),
        arrayOf(
            today + Period.ofDays(30), today + Period.ofDays(30),
            today + Period.ofDays(30), today + Period.ofDays(30),
            true,
        ),
    )

    @Test
    @Parameters(method = "startEndDateParams")
    fun testStartEndDate(
        startDate: LocalDate?,
        endDate: LocalDate?,
        expectedStartDate: LocalDate?,
        expectedEndDate: LocalDate?,
        expectedStatusShow: Boolean,
    ) {
        val campaign = steps.textCampaignSteps().createCampaign(client, fullTextCampaign()
            .withStartDate(startDate)
            .withEndDate(endDate)
            .withStatusShow(true))

        val copiedCampaign: TextCampaign = copyValidCampaign(campaign)

        softly {
            assertThat(copiedCampaign.statusShow).isEqualTo(expectedStatusShow)
            assertThat(copiedCampaign.startDate).matchesExpectedDate(expectedStartDate)
            assertThat(copiedCampaign.endDate).matchesExpectedDate(expectedEndDate)
        }
    }

    /**
     * Небольшое допущение для того, чтобы тест был стабилен близко к границе суток:
     *
     * Мокать текущую дату сложно, особенно в глубине операции добавления кампании. Поэтому, в расчете на то, что
     * валидация даты кампании при копировании выключена, хотим гарантировать, что дата равна либо текущему дню, либо
     * предыдущему дню (если препроцессингу кампании повезло выполниться в предыдущую дату).
     */
    private fun LocalDateAssert.matchesExpectedDate(expected: LocalDate?) {
        if (expected == todayPlaceholder) {
            val now = LocalDate.now()
            isIn(now, now - Period.ofDays(1))
        } else {
            isEqualTo(expected)
        }
    }
}
