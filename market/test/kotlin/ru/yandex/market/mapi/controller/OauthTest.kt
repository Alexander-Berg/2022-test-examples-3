package ru.yandex.market.mapi.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import ru.yandex.market.mapi.AbstractMapiTest
import ru.yandex.market.mapi.core.MapiHeaders
import ru.yandex.market.mapi.engine.EngineContextPreparer
import ru.yandex.market.mapi.mock.BlackboxMocker
import ru.yandex.market.mapi.mock.FapiMocker
import ru.yandex.market.mapi.mock.TemplatorMocker

class OauthTest : AbstractMapiTest() {

    @Autowired
    private lateinit var blackboxMocker: BlackboxMocker

    @Autowired
    lateinit var templatorMocker: TemplatorMocker

    @Autowired
    lateinit var fapiMocker: FapiMocker

    @Test
    fun shouldPassWithToken() {
        blackboxMocker.mockOauth()

        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json")
        fapiMocker.mockFapiBatchResponse("/engine/plus/plusResolverCorrect.json", EngineContextPreparer.YA_PLUS_BATCH)

        mvcCall(
            get("/api/screen/main")
                .header(MapiHeaders.HEADER_OAUTH, "default token")
        )
    }

    @Test
    fun shouldPassWithoutToken() {
        templatorMocker.mockPageResponse("/engine/basicCmsTestPage.json")

        mvcCall(
            get("/api/screen/main")
        )
    }
}
