package ru.yandex.direct.web.entity.uac.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import ru.yandex.direct.core.entity.campaign.service.CampMetrikaCountersService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.metrika.service.campaigngoals.CampaignGoalsService
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbTrackerUrlStatRepository
import ru.yandex.direct.dbutil.model.ClientId

@RunWith(JUnitParamsRunner::class)
class UacGoalsServiceGetAvailableCountersTest {
    private val campMetrikaCountersService: CampMetrikaCountersService =
        Mockito.mock(CampMetrikaCountersService::class.java)

    private val uacGoalsService = UacGoalsService(
        campMetrikaCountersService,
        Mockito.mock(CampaignGoalsService::class.java),
        Mockito.mock(MetrikaGoalsService::class.java),
        Mockito.mock(FeatureService::class.java),
        Mockito.mock(UacYdbTrackerUrlStatRepository::class.java),
        Mockito.mock(MobileAppConversionStatisticRepository::class.java),
        Mockito.mock(UacPropertiesService::class.java)
    )

    fun testParameters() = arrayOf(
        arrayOf("Запрашивается один доступный счетчик", listOf(1L), setOf(1L), emptySet<Long>(), setOf(1)),
        arrayOf("Запрашивается два доступных счетчика", listOf(1L, 2L), setOf(1L, 2L), emptySet<Long>(), setOf(1, 2)),
        arrayOf(
            "Запрашивается один недоступный, но разрешенный счетчик",
            listOf(1L),
            emptySet<Long>(),
            setOf(1L),
            setOf(1)
        ),
        arrayOf(
            "Запрашиваются недоступные счетчики",
            listOf(1L, 2L),
            emptySet<Long>(),
            emptySet<Long>(),
            emptySet<Int>()
        ),
        arrayOf(
            "Запрашивается один доступный, один недоступный, но разрешенный и один недоступный счетчик",
            listOf(1L, 2L, 3L),
            setOf(1L),
            setOf(2L),
            setOf(1, 2)
        )
    )

    @Test
    @TestCaseName("{0}")
    @Parameters(method = "testParameters")
    fun getAvailableCountersTest(
        description: String,
        requestedCounterIds: List<Long>,
        availableCounterIds: Set<Long>,
        allowedInaccessibleCounterIds: Set<Long>,
        expectedAvailableCounterIds: Set<Int>
    ) {
        doReturn(availableCounterIds).`when`(campMetrikaCountersService)
            .getAvailableCounterIdsByClientId(any(), any())
        doReturn(allowedInaccessibleCounterIds).`when`(campMetrikaCountersService)
            .getAllowedInaccessibleCounterIds(any())

        val availableCounters = uacGoalsService.getAvailableCounters(ClientId.fromLong(1L), requestedCounterIds)
        Assertions.assertThat(availableCounters).containsExactlyInAnyOrderElementsOf(expectedAvailableCounterIds)
    }
}
