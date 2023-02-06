package ru.yandex.direct.grid.processing.service.group

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import ru.yandex.direct.common.util.HttpUtil.GDPR
import ru.yandex.direct.common.util.HttpUtil.HEADER_X_REAL_IP
import ru.yandex.direct.common.util.HttpUtil.IS_GDPR
import ru.yandex.direct.common.util.HttpUtil.YANDEX_GID
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.geobasehelper.GeoBaseHelperStub
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.regions.Region.MOSCOW_REGION_ID
import ru.yandex.direct.regions.Region.RUSSIA_REGION_ID
import ru.yandex.direct.regions.Region.SAINT_PETERSBURG_REGION_ID
import ru.yandex.direct.regions.Region.SOUTH_FEDERAL_DISTRICT_REGION_ID
import ru.yandex.direct.regions.Region.VORONEZH_OBLAST_REGION_ID
import ru.yandex.direct.test.utils.checkEquals
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest

private const val IP = "255.1.1.1"

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupGraphQLServiceGeoSuggestTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var geoBaseHelperStub: GeoBaseHelperStub

    private lateinit var userInfo: UserInfo
    private lateinit var request: HttpServletRequest

    @Before
    fun setUp() {
        userInfo = steps.userSteps().createDefaultUser()
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)

        request = mock(HttpServletRequest::class.java)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @Test
    fun getGeoSuggest_GdprClientWithNoIp() {
        `when`(request.cookies).thenReturn(arrayOf(Cookie(IS_GDPR, "1")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun getGeoSuggest_GdprClient2WithNoIp() {
        `when`(request.cookies).thenReturn(arrayOf(Cookie(GDPR, "1")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun getGeoSuggest_GdprClient3WithNoIp() {
        `when`(request.cookies).thenReturn(arrayOf(Cookie(GDPR, "2")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun getGeoSuggest_ProbablyGdprClientWithNoIp() {
        `when`(request.cookies).thenReturn(arrayOf())

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun getGeoSuggest_GdprClientWithIp() {
        geoBaseHelperStub.addRegionIdWithIp(VORONEZH_OBLAST_REGION_ID, IP)
        `when`(request.getHeader(eq(HEADER_X_REAL_IP))).thenReturn(IP)
        `when`(request.cookies).thenReturn(arrayOf())

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(VORONEZH_OBLAST_REGION_ID, MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun getGeoSuggest_NonGdprClientWithIp() {
        geoBaseHelperStub.addRegionIdWithIp(VORONEZH_OBLAST_REGION_ID, IP)
        `when`(request.getHeader(eq(HEADER_X_REAL_IP))).thenReturn(IP)
        `when`(request.cookies).thenReturn(arrayOf(
            Cookie(IS_GDPR, "0"),
            Cookie(YANDEX_GID, "$SOUTH_FEDERAL_DISTRICT_REGION_ID")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(SOUTH_FEDERAL_DISTRICT_REGION_ID, MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun geoGeoSuggest_NonGdprClientFromSpb() {
        `when`(request.cookies).thenReturn(arrayOf(
            Cookie(IS_GDPR, "0"),
            Cookie(YANDEX_GID, "$SAINT_PETERSBURG_REGION_ID")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(SAINT_PETERSBURG_REGION_ID, MOSCOW_REGION_ID, RUSSIA_REGION_ID))
    }

    @Test
    fun geoGeoSuggest_NonGdprClientFromMoscow() {
        `when`(request.cookies).thenReturn(arrayOf(
            Cookie(IS_GDPR, "0"),
            Cookie(YANDEX_GID, "$MOSCOW_REGION_ID")))

        ktGraphQLTestExecutor.getGeoSuggest(userInfo.login)
            .checkEquals(listOf(MOSCOW_REGION_ID, SAINT_PETERSBURG_REGION_ID, RUSSIA_REGION_ID))
    }
}
