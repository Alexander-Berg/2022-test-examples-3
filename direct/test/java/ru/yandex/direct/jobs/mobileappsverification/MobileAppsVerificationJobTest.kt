package ru.yandex.direct.jobs.mobileappsverification

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.appmetrika.AppMetrikaClient
import ru.yandex.direct.appmetrika.model.Platform
import ru.yandex.direct.appmetrika.model.response.Application
import ru.yandex.direct.appmetrika.model.response.BundleId
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.client.service.ClientService
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppConversionStatisticRepository
import ru.yandex.direct.core.entity.mobileapp.repository.MobileAppRepository
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent
import ru.yandex.direct.core.entity.mobilecontent.repository.MobileContentRepository
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.MobileAppInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.jobs.configuration.JobsTest
import ru.yandex.direct.model.ModelChanges

@JobsTest
@ExtendWith(SpringExtension::class)
class MobileAppsVerificationJobTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var appMetrikaClient: AppMetrikaClient

    @Autowired
    private lateinit var mobileAppConversionStatisticRepository: MobileAppConversionStatisticRepository

    @Autowired
    private lateinit var mobileAppRepository: MobileAppRepository

    @Autowired
    private lateinit var mobileContentRepository: MobileContentRepository

    @Autowired
    private lateinit var clientService: ClientService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    private lateinit var mobileAppsVerificationJob: MobileAppsVerificationJob
    private lateinit var clientInfo: ClientInfo
    private lateinit var iosApp: MobileAppInfo
    private lateinit var androidApp: MobileAppInfo

    companion object {
        const val STORE_URL = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=ru.yandex.music"
        const val IOS_STORE_URL = "https://itunes.apple.com/ru/app/meduza/id921508170?mt=8"
        const val BUNDLE_ID = "com.bundle.app"
    }

    @BeforeEach
    fun init() {
        clientInfo = steps.clientSteps().createDefaultClient()
        iosApp = steps.mobileAppSteps()
            .createMobileApp(clientInfo, IOS_STORE_URL)
        androidApp = steps.mobileAppSteps()
            .createMobileApp(clientInfo, STORE_URL)

        mobileAppsVerificationJob = MobileAppsVerificationJob(
            mobileAppConversionStatisticRepository,
            mobileAppRepository,
            mobileContentRepository,
            clientService,
            appMetrikaClient,
            ppcPropertiesSupport
        )
        mobileAppsVerificationJob.withShard(clientInfo.shard)
    }

    @Test
    fun verifyExternal() {
        updateBundle(iosApp)
        val beforeState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .none { it.hasVerification }

        externalVerify()

        val afterState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .all { it.hasVerification }
        SoftAssertions().apply {
            assertThat(beforeState)
                .`as`("Состояние до запуска: нет верификации на контрольных примерах")
                .isEqualTo(true)
            assertThat(afterState)
                .`as`("Состояние после запуска: есть верификация на контрольных примерах")
                .isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun verifyMetrica() {
        updateBundle(iosApp)
        val beforeState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .none { it.hasVerification }

        val androidContent = mobileContentRepository
            .getMobileContent(androidApp.clientInfo.shard, androidApp.mobileContentId)

        doReturn(emptyList<Long>())
            .`when`(mobileAppConversionStatisticRepository)
            .getVerifiedAppIds(anyOrNull(), anyOrNull())


        doReturn(
            listOf(
                generateMetricaApp(BUNDLE_ID, Platform.ios, "app", iosApp.mobileAppId),
                generateMetricaApp(
                    androidContent.storeContentId, Platform.android, androidContent.name, androidContent.id)
            )
        ).`when`(appMetrikaClient)
            .getApplications(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())

        mobileAppsVerificationJob.execute()

        val afterState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .all { it.hasVerification }

        SoftAssertions().apply {
            assertThat(beforeState)
                .`as`("Состояние до запуска: нет верификации на контрольных примерах")
                .isEqualTo(true)
            assertThat(afterState)
                .`as`("Состояние до запуска: есть верификации на контрольных примерах")
                .isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun verifyExternal_notExternalData() {
        val beforeState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .none { it.hasVerification }

        doReturn(emptyList<Long>())
            .`when`(mobileAppConversionStatisticRepository)
            .getVerifiedAppIds(anyOrNull(), anyOrNull())

        doReturn(emptyList<Application>())
            .`when`(appMetrikaClient)
            .getApplications(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())

        mobileAppsVerificationJob.execute()

        val afterState = mobileAppRepository
            .getMobileApps(clientInfo.shard, clientInfo.clientId, listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .none { it.hasVerification }

        SoftAssertions().apply {
            assertThat(beforeState)
                .`as`("Состояние до запуска: нет верификации на контрольных примерах")
                .isEqualTo(true)
            assertThat(afterState)
                .`as`("Состояние после запуска: нет верификации на контрольных примерах")
                .isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun verifyByBundle() {
        val iosApp2 = steps.mobileAppSteps()
            .createMobileApp(clientInfo, "https://apps.apple.com/us/app/yandex-browser/id483693909")
        val androidApp2 = steps.mobileAppSteps()
            .createMobileApp(clientInfo, STORE_URL)

        updateBundle(iosApp)
        updateBundle(iosApp2)

        val beforeState = mobileAppRepository.getMobileApps(
            clientInfo.shard,
            clientInfo.clientId,
            listOf(iosApp.mobileAppId, androidApp.mobileAppId, iosApp2.mobileAppId, androidApp2.mobileAppId))
            .none { it.hasVerification }

        externalVerify()

        val afterState = mobileAppRepository
            .getMobileApps(clientInfo.shard,
                clientInfo.clientId,
                listOf(iosApp.mobileAppId, androidApp.mobileAppId, iosApp2.mobileAppId, androidApp2.mobileAppId))
            .all { it.hasVerification }

        SoftAssertions().apply {
            assertThat(beforeState)
                .`as`("Состояние до запуска: нет верификации на контрольных примерах")
                .isEqualTo(true)
            assertThat(afterState)
                .`as`("Состояние до запуска: есть верификации на контрольных примерах")
                .isEqualTo(true)
        }.assertAll()
    }

    private fun updateBundle(iosApp: MobileAppInfo) {
        val mc = mobileContentRepository.getMobileContent(iosApp.clientInfo.shard, iosApp.mobileContentId)
        val changes = ModelChanges(iosApp.mobileContentId, MobileContent::class.java)
        changes.process(BUNDLE_ID, MobileContent.BUNDLE_ID)
        val applied = changes.applyTo(mc)
        mobileContentRepository.updateMobileContent(iosApp.clientInfo.shard, listOf(applied))
    }

    private fun generateMetricaApp(
        bundleId: String,
        platform: Platform,
        name: String,
        id: Long
    ) {
        val bundle = BundleId()
        bundle.bundleId = bundleId
        bundle.platform = platform.toString()
        val iosAppMetrica = Application()
        iosAppMetrica.id = id
        iosAppMetrica.name = name
        iosAppMetrica.bundleIds = listOf(bundle)
    }

    private fun externalVerify() {
        doReturn(listOf(iosApp.mobileAppId, androidApp.mobileAppId))
            .`when`(mobileAppConversionStatisticRepository)
            .getVerifiedAppIds(anyOrNull(), anyOrNull())

        doReturn(emptyList<Application>())
            .`when`(appMetrikaClient)
            .getApplications(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())

        mobileAppsVerificationJob.execute()
    }
}
