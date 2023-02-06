package ru.yandex.market.tpl.courier.domain.feature.point

import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.yandex.market.tpl.courier.arch.fp.orThrow
import ru.yandex.market.tpl.courier.data.parse
import ru.yandex.market.tpl.courier.data.toJson
import ru.yandex.market.tpl.courier.testApplication

@RunWith(RobolectricTestRunner::class)
class RoutePointJsonMappingTest {

    @Test
    fun `RoutePoint mapping to json and back works as expected`() {
        val jsonMapper = testApplication.component.jsonMapper
        val initialRoutePoint = routePointTestInstance()
        val asJson = jsonMapper.toJson(initialRoutePoint).orThrow()
        val mappedRoutePoint = jsonMapper.parse<RoutePoint>(asJson).orThrow()

        initialRoutePoint shouldBe mappedRoutePoint
    }
}