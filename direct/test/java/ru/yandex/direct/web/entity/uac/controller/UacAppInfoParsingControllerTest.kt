package ru.yandex.direct.web.entity.uac.controller

import com.google.protobuf.util.JsonFormat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.whenever
import io.netty.handler.codec.http.HttpHeaderNames.LOCATION
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.asynchttpclient.Response
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.direct.asynchttp.Result
import ru.yandex.direct.avatars.client.AvatarsClient
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool
import ru.yandex.direct.core.entity.uac.avatarInfo
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.RecommendedCostType
import ru.yandex.direct.core.entity.uac.model.Store
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAppInfoRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAppInfo
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbRecommendedCost
import ru.yandex.direct.core.entity.uac.service.appinfo.ParseAppStoreContentService.ParseAppStoreContentException
import ru.yandex.direct.core.entity.uac.service.appinfo.ParsedAppStoreContent
import ru.yandex.direct.core.entity.uac.service.appinfo.ParsedGooglePlayContent
import ru.yandex.direct.core.entity.uac.service.appinfo.ParsedItunesContent
import ru.yandex.direct.core.entity.uac.thumbUrl
import ru.yandex.direct.core.entity.uac.validation.invalidAppId
import ru.yandex.direct.core.entity.uac.validation.invalidAppUrl
import ru.yandex.direct.core.entity.uac.validation.invalidGooglePlayUrl
import ru.yandex.direct.core.entity.uac.validation.invalidItunesUrl
import ru.yandex.direct.core.entity.zora.ZoraService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.repository.TestUacYdbAppInfoRepository
import ru.yandex.direct.core.testing.repository.TestUacYdbRecommendedCostRepository
import ru.yandex.direct.utils.JsonUtils
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.web.common.getValue
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper
import ru.yandex.direct.web.core.security.DirectWebAuthenticationSource
import ru.yandex.direct.web.entity.uac.model.ParseAppInfoRequest
import ru.yandex.direct.web.validation.model.WebValidationResult
import ru.yandex.direct.zorafetcher.ZoraResponse
import ru.yandex.grut.client.GrutClient

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacAppInfoParsingControllerTest {
    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var testAuthHelper: TestAuthHelper

    @Autowired
    private lateinit var directWebAuthenticationSource: DirectWebAuthenticationSource

    @Autowired
    private lateinit var appInfoRepository: UacYdbAppInfoRepository

    @Autowired
    private lateinit var zoraService: ZoraService

    @Autowired
    private lateinit var grutClient: GrutClient

    @Autowired
    private lateinit var uacYdbRecommendedCostRepository: TestUacYdbRecommendedCostRepository

    @Autowired
    private lateinit var testUacYdbAppInfoRepository: TestUacYdbAppInfoRepository

    @Autowired
    private lateinit var uacAvatarsClientPool: AvatarsClientPool

    private lateinit var avatarsClient: AvatarsClient
    private lateinit var userInfo: UserInfo

    @Before
    fun before() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
        userInfo = testAuthHelper.createDefaultUser()
        TestAuthHelper.setSecurityContextWithAuthentication(
            directWebAuthenticationSource.authentication
        )
        avatarsClient = uacAvatarsClientPool.defaultClient
        whenever(avatarsClient.uploadByUrl(any(), any())).thenReturn(avatarInfo)
        whenever(avatarsClient.getReadUrl(any(), any())).thenReturn(thumbUrl)
    }

    @After
    fun after() {
        uacYdbRecommendedCostRepository.clean()
        testUacYdbAppInfoRepository.clean()
        reset(zoraService)
    }

    private fun testFromCache(
        cached: UacYdbAppInfo,
        requestUrl: String,
        expectedJson: String
    ) {
        val request = ParseAppInfoRequest(
            url = requestUrl,
            cacheTtl = -1
        )
        appInfoRepository.saveAppInfo(cached)
        val resp = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/app_info/parsing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString
        assertThat(JsonUtils.MAPPER.readTree(resp))
            .isEqualTo(JsonUtils.MAPPER.readTree(expectedJson))
    }

    @Test
    fun itunesFromCache() {
        val cached = UacYdbAppInfo(
            id = "15026316425640756135",
            appId = "id123",
            bundleId = "bundleId123",
            region = "ru",
            language = "ru",
            platform = Platform.IOS,
            source = Store.ITUNES,
            data = ITUNES_APP_INFO_DATA,
        )

        testFromCache(
            cached = cached,
            requestUrl = "https://apps.apple.com/ru/app/id123",
            expectedJson = ITUNES_DB_EXPECTED_APP_INFO
        )
    }

    @Test
    fun googlePlayFromCache() {
        val cached = UacYdbAppInfo(
            id = "4791608187873723201",
            appId = "app.yandex.some",
            bundleId = "app.yandex.some",
            region = "ru",
            language = "ru",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = GP_APP_INFO_DATA
        )

        testFromCache(
            cached = cached,
            requestUrl = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.some",
            expectedJson = GP_DB_EXPECTED_APP_INFO
        )
    }

    @Test
    fun fromTrackingUrl() {
        val trackingUrl =
            "https://control.kochava.com/v1/cpi/click?campaign_id=koidtandroidus205652e7fe894452f471b27363e9ea" +
                "&network_id=221&click_id={logid}&site_id=p277-s322-"
        val appUrl = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.other"

        val resp = mock<Response> {
            on { getHeader(LOCATION) } doReturn appUrl
        }
        val zoraResponse = mock<ZoraResponse> {
            on { isRedirect } doReturn true
            on { response } doReturn resp
        }
        val result = Result<ZoraResponse>(0)
        result.success = zoraResponse
        doReturn(result)
            .whenever(zoraService)
            .fetchByUrl(any<String>(), eq(false))

        val cached = UacYdbAppInfo(
            id = "3699902402152278654",
            appId = "app.yandex.other",
            bundleId = "app.yandex.other",
            region = "ru",
            language = "ru",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = GP_APP_INFO_DATA
        )

        testFromCache(
            cached = cached,
            requestUrl = trackingUrl,
            expectedJson = GP_TRACKING_EXPECTED_APP_INFO
        )
    }

    @Test
    fun withRecommended() {
        val recommendedCpa = UacYdbRecommendedCost(
            id = UacYdbUtils.generateUniqueRandomId(),
            platform = Platform.ANDROID,
            recommendedCost = 123,
            type = RecommendedCostType.CPA,
            category = "application",
        )
        val recommendedCpi = UacYdbRecommendedCost(
            id = UacYdbUtils.generateUniqueRandomId(),
            platform = Platform.ANDROID,
            recommendedCost = 999,
            type = RecommendedCostType.CPI,
            category = "application",
        )
        uacYdbRecommendedCostRepository.addRecommendedCost(recommendedCpa)
        uacYdbRecommendedCostRepository.addRecommendedCost(recommendedCpi)

        val cached = UacYdbAppInfo(
            id = "17620644518839316272",
            appId = "app.yandex.yet.another",
            bundleId = "app.yandex.yet.another",
            region = "ru",
            language = "ru",
            platform = Platform.ANDROID,
            source = Store.GOOGLE_PLAY,
            data = GP_APP_INFO_DATA
        )
        testFromCache(
            cached = cached,
            requestUrl = "https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.yet.another",
            expectedJson = GP_RECOMMENDED_EXPECTED
        )
    }

    fun googlePlayParameters() = arrayOf(
        arrayOf(
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=ru&gl=ru",
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=ru&gl=ru"
        ),
        arrayOf(
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=ru",
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=ru&gl=ru"
        ),
        arrayOf(
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&gl=ru",
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&gl=ru&hl=ru"
        ),
        arrayOf(
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox",
            "https://play.google.com/store/apps/details?id=org.mozilla.firefox&hl=ru&gl=ru"
        ),
    )

    @Test
    @TestCaseName("Get app info with {0} google play url")
    @Parameters(method = "googlePlayParameters")
    fun googlePlayFromParser(
        originalAppUrl: String,
        fixedAppUrl: String,
    ) {
        val parsed = with(ParsedGooglePlayContent.newBuilder()) {
            JsonFormat.parser().merge(GP_PARSED_DATA, this)
            build()
        }
        whenever(grutClient.parseAppStoreContent(any()))
            .doReturn(ParsedAppStoreContent.newBuilder().setGooglePlay(parsed).build())

        val zoraResponseEmpty = zoraResponse("")
        doReturn(zoraResponseEmpty)
            .whenever(zoraService)
            .fetchByUrl(fixedAppUrl, true)

        val request = ParseAppInfoRequest(
            url = originalAppUrl,
            cacheTtl = 0
        )
        val parsingResp = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/app_info/parsing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val actualTree = JsonUtils.MAPPER.readTree(parsingResp)
        val id: String = actualTree.getValue("app_info/id")

        assertThat(actualTree)
            .isEqualTo(JsonUtils.MAPPER.readTree(GP_PARSED_EXPECTED_APP_INFO))
    }

    @Test
    fun itunesFromParser() {
        val appUrl = "https://apps.apple.com/ru/app/resurfer/id1489340046"
        val lookupUrl = "https://itunes.apple.com/lookup?id=1489340046&country=RU&l=RU"

        val parsed = with(ParsedItunesContent.newBuilder()) {
            JsonFormat.parser().merge(ITUNES_APP_INFO_ADDITIONAL, this)
            build()
        }
        whenever(grutClient.parseAppStoreContent(any()))
            .doReturn(ParsedAppStoreContent.newBuilder().setItunes(parsed).build())
        val zoraResponseEmpty = zoraResponse("")
        doReturn(zoraResponseEmpty)
            .whenever(zoraService)
            .fetchByUrl(appUrl, true)
        val zoraLookupResponse = zoraResponse(ITUNES_APP_INFO_LOOKUP)
        whenever(zoraService.fetchByUrl(lookupUrl, true))
            .doReturn(zoraLookupResponse)

        val request = ParseAppInfoRequest(
            url = appUrl,
            cacheTtl = 0
        )
        val parsingResp = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/app_info/parsing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
        ).andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
            .response
            .contentAsString

        val actualTree = JsonUtils.MAPPER.readTree(parsingResp)
        val id: String = actualTree.getValue("app_info/id")

        assertThat(actualTree)
            .isEqualTo(JsonUtils.MAPPER.readTree(ITUNES_PARSED_EXPECTED_APP_INFO))
    }

    private fun testBadRequest(requestUrl: String, defect: Defect<Void>) {
        val request = ParseAppInfoRequest(
            url = requestUrl,
            cacheTtl = 0
        )
        val resp = mockMvc.perform(
            MockMvcRequestBuilders
                .post("/uac/app_info/parsing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtils.toJson(request))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andReturn()
            .response
            .contentAsString

        val validationResult =
            JsonUtils.fromJson(
                JsonUtils.fromJson(resp)["validation_result"].toString(),
                WebValidationResult::class.java
            )

        val soft = SoftAssertions()
        soft.assertThat(validationResult.errors).hasSize(1)
        soft.assertThat(validationResult.errors[0].code)
            .isEqualTo(defect.defectId().code)
        soft.assertAll()
    }

    @Test
    fun rejectBadUrls() {
        val urls = listOf(
            "abcdef", ""
        )
        for (url in urls) {
            testBadRequest(url, invalidAppUrl())
        }
    }

    @Test
    fun rejectBadGooglePlayUrl() {
        val url = "https://play.google.com"
        testBadRequest(url, invalidGooglePlayUrl())
    }

    @Test
    fun rejectBadAppleUrl() {
        val url = "https://apps.apple.com"
        testBadRequest(url, invalidItunesUrl())
    }

    @Test
    fun rejectInvalidId() {
        val url = "https://play.google.com/store/apps/details?hl=ru"
        testBadRequest(url, invalidAppId())
    }

    @Test
    fun reject404Urls() {
        val appUrl = "https://play.google.com/store/apps/details?id=com.test&hl=ru&gl=ru"
        whenever(zoraService.fetchByUrl(eq(appUrl), eq(true)))
            .doThrow(ParseAppStoreContentException(""))

        testBadRequest(appUrl, invalidAppUrl())
    }

    fun zoraResponse(body: String): Result<ZoraResponse> {
        val resp = mock<Response> {
            on { responseBody } doReturn body
        }
        val zoraResponse = mock<ZoraResponse> {
            on { isOk } doReturn true
            on { response } doReturn resp
        }
        val result = Result<ZoraResponse>(0)
        result.success = zoraResponse
        return result
    }

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        const val ITUNES_APP_INFO_DATA = """
{
    "trackName":"store itunes title"   ,
    "description":"store itunes description",
    "icon_mds":"https://itunes.com/store",
    "artistName":"store itunes artist",
    "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Purple115/v4/b6/ef/ef/b6efefa3-3b22-f2d4-43be-c78f5fbfa924/source/100x100bb.jpg",
    "artworkUrl512":"https://is1-ssl.mzstatic.com/image/thumb/Purple115/v4/b6/ef/ef/b6efefa3-3b22-f2d4-43be-c78f5fbfa924/source/512x512bb.jpg",
    "artworkUrl60":"https://is1-ssl.mzstatic.com/image/thumb/Purple115/v4/b6/ef/ef/b6efefa3-3b22-f2d4-43be-c78f5fbfa924/source/60x60bb.jpg",
    "averageUserRating":4.21,
    "userRatingCount":321,
    "bigScreenshotUrls":[
        "https://is2-ssl.mzstatic.com/image/thumb/Purple113/v4/9a/1c/4f/9a1c4f68-d690-0d9d-c91c-bd5a34af8e3d/pr_source.png/2778x0w.png"
    ],
    "bundleId":"app.yandex.some",
    "contentAdvisoryRating":"18+",
    "videos":[],
    "minimumOsVersion":"11.1",
    "additional":{
        "subtitle":"store itunes subtitle"
    },
    "currency":"RUB",
    "price":14.99,
    "primaryGenreName":"APPLICATION"
}
        """

        const val ITUNES_DB_EXPECTED_APP_INFO = """
{
    "app_info":{
        "id":"15026316425640756135",
        "app_id":"id123",
        "bundle_id":"bundleId123",
        "title":"store itunes title",
        "subtitle":"store itunes subtitle",
        "description":"store itunes description",
        "icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb",
        "language":"ru",
        "platform":2,
        "region":"ru",
        "vendor":"store itunes artist",
        "rating":4.21,
        "reviews":321,
        "age_limit":18,
        "min_os_version":"11.1",
        "url":"https://apps.apple.com/ru/app/id123?l=ru",
        "currency":"RUB",
        "price":14.99,
        "interests":[],
        "recommended_cpa":null,
        "recommended_cpi":null
    },
    "contents":[
        {
            "type":"image",
            "thumb":null,
            "source_url":"https://is2-ssl.mzstatic.com/image/thumb/Purple113/v4/9a/1c/4f/9a1c4f68-d690-0d9d-c91c-bd5a34af8e3d/pr_source.png/2778x0w.png",
            "source_iw":null,
            "source_ih":null,
            "mds_url":null,
            "filename":null
        }
    ],
    "is_tracking_url": false,
    "default_target_region": 225,
    "default_target_region_name": "Россия"
}
        """

        const val GP_APP_INFO_DATA = """
{
    "id":"app.yandex.some",
    "name":"store google play title",
    "description":"store google play description",
    "html_description":"store google play description</p>",
    "icon":"https://google.com/store",
    "author":{
        "name":"store google play artist"
    },
    "rating":{
        "votes":10101,
        "value":2.2
    },
    "adult":"18+",
    "screens":[
        "https://play-lh.googleusercontent.com/XYf4XCbsA7ehNjKBC7KimOIsJwHcBxXa3dPs7Zk_CLswhZWoS0rYV17ZOzIYpCg_-Lk=w9000"
    ],
    "screens_ex": [
        {
            "meta": {
                "AppTimestamp": "Feb  5 2019 13:45:22",
                "orig-size": {"y": 1200, "x": 1200},
                "orig-format": "PNG",
                "orig-animated": false,
                "r-orig-size": {"y": 1200, "x": 1200},
                "orig-size-bytes": 179226,
                "orig-orientation": "0"
            }
        }
    ],
    "videos":[
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    ],
    "minimumOsVersion":null,
    "price":{
        "currency":"RUB",
        "value":14.99
    },
    "videos_s3":{
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ":"http://s3.mds.yandex.net/google-play-videos/video1"
    },
    "categories":[
        "APPLICATION"
    ],
    "subcategory_eng":[
        "APPLICATION"
    ]
}
        """

        const val GP_DB_EXPECTED_APP_INFO = """
{
    "app_info":{
        "id":"4791608187873723201",
        "app_id":"app.yandex.some",
        "bundle_id":"app.yandex.some",
        "title":"store google play title",
        "subtitle":"store google play description",
        "description":"store google play description",
        "icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb",
        "language":"ru",
        "platform":1,
        "region":"ru",
        "vendor":"store google play artist",
        "rating":2.2,
        "reviews":10101,
        "age_limit":18,
        "min_os_version":null,
        "url":"https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.some",
        "currency":"RUB",
        "price":14.99,
        "interests":[
            21672105
        ],
        "recommended_cpa":null,
        "recommended_cpi":null
    },
    "contents":[
        {
            "type":"image",
            "thumb":null,
            "source_url":"https://play-lh.googleusercontent.com/XYf4XCbsA7ehNjKBC7KimOIsJwHcBxXa3dPs7Zk_CLswhZWoS0rYV17ZOzIYpCg_-Lk=w9000",
            "source_iw":null,
            "source_ih":null,
            "mds_url":null,
            "filename":null
        },
        {
            "type":"video",
            "thumb":null,
            "source_url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "source_iw":null,
            "source_ih":null,
            "mds_url":"http://s3.mds.yandex.net/google-play-videos/video1",
            "filename":null
        }
    ],
    "is_tracking_url": false,
    "default_target_region": 225,
    "default_target_region_name": "Россия"
}
        """
        const val GP_TRACKING_EXPECTED_APP_INFO = """
{
    "app_info":{
        "id":"3699902402152278654",
        "app_id":"app.yandex.other",
        "bundle_id":"app.yandex.other",
        "title":"store google play title",
        "subtitle":"store google play description",
        "description":"store google play description",
        "icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb",
        "language":"ru",
        "platform":1,
        "region":"ru",
        "vendor":"store google play artist",
        "rating":2.2,
        "reviews":10101,
        "age_limit":18,
        "min_os_version":null,
        "url":"https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.other",
        "currency":"RUB",
        "price":14.99,
        "interests":[
            21672105
        ],
        "recommended_cpa":null,
        "recommended_cpi":null
    },
    "contents":[
        {
            "type":"image",
            "thumb":null,
            "source_url":"https://play-lh.googleusercontent.com/XYf4XCbsA7ehNjKBC7KimOIsJwHcBxXa3dPs7Zk_CLswhZWoS0rYV17ZOzIYpCg_-Lk=w9000",
            "source_iw":null,
            "source_ih":null,
            "mds_url":null,
            "filename":null
        },
        {
            "type":"video",
            "thumb":null,
            "source_url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "source_iw":null,
            "source_ih":null,
            "mds_url":"http://s3.mds.yandex.net/google-play-videos/video1",
            "filename":null
        }
    ],
    "is_tracking_url": true,
    "default_target_region": 225,
    "default_target_region_name": "Россия"
}
        """

        const val GP_RECOMMENDED_EXPECTED = """
{
    "app_info":{
        "id":"17620644518839316272",
        "app_id":"app.yandex.yet.another",
        "bundle_id":"app.yandex.yet.another",
        "title":"store google play title",
        "subtitle":"store google play description",
        "description":"store google play description",
        "icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb",
        "language":"ru",
        "platform":1,
        "region":"ru",
        "vendor":"store google play artist",
        "rating":2.2,
        "reviews":10101,
        "age_limit":18,
        "min_os_version":null,
        "url":"https://play.google.com/store/apps/details?hl=ru&gl=ru&id=app.yandex.yet.another",
        "currency":"RUB",
        "price":14.99,
        "interests":[
            21672105
        ],
        "recommended_cpa":123,
        "recommended_cpi":999
    },
    "contents":[
        {
            "type":"image",
            "thumb":null,
            "source_url":"https://play-lh.googleusercontent.com/XYf4XCbsA7ehNjKBC7KimOIsJwHcBxXa3dPs7Zk_CLswhZWoS0rYV17ZOzIYpCg_-Lk=w9000",
            "source_iw":null,
            "source_ih":null,
            "mds_url":null,
            "filename":null
        },
        {
            "type":"video",
            "thumb":null,
            "source_url":"https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "source_iw":null,
            "source_ih":null,
            "mds_url":"http://s3.mds.yandex.net/google-play-videos/video1",
            "filename":null
        }
    ],
    "is_tracking_url": false,
    "default_target_region": 225,
    "default_target_region_name": "Россия"
}
        """

        const val GP_PARSED_DATA = """
{
    "adult": "Everyone",
    "author": {
        "email": "firefox-android-feedback@mozilla.com",
        "id": "7083182635971239206",
        "name": "Mozilla",
        "website": "https://www.mozilla.org/firefox/mobile/"
    },
    "categories": [
        "APPLICATION"
    ],
    "date_release": "November 9, 2021",
    "date_release_ts": "1636416000",
    "description": "Go bold with more control of your internet experience than ever before. Now you can see your open tabs, recent searches and favorite sites all in one place. The new Firefox home screen adapts to you. The more you use it, the smarter it gets. Firefox makes it easier for you to pick up right where you left off. When it comes to your life online, you have a choice: accept the factory settings or put your privacy first. When you choose Firefox as your default browser, you’re choosing to protect your data while supporting an independent tech company. Firefox is also the only major browser backed by a non-profit fighting to give you more openness, transparency and control of your life online. Join hundreds of millions of people who choose to protect what's important by choosing Firefox. TL;DR - Feel more in control of your internet experience. The new home screen lets you pick up right where you left off every time - Even more industry-leading privacy and security from the most trusted name in tech PERSONALIZED HOME SCREEN Pick up right where you left off. Now you can see all your open tabs intuitively grouped and displayed along with your recent searches, bookmarks and favorite sites. You’ll also see popular articles recommended by Pocket, updated daily. MOVE THE SEARCH BAR WHERE YOU WANT IT Firefox lets you choose the default placement of the search bar, either at the top or the bottom of your screen. Sometimes it’s easier to search for things if you put the search bar at the bottom — especially if you’re using one hand. FAST. PRIVATE. SAFE. Keep what’s personal private without sacrificing speed. With Firefox, you can have more control to decide what to share online and when, because your life is your business. We designed Firefox with smart browsing features that let you take your privacy, password and bookmarks with you safely wherever you go. MAKE FIREFOX YOUR OWN Make Firefox your go-to, get-it-done, default browser. And with Firefox widgets, you can jump right to searching the web or using private-browsing mode straight from your phone’s home screen. And Firefox makes password management easy by remembering your passwords across devices. PRIVACY CONTROL IN ALL THE RIGHT PLACES Firefox gives you greater privacy protection while you’re on the web. By default, Firefox blocks trackers and scripts such as social media trackers, cross-site cookie trackers, cryptominers and fingerprinters. You can also easily choose to search in private browsing mode. And when you close private browsing mode, your browsing history and any cookies are automatically erased from your device. FIND IT FAST WITH FIREFOX’S SEARCH BAR Get search suggestions in the search bar and quickly access the sites you visit most. Type in your search question and get suggested and previously searched results across your favorite search engines. COLLECTIONS Open as many tabs as you like and organize them into Collections to stay on task. Collections can also be shared across devices so you get more done no matter where you are or what device you’re using. GET ADD-ONS Full support for the most popular add-ons, including ways to turbo-charge powerful default privacy settings and customize your experience. SHARE ANYTHING IN A FEW TAPS The Firefox web browser makes it easy to share links to web pages or specific items on a page with easy, quick access to your most recently used apps. LEARN MORE ABOUT FIREFOX WEB BROWSER: - Read about Firefox permissions: http://mzl.la/Permissions - Learn more about what’s up at Mozilla: https://blog.mozilla.org ABOUT MOZILLA Mozilla exists to build the Internet as a public resource accessible to all because we believe open and free is better than closed and controlled. We build products like Firefox to promote choice and transparency and give people more control over their lives online. Learn more at https://www.mozilla.org.",
    "detected_lang": "en",
    "downloads": "100,000,000+",
    "has_ads": false,
    "has_purchases": false,
    "html_description": "Go bold with more control of your internet experience than ever before. Now you can see your open tabs, recent searches and favorite sites all in one place. The new Firefox home screen adapts to you. The more you use it, the smarter it gets. Firefox makes it easier for you to pick up right where you left off.<br><br>When it comes to your life online, you have a choice: accept the factory settings or put your privacy first. When you choose Firefox as your default browser, you’re choosing to protect your data while supporting an independent tech company. Firefox is also the only major browser backed by a non-profit fighting to give you more openness, transparency and control of your life online. Join hundreds of millions of people who choose to protect what's important by choosing Firefox.<br><br><b>TL;DR</b><br>- Feel more in control of your internet experience. The new home screen lets you pick up right where you left off every time<br>- Even more industry-leading privacy and security from the most trusted name in tech<br><br><b>PERSONALIZED HOME SCREEN</b><br>Pick up right where you left off. Now you can see all your open tabs intuitively grouped and displayed along with your recent searches, bookmarks and favorite sites. You’ll also see popular articles recommended by Pocket, updated daily.<br><br><b>MOVE THE SEARCH BAR WHERE YOU WANT IT</b><br>Firefox lets you choose the default placement of the search bar, either at the top or the bottom of your screen. Sometimes it’s easier to search for things if you put the search bar at the bottom — especially if you’re using one hand. <br><br><b>FAST. PRIVATE. SAFE.</b><br>Keep what’s personal private without sacrificing speed. With Firefox, you can have more control to decide what to share online and when, because your life is your business. We designed Firefox with smart browsing features that let you take your privacy, password and bookmarks with you safely wherever you go.<br><br><b>MAKE FIREFOX YOUR OWN</b><br>Make Firefox your go-to, get-it-done, default browser. And with Firefox widgets, you can jump right to searching the web or using private-browsing mode straight from your phone’s home screen. And Firefox makes password management easy by remembering your passwords across devices.<br><br><b>PRIVACY CONTROL IN ALL THE RIGHT PLACES</b><br>Firefox gives you greater privacy protection while you’re on the web. By default, Firefox blocks trackers and scripts such as social media trackers, cross-site cookie trackers, cryptominers and fingerprinters. You can also easily choose to search in private browsing mode. And when you close private browsing mode, your browsing history and any cookies are automatically erased from your device.<br><br><b>FIND IT FAST WITH FIREFOX’S SEARCH BAR</b><br>Get search suggestions in the search bar and quickly access the sites you visit most. Type in your search question and get suggested and previously searched results across your favorite search engines.<br><br><b>COLLECTIONS</b><br>Open as many tabs as you like and organize them into Collections to stay on task. Collections can also be shared across devices so you get more done no matter where you are or what device you’re using.<br><br><b>GET ADD-ONS</b><br>Full support for the most popular add-ons, including ways to turbo-charge powerful default privacy settings and customize your experience.<br><br><b>SHARE ANYTHING IN A FEW TAPS</b><br>The Firefox web browser makes it easy to share links to web pages or specific items on a page with easy, quick access to your most recently used apps.<br><br><b>LEARN MORE ABOUT FIREFOX WEB BROWSER:</b><br>- Read about Firefox permissions: http://mzl.la/Permissions<br>- Learn more about what’s up at Mozilla: https://blog.mozilla.org<br><br><b>ABOUT MOZILLA</b><br>Mozilla exists to build the Internet as a public resource accessible to all because we believe open and free is better than closed and controlled. We build products like Firefox to promote choice and transparency and give people more control over their lives online. Learn more at https://www.mozilla.org.",
    "icon": "https://play-lh.googleusercontent.com/zqsuwFUBwKRcGOSBinKQCL3JgfvOW49vJphq0ZF32aDgfqmuDyl-fEpx4Lxm4pRr7A=w9000",
    "id": "org.mozilla.firefox",
    "inapp_prices": null,
    "interactive_elements": [
        "Unrestricted Internet"
    ],
    "is_alpha": false,
    "is_available": false,
    "name": "Firefox Browser: fast, private & safe web browser",
    "ownCategories": [],
    "price": {
        "currency": "USD",
        "value": 0
    },
    "rating": {
        "value": 4.5,
        "votes": 4230056
    },
    "releaseNotes": [
        "New* Jump back into the content you care about. On the new customizable Homepage, you’ll see:- Recent tabs - Recent bookmarks- Recent searches - Articles recommended by Pocket* Tabs you haven’t viewed for 2 weeks will now move to the Inactive Tabs section to help keep your tabs tray clutter-free. Toggle this option in Settings.Changed* Settings now available to control which sections are visible on your Homepage.* Suggested history items in your search suggestions are now more relevant.Read moreCollapse"
    ],
    "requirements": "5.0 and up",
    "reviews": [
        {
            "author": "M A",
            "date": 1637171439,
            "rating": 1,
            "text": "I used to have a wonderful tile view of my tabs, I wake up and it's changed to a list without an option. I have a lot of tabs. Undoing a closed tab no longer returns to its original position. I now have to scroll to the top of the list and reposition the tab. Unusable app. What were you thinking not providing an option? Not Mozilla like at all. This app is ruined. You did exactly what G does and changed something no one asked you to fix. Update: Still dont have a print option. Chrome?"
        },
        {
            "author": "Adam Gretzinger",
            "date": 1637164122,
            "rating": 5,
            "text": "Really like the Firefox using interface compared to Chrome mobile. Would like to be able to run the app from SD card especially especially with the cache documents that build-up with normal usage. Firefox Bottom mounted scroll bar Wasn't a good idea but glad that I could move it to the top like a normal Web browser to avoid accidental presses. Some GPS website apps have trouble with Firefox. Really like the ability to switch between regular and incognito tabs. Printing functionality missing?"
        },
        {
            "author": "Tony Vangel",
            "date": 1636877565,
            "rating": 1,
            "text": "Trying to give FF all the chances to get me back. Just started FF mobile,and I get this certificate issue that says even Firefox sites are not safe. I can't find a fix anywhere. Mozilla says something drastic like don't use them. But that's on the pc. I could go to reddit and probably find an answer, but I think I'll stick with Vivaldi and Brave. It's almost 25 mins I can't be believe this. Bring back the old FF when they were smaller and cared. They're like the old Microsoft now,but worse"
        },
        {
            "author": "Bronson",
            "date": 1636886402,
            "rating": 4,
            "text": "This is a fantastic browser and app. It's been improving a lot in my year+ using it. My main problem with it is that I am unable to watch two or more videos at once that have different volume levels. Up until about 3 months ago I could, but eventually it stopped working. If there was a plugin made that could allow this again or some kind of patch, I would be very appreciative."
        }
    ],
    "screens": [
        "https://play-lh.googleusercontent.com/Wm8Isb6ta5HYPjuuDi86pNB3FB0T46V549ov-qXSh7pMMUB9IvwKhy2hNrLnHTTOyWo=w9000",
        "https://play-lh.googleusercontent.com/YuBl0w-3DsKwLHplDvc8nRqTKbK3RzgNQ6yRb2PiI2JO7izj4AQCnqGWhSvAVtqXu6U=w9000",
        "https://play-lh.googleusercontent.com/8NHGiR1mTB71a_EbMTp8iiU6_BytnRDB7zaKzgpJuSP4qL4o_FsYiDwTDKu69HvwGSo=w9000",
        "https://play-lh.googleusercontent.com/MRuva-TPm4Y_HyBQyrR6nAmZx6MRz74hJQQmCBZGl-nFYKmSNS5PnlMWEFUcTuDlcxM=w9000",
        "https://play-lh.googleusercontent.com/7NL16sJBDCnHhUPkGRVft8rtfVxCuWxWNtGf5pBpZ2xLDVS6XpfZgQaR020hiJdM6A=w9000",
        "https://play-lh.googleusercontent.com/1tYD-N0WnKw_z5MSg_MKBQuY7VEUe07hbCFlAegZ90JdVh9ZoFEMoMu8Ik5j4QdSz3UZ=w9000",
        "https://play-lh.googleusercontent.com/0rDV4Z3LsUwEOXfvn26zhOdXs0jNuX10C8fQA9ASB1VB3WPcYgYt92jmhEcm1Ofjuw=w9000",
        "https://play-lh.googleusercontent.com/-uKet0akgqLEkiWbiRkkOXBSKETxfaskAVdFc5yWH7L0tV2G5Waim3lBzSXP4fzONA=w9000",
        "https://play-lh.googleusercontent.com/2Yh_fmuTLSFyrtSZ5y6bKEIuj3P-JmLwxlCwAI0U206zAaTggS1yFxDqVZB4K4bucej6=w9000",
        "https://play-lh.googleusercontent.com/IDY6P_O5J6ZYRv9lRaoU174mTReiCRJhJRFWLXbhv5__UBWE6yewKA8YAB972XWDvGM=w9000",
        "https://play-lh.googleusercontent.com/cmg6C9VIfRsG3YBSPZLbIE564XMOoCNgoFWNaPLhywi3zYvSuLj4hp0adnDErl1KbN-s=w9000",
        "https://play-lh.googleusercontent.com/BUqEtGXfz4RRCohCR9kds-zwuMvDkUkFlARYhFEmyXErE0wQeev2CBXNdDqhFNdQPD02=w9000",
        "https://play-lh.googleusercontent.com/DLitZRJJrlr1CJZg3-vbWu6Da1iuArtPa5WFinFamK-c8j0M4aXCMvy_0nVcpUMsIQ2E=w9000",
        "https://play-lh.googleusercontent.com/W4LGpWy6kTK-xdctJSwb_1rD2v8QTfKcrjPMDcBT4WPwj_xpu5a1FtDnPa-XB6uNhS4=w9000",
        "https://play-lh.googleusercontent.com/1XewxerpoCmtgWsulRiyEJy8VxxPKWKTAJ5FzvoIIY0yB6bttu5bfHpY9JJxR4UIexY=w9000",
        "https://play-lh.googleusercontent.com/LOtEJtIJD2SrFTmUMDwklYlsYX-iNwkNKJFJhcn7GGlqmQCZS5mIeJMYCeGKBoYHhzA=w9000",
        "https://play-lh.googleusercontent.com/RkA8_nsujTfqoxPpWiy5kQ6D0ypFdmKfjgvJ-9VhVHYJzWLWxgG0my_kTUGmmLCjdg=w9000",
        "https://play-lh.googleusercontent.com/Eb9TbPf_RQmfJkPr_bCcd5tDFOUOxRYfUsABIDv-K5Oo76aT0K787GBpGFCc46ILgQ=w9000",
        "https://play-lh.googleusercontent.com/AAulp3YzgnAldJ1x7ubcogiUwx1qSCQh4JTmR1ovqtFtTIg3czYJ7_c3BAjaelPZkg=w9000",
        "https://play-lh.googleusercontent.com/X40UVW9Q8iYuxxGkLa-e-Ouflo8qlWhprtMUgQrLIdfy-YNpw9VVBIuOUJ10Si-Wrw=w9000",
        "https://play-lh.googleusercontent.com/Cy_XQEhVHxwkVl8AFwICy3WZKekbYJx3JqVL7uB2b5AJjdOMZE6Ud4XA_vUTN3kScl0=w9000",
        "https://play-lh.googleusercontent.com/zlSlPAGzV1Ma9plw_Da4yA28mgueYQ-3LGZvOtXDCFw1lo4ig0lZE-qYFDY6qCxWW3Q=w9000",
        "https://play-lh.googleusercontent.com/CUUF00Bjr6kWO15sriXziaG9EUK5wdOm6gUtPzdTQD5g8Fg8wpNvsHphp2nrghxniBY=w9000",
        "https://play-lh.googleusercontent.com/V2fAdvR9HMacjAueE14GfeHJ-hA_nzvibhH4vL9FghdrR7ae4SO4nODLW_vDiJ7ckA=w9000"
    ],
    "subcategory": [
        "Communication"
    ],
    "subcategory_eng": [
        "COMMUNICATION"
    ],
    "tags": {
        "_Tag__appId": "org.mozilla.firefox",
        "_Tag__negative": [],
        "_Tag__positive": []
    },
    "version": "94.1.2",
    "videos": []
}
"""

        const val ITUNES_APP_INFO_ADDITIONAL = """
{
    "best_in_category":null,
    "has_purchases":true,
    "detected_lang":"ru",
    "subtitle":"Minimal but powerful"
}
"""

        const val ITUNES_APP_INFO_LOOKUP = """
{
 "resultCount":1,
 "results": [
    {
    "ipadScreenshotUrls":["https://is1-ssl.mzstatic.com/image/thumb/Purple113/v4/eb/fc/bf/ebfcbf4d-ff8b-b3be-f1fa-d91076ac68fd/pr_source.png/576x768bb.png", "https://is5-ssl.mzstatic.com/image/thumb/Purple123/v4/a1/38/22/a1382204-51cf-740d-fb15-fd13c1fb37c4/pr_source.png/576x768bb.png", "https://is4-ssl.mzstatic.com/image/thumb/Purple113/v4/27/74/ac/2774ac52-c088-3aae-18e1-ec9b5103b221/pr_source.png/576x768bb.png", "https://is1-ssl.mzstatic.com/image/thumb/Purple123/v4/ac/e5/19/ace519c8-c883-9c12-cfe5-0ba22c2c9353/pr_source.png/576x768bb.png", "https://is2-ssl.mzstatic.com/image/thumb/Purple113/v4/ca/c1/ed/cac1edca-1a6c-75ae-a1aa-090251336516/pr_source.png/576x768bb.png"], "appletvScreenshotUrls":[], "artworkUrl60":"https://is2-ssl.mzstatic.com/image/thumb/Purple126/v4/ac/2c/67/ac2c67dd-3c18-dead-db95-902309d93a14/source/60x60bb.jpg", "artworkUrl512":"https://is2-ssl.mzstatic.com/image/thumb/Purple126/v4/ac/2c/67/ac2c67dd-3c18-dead-db95-902309d93a14/source/512x512bb.jpg", "artworkUrl100":"https://is2-ssl.mzstatic.com/image/thumb/Purple126/v4/ac/2c/67/ac2c67dd-3c18-dead-db95-902309d93a14/source/100x100bb.jpg", "artistViewUrl":"https://apps.apple.com/us/developer/francesco-bianco/id1329694016?l=ru&uo=4",
    "screenshotUrls":["https://is4-ssl.mzstatic.com/image/thumb/Purple123/v4/8f/04/cc/8f04cc95-8e6d-2f44-5048-66d04a262358/pr_source.png/392x696bb.png", "https://is3-ssl.mzstatic.com/image/thumb/Purple113/v4/c7/3c/fe/c73cfed8-adc7-044e-b5a1-a41be3f73c13/pr_source.png/392x696bb.png", "https://is2-ssl.mzstatic.com/image/thumb/Purple113/v4/6c/9f/0f/6c9f0fbc-37fb-5a60-798d-fd96e47ac10a/pr_source.png/392x696bb.png", "https://is4-ssl.mzstatic.com/image/thumb/Purple123/v4/e8/24/71/e824715d-956d-0d07-a655-1dd1c778c5fd/pr_source.png/392x696bb.png", "https://is5-ssl.mzstatic.com/image/thumb/Purple113/v4/36/fb/30/36fb30b5-279d-f6b2-f155-63db6411301e/pr_source.png/392x696bb.png"], "isGameCenterEnabled":false, "features":["iosUniversal"],
    "supportedDevices":["iPhone5s-iPhone5s", "iPadAir-iPadAir", "iPadAirCellular-iPadAirCellular", "iPadMiniRetina-iPadMiniRetina", "iPadMiniRetinaCellular-iPadMiniRetinaCellular", "iPhone6-iPhone6", "iPhone6Plus-iPhone6Plus", "iPadAir2-iPadAir2", "iPadAir2Cellular-iPadAir2Cellular", "iPadMini3-iPadMini3", "iPadMini3Cellular-iPadMini3Cellular", "iPodTouchSixthGen-iPodTouchSixthGen", "iPhone6s-iPhone6s", "iPhone6sPlus-iPhone6sPlus", "iPadMini4-iPadMini4", "iPadMini4Cellular-iPadMini4Cellular", "iPadPro-iPadPro", "iPadProCellular-iPadProCellular", "iPadPro97-iPadPro97", "iPadPro97Cellular-iPadPro97Cellular", "iPhoneSE-iPhoneSE", "iPhone7-iPhone7", "iPhone7Plus-iPhone7Plus", "iPad611-iPad611", "iPad612-iPad612", "iPad71-iPad71", "iPad72-iPad72", "iPad73-iPad73", "iPad74-iPad74", "iPhone8-iPhone8", "iPhone8Plus-iPhone8Plus", "iPhoneX-iPhoneX", "iPad75-iPad75", "iPad76-iPad76", "iPhoneXS-iPhoneXS", "iPhoneXSMax-iPhoneXSMax", "iPhoneXR-iPhoneXR", "iPad812-iPad812", "iPad834-iPad834", "iPad856-iPad856", "iPad878-iPad878", "iPadMini5-iPadMini5", "iPadMini5Cellular-iPadMini5Cellular", "iPadAir3-iPadAir3", "iPadAir3Cellular-iPadAir3Cellular", "iPodTouchSeventhGen-iPodTouchSeventhGen", "iPhone11-iPhone11", "iPhone11Pro-iPhone11Pro", "iPadSeventhGen-iPadSeventhGen", "iPadSeventhGenCellular-iPadSeventhGenCellular", "iPhone11ProMax-iPhone11ProMax", "iPhoneSESecondGen-iPhoneSESecondGen", "iPadProSecondGen-iPadProSecondGen", "iPadProSecondGenCellular-iPadProSecondGenCellular", "iPadProFourthGen-iPadProFourthGen", "iPadProFourthGenCellular-iPadProFourthGenCellular", "iPhone12Mini-iPhone12Mini", "iPhone12-iPhone12", "iPhone12Pro-iPhone12Pro", "iPhone12ProMax-iPhone12ProMax", "iPadAir4-iPadAir4", "iPadAir4Cellular-iPadAir4Cellular", "iPadEighthGen-iPadEighthGen", "iPadEighthGenCellular-iPadEighthGenCellular", "iPadProThirdGen-iPadProThirdGen", "iPadProThirdGenCellular-iPadProThirdGenCellular", "iPadProFifthGen-iPadProFifthGen", "iPadProFifthGenCellular-iPadProFifthGenCellular", "iPhone13Pro-iPhone13Pro", "iPhone13ProMax-iPhone13ProMax", "iPhone13Mini-iPhone13Mini", "iPhone13-iPhone13", "iPadMiniSixthGen-iPadMiniSixthGen", "iPadMiniSixthGenCellular-iPadMiniSixthGenCellular", "iPadNinthGen-iPadNinthGen", "iPadNinthGenCellular-iPadNinthGenCellular"],
    "advisories":["Малое/умеренное количество тем, предназначенных только для взрослых", "Малое/умеренное количество медицинской или лечебной тематики", "Малое/умеренное количество сквернословия или грубого юмора", "Малое/умеренное количество тем, вызывающих ужас или страх", "Неограниченный доступ к Сети", "Малое/умеренное количество контента сексуального или эротического характера", "Малое/умеренное количество реалистичного насилия", "Малое/умеренное количество мультипликационного или фэнтезийного насилия"], "kind":"software", "minimumOsVersion":"15.0", "trackCensoredName":"ReSurfer", "languageCodesISO2A":["EN"], "fileSizeBytes":"15261696", "formattedPrice":"Бесплатно", "contentAdvisoryRating":"17+", "averageUserRatingForCurrentVersion":4.5357099999999999084820956340990960597991943359375, "userRatingCountForCurrentVersion":28, "averageUserRating":4.5357099999999999084820956340990960597991943359375, "trackViewUrl":"https://apps.apple.com/us/app/resurfer/id1489340046?l=ru&uo=4", "trackContentRating":"17+", "bundleId":"FrancescoBianco.RedditUIKit", "trackId":1489340046, "trackName":"ReSurfer", "releaseDate":"2020-06-07T07:00:00Z", "primaryGenreName":"News", "genreIds":["6009"], "isVppDeviceBasedLicensingEnabled":true, "currentVersionReleaseDate":"2022-01-05T13:26:26Z", "sellerName":"Francesco Bianco",
    "releaseNotes":"This release has some small fixes regarding posting comments and themes:\n• Fixed a bug that prevented to post comments on the right post.\n• Added more app themes: Nord, Basic and YinYang!\n• Added a new widget that displays images from your subscriptions into your homescreen!\n• Upgraded the theming engine: now it's possible to specify the secondary color for a new theme.\n• Completely renewed process of creating new themes.\n• Added the ability to customize the tint color of links on the theme creator view.\n• Improved the color scheme of some themes (Dracula and Abyss).\n• Solved some crashes that could occur while interacting with links.\n• Fixed an issue that prevented you to view a specific image inside a gallery.\n• General bugfixing.\n\nHappy new year !", "primaryGenreId":6009, "currency":"USD",
    "description":"Less is More\n\nReSurfer is a client for Reddit that has its minimal design as its strength. Every small component of the app has been designed to be essential and at the same time immediate. But don’t worry, despite the ReSurfer aspect it is able to best meet the needs of each Redditor. With ReSurfer you will be able to take advantage of the main features of Reddit, including:\n• Scroll through your subscriptions\n• Follow new communities\n• Consult your personalized lists (multireddits)\n• Consult notifications and messages\n• Create posts with a fantastic Markdown editor\n• Reply and comment on the links\n• Rate and save your favorite content\nAs soon as you start the app you will find yourself immersed in the world of Reddit, on the Internet Front Page.\n\nKeyword: Personalization\n\nReSurfer has been designed to be completely customizable: from content and sorting preferences to the size of each post. In fact, you can choose between:\n• Compact and large posts;\n• Autoplay of videos and gifs;\n• A varied collection of themes! You will have the opportunity to truly make the app\nyours, thanks to a collection of 10 themes created to make the app unique;\n• Custom app icons! ReSurfer offers a large selection of icons\ncustomized to choose from, choose your favorite!\n\nGestures to simplify your life\n\nReSurfer uses gestures to make any action in the app easier, vote, comment, share and much more. In addition to custom gestures ReSurfer takes advantage of the contextual menus offered by iOS 13. Do you use an iPad with a trackpad or mouse? No problem ! ReSurfer supports use with an external mouse (available from iOS 13.4).\nAverage for everyone\nReSurfer has been designed to provide an immersive experience, in fact it allows you to view videos, gifs, images, links without having to leave the app. It also makes it easy to share any content that is shown within the app, giving you the opportunity to share posts but also media instantly.", "artistId":1329694016, "artistName":"Francesco Bianco", "genres":["Новости"], "price":0.00, "version":"1.4.5", "wrapperType":"software", "userRatingCount":28
    }]
}
"""
        const val ITUNES_PARSED_EXPECTED_APP_INFO = """
          {"app_info":{"id":"6678314111666544000","app_id":"id1489340046","bundle_id":"FrancescoBianco.RedditUIKit","title":"ReSurfer","subtitle":"Minimal but powerful","description":"Less is More\n\nReSurfer is a client for Reddit that has its minimal design as its strength. Every small component of the app has been designed to be essential and at the same time immediate. But don’t worry, despite the ReSurfer aspect it is able to best meet the needs of each Redditor. With ReSurfer you will be able to take advantage of the main features of Reddit, including:\n• Scroll through your subscriptions\n• Follow new communities\n• Consult your personalized lists (multireddits)\n• Consult notifications and messages\n• Create posts with a fantastic Markdown editor\n• Reply and comment on the links\n• Rate and save your favorite content\nAs soon as you start the app you will find yourself immersed in the world of Reddit, on the Internet Front Page.\n\nKeyword: Personalization\n\nReSurfer has been designed to be completely customizable: from content and sorting preferences to the size of each post. In fact, you can choose between:\n• Compact and large posts;\n• Autoplay of videos and gifs;\n• A varied collection of themes! You will have the opportunity to truly make the app\nyours, thanks to a collection of 10 themes created to make the app unique;\n• Custom app icons! ReSurfer offers a large selection of icons\ncustomized to choose from, choose your favorite!\n\nGestures to simplify your life\n\nReSurfer uses gestures to make any action in the app easier, vote, comment, share and much more. In addition to custom gestures ReSurfer takes advantage of the contextual menus offered by iOS 13. Do you use an iPad with a trackpad or mouse? No problem ! ReSurfer supports use with an external mouse (available from iOS 13.4).\nAverage for everyone\nReSurfer has been designed to provide an immersive experience, in fact it allows you to view videos, gifs, images, links without having to leave the app. It also makes it easy to share any content that is shown within the app, giving you the opportunity to share posts but also media instantly.","icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb","language":"ru","platform":2,"region":"ru","vendor":"Francesco Bianco","rating":4.53571,"reviews":28,"age_limit":17,"min_os_version":"15.0","url":"https://apps.apple.com/ru/app/id1489340046?l=ru","currency":"USD","price":0.0,"interests":[21672155],"recommended_cpa":null,"recommended_cpi":null},"contents":[{"type":"image","thumb":null,"source_url":"https://is4-ssl.mzstatic.com/image/thumb/Purple123/v4/8f/04/cc/8f04cc95-8e6d-2f44-5048-66d04a262358/pr_source.png/2778x0w.png","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://is3-ssl.mzstatic.com/image/thumb/Purple113/v4/c7/3c/fe/c73cfed8-adc7-044e-b5a1-a41be3f73c13/pr_source.png/2778x0w.png","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://is2-ssl.mzstatic.com/image/thumb/Purple113/v4/6c/9f/0f/6c9f0fbc-37fb-5a60-798d-fd96e47ac10a/pr_source.png/2778x0w.png","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://is4-ssl.mzstatic.com/image/thumb/Purple123/v4/e8/24/71/e824715d-956d-0d07-a655-1dd1c778c5fd/pr_source.png/2778x0w.png","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://is5-ssl.mzstatic.com/image/thumb/Purple113/v4/36/fb/30/36fb30b5-279d-f6b2-f155-63db6411301e/pr_source.png/2778x0w.png","source_iw":null,"source_ih":null,"mds_url":null,"filename":null}],"default_target_region":225,"default_target_region_name":"Россия","is_tracking_url":false}
        """

        const val GP_PARSED_EXPECTED_APP_INFO = """
          {"app_info":{"id":"14457987235032061033","app_id":"org.mozilla.firefox","bundle_id":"org.mozilla.firefox","title":"Firefox Browser: fast, private & safe web browser","subtitle":"Go bold with more control of your internet experience than ever before.","description":"Go bold with more control of your internet experience than ever before. Now you can see your open tabs, recent searches and favorite sites all in one place. The new Firefox home screen adapts to you. The more you use it, the smarter it gets. Firefox makes it easier for you to pick up right where you left off. When it comes to your life online, you have a choice: accept the factory settings or put your privacy first. When you choose Firefox as your default browser, you’re choosing to protect your data while supporting an independent tech company. Firefox is also the only major browser backed by a non-profit fighting to give you more openness, transparency and control of your life online. Join hundreds of millions of people who choose to protect what's important by choosing Firefox. TL;DR - Feel more in control of your internet experience. The new home screen lets you pick up right where you left off every time - Even more industry-leading privacy and security from the most trusted name in tech PERSONALIZED HOME SCREEN Pick up right where you left off. Now you can see all your open tabs intuitively grouped and displayed along with your recent searches, bookmarks and favorite sites. You’ll also see popular articles recommended by Pocket, updated daily. MOVE THE SEARCH BAR WHERE YOU WANT IT Firefox lets you choose the default placement of the search bar, either at the top or the bottom of your screen. Sometimes it’s easier to search for things if you put the search bar at the bottom — especially if you’re using one hand. FAST. PRIVATE. SAFE. Keep what’s personal private without sacrificing speed. With Firefox, you can have more control to decide what to share online and when, because your life is your business. We designed Firefox with smart browsing features that let you take your privacy, password and bookmarks with you safely wherever you go. MAKE FIREFOX YOUR OWN Make Firefox your go-to, get-it-done, default browser. And with Firefox widgets, you can jump right to searching the web or using private-browsing mode straight from your phone’s home screen. And Firefox makes password management easy by remembering your passwords across devices. PRIVACY CONTROL IN ALL THE RIGHT PLACES Firefox gives you greater privacy protection while you’re on the web. By default, Firefox blocks trackers and scripts such as social media trackers, cross-site cookie trackers, cryptominers and fingerprinters. You can also easily choose to search in private browsing mode. And when you close private browsing mode, your browsing history and any cookies are automatically erased from your device. FIND IT FAST WITH FIREFOX’S SEARCH BAR Get search suggestions in the search bar and quickly access the sites you visit most. Type in your search question and get suggested and previously searched results across your favorite search engines. COLLECTIONS Open as many tabs as you like and organize them into Collections to stay on task. Collections can also be shared across devices so you get more done no matter where you are or what device you’re using. GET ADD-ONS Full support for the most popular add-ons, including ways to turbo-charge powerful default privacy settings and customize your experience. SHARE ANYTHING IN A FEW TAPS The Firefox web browser makes it easy to share links to web pages or specific items on a page with easy, quick access to your most recently used apps. LEARN MORE ABOUT FIREFOX WEB BROWSER: - Read about Firefox permissions: http://mzl.la/Permissions - Learn more about what’s up at Mozilla: https://blog.mozilla.org ABOUT MOZILLA Mozilla exists to build the Internet as a public resource accessible to all because we believe open and free is better than closed and controlled. We build products like Firefox to promote choice and transparency and give people more control over their lives online. Learn more at https://www.mozilla.org.","icon_url":"https://avatars.mds.yandex.net/get-uac/42/some_name/thumb","language":"ru","platform":1,"region":"ru","vendor":"Mozilla","rating":4.5,"reviews":4230056,"age_limit":null,"min_os_version":null,"url":"https://play.google.com/store/apps/details?hl=ru&gl=ru&id=org.mozilla.firefox","currency":"USD","price":0.0,"interests":[21672115],"recommended_cpa":null,"recommended_cpi":null},"contents":[{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/Wm8Isb6ta5HYPjuuDi86pNB3FB0T46V549ov-qXSh7pMMUB9IvwKhy2hNrLnHTTOyWo=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/YuBl0w-3DsKwLHplDvc8nRqTKbK3RzgNQ6yRb2PiI2JO7izj4AQCnqGWhSvAVtqXu6U=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/8NHGiR1mTB71a_EbMTp8iiU6_BytnRDB7zaKzgpJuSP4qL4o_FsYiDwTDKu69HvwGSo=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/MRuva-TPm4Y_HyBQyrR6nAmZx6MRz74hJQQmCBZGl-nFYKmSNS5PnlMWEFUcTuDlcxM=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/7NL16sJBDCnHhUPkGRVft8rtfVxCuWxWNtGf5pBpZ2xLDVS6XpfZgQaR020hiJdM6A=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/1tYD-N0WnKw_z5MSg_MKBQuY7VEUe07hbCFlAegZ90JdVh9ZoFEMoMu8Ik5j4QdSz3UZ=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/0rDV4Z3LsUwEOXfvn26zhOdXs0jNuX10C8fQA9ASB1VB3WPcYgYt92jmhEcm1Ofjuw=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/-uKet0akgqLEkiWbiRkkOXBSKETxfaskAVdFc5yWH7L0tV2G5Waim3lBzSXP4fzONA=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/2Yh_fmuTLSFyrtSZ5y6bKEIuj3P-JmLwxlCwAI0U206zAaTggS1yFxDqVZB4K4bucej6=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/IDY6P_O5J6ZYRv9lRaoU174mTReiCRJhJRFWLXbhv5__UBWE6yewKA8YAB972XWDvGM=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/cmg6C9VIfRsG3YBSPZLbIE564XMOoCNgoFWNaPLhywi3zYvSuLj4hp0adnDErl1KbN-s=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/BUqEtGXfz4RRCohCR9kds-zwuMvDkUkFlARYhFEmyXErE0wQeev2CBXNdDqhFNdQPD02=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/DLitZRJJrlr1CJZg3-vbWu6Da1iuArtPa5WFinFamK-c8j0M4aXCMvy_0nVcpUMsIQ2E=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/W4LGpWy6kTK-xdctJSwb_1rD2v8QTfKcrjPMDcBT4WPwj_xpu5a1FtDnPa-XB6uNhS4=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/1XewxerpoCmtgWsulRiyEJy8VxxPKWKTAJ5FzvoIIY0yB6bttu5bfHpY9JJxR4UIexY=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/LOtEJtIJD2SrFTmUMDwklYlsYX-iNwkNKJFJhcn7GGlqmQCZS5mIeJMYCeGKBoYHhzA=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/RkA8_nsujTfqoxPpWiy5kQ6D0ypFdmKfjgvJ-9VhVHYJzWLWxgG0my_kTUGmmLCjdg=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/Eb9TbPf_RQmfJkPr_bCcd5tDFOUOxRYfUsABIDv-K5Oo76aT0K787GBpGFCc46ILgQ=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/AAulp3YzgnAldJ1x7ubcogiUwx1qSCQh4JTmR1ovqtFtTIg3czYJ7_c3BAjaelPZkg=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/X40UVW9Q8iYuxxGkLa-e-Ouflo8qlWhprtMUgQrLIdfy-YNpw9VVBIuOUJ10Si-Wrw=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/Cy_XQEhVHxwkVl8AFwICy3WZKekbYJx3JqVL7uB2b5AJjdOMZE6Ud4XA_vUTN3kScl0=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/zlSlPAGzV1Ma9plw_Da4yA28mgueYQ-3LGZvOtXDCFw1lo4ig0lZE-qYFDY6qCxWW3Q=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/CUUF00Bjr6kWO15sriXziaG9EUK5wdOm6gUtPzdTQD5g8Fg8wpNvsHphp2nrghxniBY=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null},{"type":"image","thumb":null,"source_url":"https://play-lh.googleusercontent.com/V2fAdvR9HMacjAueE14GfeHJ-hA_nzvibhH4vL9FghdrR7ae4SO4nODLW_vDiJ7ckA=w9000","source_iw":null,"source_ih":null,"mds_url":null,"filename":null}],"default_target_region":225,"default_target_region_name":"Россия","is_tracking_url":false}
        """
    }
}
