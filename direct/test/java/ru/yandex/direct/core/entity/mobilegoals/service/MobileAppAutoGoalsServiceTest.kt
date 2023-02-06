package ru.yandex.direct.core.entity.mobilegoals

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.eq
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent
import ru.yandex.direct.core.entity.mobileapp.model.MobileGoalConversions
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository

class MobileAppAutoGoalsServiceTest {

    companion object {
        private val APP1_ID = 1L
        private val APP2_ID = 2L
        private val NOT_EXISTING_APP_ID = 3L
        private val NOT_EXISTING_GOAL_ID = 4L
        private val SPENT_CREDITS_GOAL_ID = 38403206L
        private val ADDED_TO_CART_GOAL_ID = 38403071L
        private val APP_LAUNCHED_GOAL_ID = 38403008L
        private val EVENT_2_GOAL_ID = 38403545L
    }

    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository
    private lateinit var mobileAppRepository: MobileAppRepository
    private lateinit var mobileAppGoalsExternalTrackerRepository: MobileAppGoalsExternalTrackerRepository
    private lateinit var service: MobileAppAutoGoalsService

    @Before
    fun setUp() {
        mobileAppConversionStatisticRepository = mock()
        mobileAppRepository = mock()
        mobileAppGoalsExternalTrackerRepository = mock()
        service = MobileAppAutoGoalsService(
            mobileAppConversionStatisticRepository,
            mobileAppRepository,
            mobileAppGoalsExternalTrackerRepository
        )
    }

    @Test
    fun getAutoGoalsToAddTest() {
        givenRepositoryConversions()
        givenRepositoryMobileApps()
        givenRepositoryExternalTrackerEvents()

        val events = service.getAutoGoalsToAdd(1, 1)

        softly {
            assertThat(events).containsExactlyInAnyOrder(
                MobileExternalTrackerEvent().withMobileAppId(APP1_ID).withEventName(ExternalTrackerEventName.SPENT_CREDITS).withCustomName("").withIsDeleted(false),
                MobileExternalTrackerEvent().withMobileAppId(APP2_ID).withEventName(ExternalTrackerEventName.ADDED_TO_CART).withCustomName("").withIsDeleted(false),
                MobileExternalTrackerEvent().withMobileAppId(APP2_ID).withEventName(ExternalTrackerEventName.EVENT_2).withCustomName("").withIsDeleted(false),
            )
        }
    }

    private fun givenRepositoryConversions() {
        whenever(mobileAppConversionStatisticRepository.getConversionStatsByAppId(anyInt()))
            .thenReturn(mapOf(
                APP1_ID to listOf(
                    MobileGoalConversions(goalId = NOT_EXISTING_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                    MobileGoalConversions(goalId = SPENT_CREDITS_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                ),
                APP2_ID to listOf(
                    MobileGoalConversions(goalId = ADDED_TO_CART_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                    MobileGoalConversions(goalId = APP_LAUNCHED_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                    MobileGoalConversions(goalId = EVENT_2_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                ),
                NOT_EXISTING_APP_ID to listOf(
                    MobileGoalConversions(goalId = SPENT_CREDITS_GOAL_ID, attributedConversions = 1, notAttributedConversions = 1),
                ),
            ))
    }

    private fun givenRepositoryMobileApps() {
        whenever(mobileAppRepository.getMobileAppsWithoutRelations(anyInt(), anyCollection()))
            .thenReturn(listOf(
                MobileApp().withId(APP1_ID),
                MobileApp().withId(APP2_ID),
            ))
    }

    private fun givenRepositoryExternalTrackerEvents() {
        whenever(mobileAppGoalsExternalTrackerRepository.getEventsByAppIds(anyInt(), anyCollection(), eq(false)))
            .thenReturn(listOf(
                MobileExternalTrackerEvent().withMobileAppId(APP1_ID).withEventName(ExternalTrackerEventName.SHARED),
                MobileExternalTrackerEvent().withMobileAppId(APP2_ID).withEventName(ExternalTrackerEventName.APP_LAUNCHED),
            ))
    }
}
