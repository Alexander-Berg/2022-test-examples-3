package ru.yandex.direct.core.entity.mobileapp.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.only
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import ru.yandex.direct.common.testing.assertThatVerificationOk
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.model.OsType
import ru.yandex.direct.dbutil.model.ClientId
import java.util.Optional

class MobileAppConversionStatisticServiceTest {
    companion object {
        private val CLIENT_ID = ClientId.fromLong(222L)
        private const val MOBILE_APP_ID = 77438L
        private val DIRECT_GOAL_IDS = listOf(3L, 4L)
        private const val PERIOD = 7
        private const val BS_APP_ID = "com.bbb.ccc.aaa"
    }

    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository
    private lateinit var mobileAppService: MobileAppService
    private lateinit var service: MobileAppConversionStatisticService

    @Before
    fun setUp() {
        mobileAppConversionStatisticRepository = mock()
        mobileAppService = mock()
        service = MobileAppConversionStatisticService(mobileAppConversionStatisticRepository, mobileAppService)
    }

    @Test
    fun notFoundApp_ReturnEmptyList() {
        givenMobileAppNotFound()

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, never())
                    .getConversionStats(anyString(), anyString(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun mobileContentWithNullInOsType_ReturnEmptyList() {
        givenMobileAppWithMobileContent(
            MobileContent().withStoreContentId("xxx").withBundleId("xxx"))

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, never())
                    .getConversionStats(anyString(), anyString(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun mobileContentIosWithNullInBundleId_ReturnEmptyList() {
        givenMobileAppWithMobileContent(
            MobileContent().withOsType(OsType.IOS).withStoreContentId("xxx"))

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, never())
                    .getConversionStats(anyString(), anyString(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun mobileContentAndroidWithNullInStoreContentId_ReturnEmptyList() {
        givenMobileAppWithMobileContent(
            MobileContent().withOsType(OsType.ANDROID).withBundleId("xxx"))

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).isEmpty()
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, never())
                    .getConversionStats(anyString(), anyString(), anyList(), anyInt())
            }
        }
    }

    @Test
    fun validIosMobileApp_ReturnStat() {
        givenMobileAppWithMobileContent(
            MobileContent().withOsType(OsType.IOS).withBundleId(BS_APP_ID))
        givenRepositoryReturnsStat()

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).containsExactlyInAnyOrder(
                MobileGoalConversions(goalId = 3, attributedConversions = 3, notAttributedConversions = 33),
                MobileGoalConversions(goalId = 4, attributedConversions = 4, notAttributedConversions = 44),
            )
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, only())
                    .getConversionStats(BS_APP_ID, "ios", DIRECT_GOAL_IDS, PERIOD)
            }
        }
    }

    @Test
    fun validAndroidMobileApp_ReturnStat() {
        givenMobileAppWithMobileContent(
            MobileContent().withOsType(OsType.ANDROID).withStoreContentId(BS_APP_ID))
        givenRepositoryReturnsStat()

        val conversions = service.getMobileGoalConversions(CLIENT_ID, MOBILE_APP_ID, DIRECT_GOAL_IDS, PERIOD)

        softly {
            assertThat(conversions).containsExactlyInAnyOrder(
                MobileGoalConversions(goalId = 3, attributedConversions = 3, notAttributedConversions = 33),
                MobileGoalConversions(goalId = 4, attributedConversions = 4, notAttributedConversions = 44),
            )
            assertThatVerificationOk {
                verify(mobileAppConversionStatisticRepository, only())
                    .getConversionStats(BS_APP_ID, "android", DIRECT_GOAL_IDS, PERIOD)
            }
        }
    }

    private fun givenMobileAppWithMobileContent(mobileContent: MobileContent) {
        whenever(mobileAppService.getMobileApp(CLIENT_ID, MOBILE_APP_ID)).thenReturn(Optional.of(
            MobileApp().withMobileContent(mobileContent)
        ))
    }

    private fun givenMobileAppNotFound() {
        whenever(mobileAppService.getMobileApp(CLIENT_ID, MOBILE_APP_ID)).thenReturn(Optional.empty())
    }

    private fun givenRepositoryReturnsStat() {
        whenever(mobileAppConversionStatisticRepository.getConversionStats(anyString(), anyString(), anyList(), anyInt()))
            .thenReturn(listOf(
                MobileGoalConversions(goalId = 3, attributedConversions = 3, notAttributedConversions = 33),
                MobileGoalConversions(goalId = 4, attributedConversions = 4, notAttributedConversions = 44),
            ))
    }
}
