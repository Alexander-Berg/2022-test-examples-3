package ru.yandex.direct.grid.processing.service.goal

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import ru.yandex.direct.common.testing.assertThatVerificationOk
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.service.MobileAppConversionStatisticService
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalConversionsInfo

class MobileGoalConversionsDataServiceTest {
    companion object {
        private val CLIENT_ID = ClientId.fromLong(222L)
        private const val MOBILE_APP_ID = 77438L
        private const val PERIOD = 7
    }

    private lateinit var conversionStatisticService: MobileAppConversionStatisticService
    private lateinit var mobileGoalConversionsDataService: MobileGoalConversionsDataService

    @Before
    fun before() {
        conversionStatisticService = mock()
        mobileGoalConversionsDataService = MobileGoalConversionsDataService(conversionStatisticService)
    }

    @Test
    fun emptyGoals() {
        val conversions = mobileGoalConversionsDataService.getMobileGoalConversions(
            CLIENT_ID, MOBILE_APP_ID, listOf(), PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(conversionStatisticService, never())
                    .getMobileGoalConversions(any(), anyLong(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun notMobileGoalsOnly() {
        val conversions = mobileGoalConversionsDataService.getMobileGoalConversions(
            CLIENT_ID, MOBILE_APP_ID, listOf(555L, 666L, 777L), PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(conversionStatisticService, never())
                    .getMobileGoalConversions(any(), anyLong(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun mobileWithoutStatistic() {
        givenThereIsNoAnyConversions()

        val conversions = mobileGoalConversionsDataService.getMobileGoalConversions(
            CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)

        softly {
            assertThat(conversions).containsExactlyInAnyOrder(
                GdMobileGoalConversionsInfo()
                    .withGoalId(7)
                    .withAttributed(false)
                    .withNotAttributed(false)
            )
            assertThatVerificationOk {
                verify(conversionStatisticService, only())
                    .getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)
            }
        }
    }

    @Test
    fun mobileWithAttributedConversions() {
        givenThereIsConversions(MobileGoalConversions(7L, 1, 0))

        val conversions = mobileGoalConversionsDataService.getMobileGoalConversions(
            CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)

        softly {
            assertThat(conversions).containsExactlyInAnyOrder(
                GdMobileGoalConversionsInfo()
                    .withGoalId(7)
                    .withAttributed(true)
                    .withNotAttributed(false)
            )
            assertThatVerificationOk {
                verify(conversionStatisticService, only())
                    .getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)
            }
        }
    }

    @Test
    fun mobileWithNotAttributedConversions() {
        givenThereIsConversions(MobileGoalConversions(7L, 0, 1))

        val conversions = mobileGoalConversionsDataService.getMobileGoalConversions(
            CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)

        softly {
            assertThat(conversions).containsExactlyInAnyOrder(
                GdMobileGoalConversionsInfo()
                    .withGoalId(7)
                    .withAttributed(false)
                    .withNotAttributed(true)
            )
            assertThatVerificationOk {
                verify(conversionStatisticService, only())
                    .getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, listOf(7L), PERIOD)
            }
        }
    }

    private fun givenThereIsNoAnyConversions() {
        whenever(conversionStatisticService.getMobileGoalConversions(any(), anyLong(), anyList(), anyInt()))
            .thenReturn(listOf())
    }

    private fun givenThereIsConversions(conversions: MobileGoalConversions) {
        whenever(conversionStatisticService.getMobileGoalConversions(any(), anyLong(), anyList(), anyInt()))
            .thenReturn(listOf(conversions))
    }
}
