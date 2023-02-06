package ru.yandex.direct.web.entity.uac.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.yandex.ydb.table.result.ValueReader
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept
import ru.yandex.direct.core.entity.uac.defaultAppInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.APP_INFO
import ru.yandex.direct.core.entity.uac.samples.ANDROID_APP_INFO_DATA
import ru.yandex.direct.core.entity.uac.samples.IOS_APP_INFO_DATA
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select
import ru.yandex.direct.ydb.column.Column


@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbAppInfoRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    private lateinit var uacYdbAppInfoRepository: UacYdbAppInfoRepository

    private lateinit var appInfo: UacYdbAppInfo

    @Before
    fun before() {
        appInfo = defaultAppInfo()
        uacYdbAppInfoRepository.saveAppInfo(appInfo)
    }

    @Test
    fun testGetAppInfoByIdNonExistent() {
        val nonExistentId = randomPositiveLong().toIdString()
        val actualAppInfo = uacYdbAppInfoRepository.getAppInfoById(nonExistentId)
        assertThat(actualAppInfo).isNull()
    }

    @Test
    fun testGetAppInfoById() {
        val actualAppInfo = uacYdbAppInfoRepository.getAppInfoById(appInfo.id)
        assertThat(actualAppInfo).`is`(
            matchedBy(
                beanDiffer(appInfo)
                    .useCompareStrategy(allFieldsExcept(newPath("data")))
            )
        )

        val objectMapper = ObjectMapper()
        val expectData = objectMapper.readValue(appInfo.data, MutableMap::class.java)
        val actualData = objectMapper.readValue(actualAppInfo?.data, MutableMap::class.java)
        assertThat(actualData).isEqualTo(expectData)
    }

    @Test
    @TestCaseName("testSaveAppInfo({0})")
    @Parameters(source = UacIdsProvider::class)
    fun testSaveAppInfo(caseName: String, id: String) {
        val appInfo = defaultAppInfo(id = id)

        uacYdbAppInfoRepository.saveAppInfo(appInfo)

        val actualAppInfo = uacYdbAppInfoRepository.getAppInfoById(appInfo.id)
        assertThat(actualAppInfo).isNotNull
    }

    fun providePlatforms() = arrayOf(
        arrayOf(Platform.ANDROID, Store.GOOGLE_PLAY, ANDROID_APP_INFO_DATA),
        arrayOf(Platform.IOS, Store.ITUNES, IOS_APP_INFO_DATA),
    )

    @Test
    @TestCaseName("testSaveAppInfoPlatforms({0}, {1})")
    @Parameters(method = "providePlatforms")
    fun testSaveAppInfoPlatforms(platform: Platform, store: Store, data: String) {
        val appInfo = defaultAppInfo(
            platform = platform,
            source = store,
            data = data,
        )

        uacYdbAppInfoRepository.saveAppInfo(appInfo)

        val actualAppInfo = uacYdbAppInfoRepository.getAppInfoById(appInfo.id)
        assertThat(actualAppInfo).isNotNull
    }

    @Test
    @TestCaseName("testSaveAppInfoPlatforms({0}, {1})")
    @Parameters(method = "providePlatforms")
    fun testSaveAppInfoEnumValues(platform: Platform, source: Store, data: String) {
        val appInfo = defaultAppInfo(
            platform = platform,
            source = source,
            data = data,
        )

        uacYdbAppInfoRepository.saveAppInfo(appInfo)

        val actualPlatform = getSavedValue(appInfo.id, APP_INFO.PLATFORM).uint32.toInt()
        val actualSource = getSavedValue(appInfo.id, APP_INFO.SOURCE).uint32.toInt()

        assertSoftly {
            it.assertThat(actualPlatform).isEqualTo(platform.id)
            it.assertThat(actualSource).isEqualTo(source.id)
        }
    }

    private fun <T> getSavedValue(id: String, column: Column<T>): ValueReader {
        val query = select(column)
            .from(APP_INFO)
            .where(APP_INFO.ID.eq(id.toIdLong()))
            .queryAndParams(path)
        val result = ydbClient.executeQuery(query).getResultSet(0)
        result.next()
        return result.getValueReader(column)
    }
}
