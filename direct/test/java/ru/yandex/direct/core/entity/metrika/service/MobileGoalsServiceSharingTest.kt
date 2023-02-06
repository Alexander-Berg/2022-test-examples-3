package ru.yandex.direct.core.entity.metrika.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.mobileapp.model.ExternalTrackerEventName
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.mobileapp.model.MobileExternalTrackerEvent
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.retargeting.model.GoalType
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestGroups.getDefaultStoreHref
import ru.yandex.direct.core.testing.data.TestMobileContents.androidMobileContent
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.info.MobileContentInfo
import ru.yandex.direct.core.testing.steps.ClientSteps.Companion.ANOTHER_SHARD
import ru.yandex.direct.core.testing.steps.ClientSteps.Companion.DEFAULT_SHARD
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName.IN_APP_MOBILE_TARGETING
import ru.yandex.direct.feature.FeatureName.IN_APP_MOBILE_TARGETING_CUSTOM_EVENTS_FOR_EXTERNAL_TRACKERS
import ru.yandex.direct.rbac.RbacClientsRelations

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class MobileGoalsServiceSharingTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var mobileGoalsService: MobileGoalsService

    @Autowired
    private lateinit var rbacClientsRelations: RbacClientsRelations

    private lateinit var ownerOfMobileGoalsClient: ClientInfo
    private lateinit var consumerOfMobileGoalsClient: ClientInfo
    private lateinit var unremarkableClient: ClientInfo

    private lateinit var mobileAppInfo: MobileAppInfo

    @Before
    fun before() {
        ownerOfMobileGoalsClient = createClientWithInAppMobileTargetingEnabled()  // владелец приложения и целей
        mobileAppInfo = clientHasAndroidMobileAppWithConfiguredEvents(ownerOfMobileGoalsClient)

        consumerOfMobileGoalsClient =
            createClientWithInAppMobileTargetingEnabled(shard = ANOTHER_SHARD)  // видит цели владельца приложений
        rbacClientsRelations.addMobileGoalsAccessRelations(
            listOf(consumerOfMobileGoalsClient.clientId!!), ownerOfMobileGoalsClient.clientId!!
        )

        unremarkableClient = createClientWithInAppMobileTargetingEnabled()  // мимо проходил
    }

    @Test
    fun ownerOfMobileGoalsSeesMobileGoals() {
        val mobileGoals = mobileGoalsService.getAllAvailableInAppMobileGoals(ownerOfMobileGoalsClient.clientId)
        assertThat(mobileGoals).containsGoalsOfMobileApplication(mobileAppInfo)
    }

    @Test
    fun consumerOfMobileGoalsSeesSharedMobileGoals() {
        val mobileGoals = mobileGoalsService.getAllAvailableInAppMobileGoals(consumerOfMobileGoalsClient.clientId)
        assertThat(mobileGoals).containsGoalsOfMobileApplication(mobileAppInfo)
    }

    @Test
    fun unremarkableClientDoesNotSeeAnyMobileGoals() {
        val mobileGoals = mobileGoalsService.getAllAvailableInAppMobileGoals(unremarkableClient.clientId)
        assertThat(mobileGoals).isEmpty()
    }

    private fun createClientWithInAppMobileTargetingEnabled(shard: Int = DEFAULT_SHARD): ClientInfo {
        val clientInfo = steps.clientSteps().createClient(ClientInfo().withShard(shard))
        steps.featureSteps().addClientFeature(clientInfo.clientId, IN_APP_MOBILE_TARGETING, true)
        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, IN_APP_MOBILE_TARGETING_CUSTOM_EVENTS_FOR_EXTERNAL_TRACKERS, true)
        return clientInfo
    }

    private fun clientHasAndroidMobileAppWithConfiguredEvents(clientInfo: ClientInfo): MobileAppInfo {
        val mobileContentInfo = steps.mobileContentSteps().createMobileContent(
            MobileContentInfo()
                .withClientInfo(clientInfo)
                .withMobileContent(androidMobileContent())
        )

        val mobileApp = MobileApp()
            .withAppMetrikaApplicationId(null)
            .withMobileAppMetrikaEvents(emptyList())
            .withMobileExternalTrackerEvents(
                listOf(
                    MobileExternalTrackerEvent()
                        .withEventName(ExternalTrackerEventName.EVENT_1)
                        .withCustomName("custom name 1")
                        .withIsDeleted(false),
                    MobileExternalTrackerEvent()
                        .withEventName(ExternalTrackerEventName.EVENT_2)
                        .withCustomName("some trackable application event")
                        .withIsDeleted(false),
                    MobileExternalTrackerEvent()
                        .withEventName(ExternalTrackerEventName.EVENT_3)
                        .withCustomName("some deleted event")
                        .withIsDeleted(true),
                )
            )
            .withName("Some mobile application name")
            .withStoreHref(getDefaultStoreHref(mobileContentInfo.mobileContent))
            .withDisplayedAttributes(emptySet())
            .withTrackers(emptyList())

        val mobileAppInfo = MobileAppInfo()
            .withMobileContentInfo(mobileContentInfo)
            .withMobileApp(mobileApp)
        steps.mobileAppSteps().createMobileApp(
            mobileAppInfo
        )
        return mobileAppInfo
    }

}


private fun ListAssert<Goal>.containsGoalsOfMobileApplication(mobileAppInfo: MobileAppInfo) {
    val expectedGoals: List<Goal> = mobileAppInfo.mobileApp.mobileExternalTrackerEvents
        .filter { !it.isDeleted }
        .map {
            Goal().apply {
                type = GoalType.MOBILE
                mobileAppId = mobileAppInfo.mobileAppId
                mobileAppName = mobileAppInfo.mobileApp.name
                name = it.customName
            }
        }
    this.usingRecursiveComparison()
        .ignoringCollectionOrder()
        .ignoringExpectedNullFields()
        .isEqualTo(expectedGoals)
}

