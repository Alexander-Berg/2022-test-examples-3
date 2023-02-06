package ru.yandex.direct.web.entity.uac.service

import com.nhaarman.mockitokotlin2.doReturn
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.appmetrika.AppMetrikaClient
import ru.yandex.direct.appmetrika.model.response.Application
import ru.yandex.direct.appmetrika.model.response.BundleId
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.service.UacAppInfoService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacMobileAppServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var uacSuggestService: UacSuggestService

    @Autowired
    private lateinit var uacAppMetrikaService: UacAppMetrikaService

    @Autowired
    private lateinit var appMetrikaClient: AppMetrikaClient

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAppInfoService: UacAppInfoService

    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        userInfo = steps.userSteps().createDefaultUser()
    }

    @Test
    fun twoMobileAppsWithSameBundleId() {
        doReturn(
            listOf(
                Application().apply {
                    bundleIds = listOf(
                        BundleId().apply {
                            bundleId = "com.yandex.some"
                            platform = "ios"
                        },
                    )
                }
            )
        ).`when`(appMetrikaClient).getApplications(
            userInfo.uid, null, null, null, 100, null
        )
        val androidAppInfo = defaultAppInfo(
            appId = "com.yandex.some", bundleId = "com.yandex.some",
            platform = Platform.ANDROID, source = Store.GOOGLE_PLAY,
            data = ANDROID_APP_INFO_DATA
        )
        val iosAppInfo = defaultAppInfo(
            appId = "app123", bundleId = "com.yandex.some",
            platform = Platform.IOS, source = Store.ITUNES,
            data = IOS_APP_INFO_DATA
        )
        listOf(androidAppInfo, iosAppInfo).forEach { uacYdbAppInfoRepository.saveAppInfo(it) }

        uacSuggestService.suggestAppMetrikaApps(userInfo.user!!, null)
            .checkEquals(listOf(uacAppInfoService.getAppInfo(iosAppInfo)))
    }
}
