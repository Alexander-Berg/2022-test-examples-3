package ru.yandex.market.mapi.section.orders

import org.junit.jupiter.api.Test
import ru.yandex.market.mapi.core.UserExpInfo
import ru.yandex.market.mapi.core.util.assertJson
import ru.yandex.market.mapi.core.util.mockExpInfo
import ru.yandex.market.mapi.section.AbstractSectionTest
import ru.yandex.market.mapi.section.common.orders.OrderListNoAuthAssembler
import ru.yandex.market.mapi.section.common.orders.OrderListUnauthorizedRenderer
import ru.yandex.market.mapi.section.common.orders.UserOrdersNoAuthSection

/**
 * @author: Anastasia Fakhrieva | afakhrieva@
 * Date: 21.07.2022
 */
class UserOrdersNoAuthSectionTest : AbstractSectionTest() {
    private val assembler = OrderListNoAuthAssembler()

    @Test
    fun testDivkitTemplates() {
        assertJson(OrderListUnauthorizedRenderer.templates, "/section/common/orders/no_auth/templates.json")
    }

    @Test
    fun testOrderNoAuthContent() {
        mockExpInfo(
            UserExpInfo(
                version = "test-version",
                uaasRearrs = linkedSetOf("test-rearr1=0")
            )
        )
        testContentResult(
            section = buildWidget(),
            assembler = assembler,
            expected = "/section/common/orders/no_auth/contentResult.json"
        )
    }

    @Test
    fun testSection() {
        testSectionResult(
            section = buildWidget(),
            assembler = assembler,
            resolver = buildAnyResolver(),
            expected = "/section/common/orders/no_auth/sectionResult.json"
        )
    }

    private fun buildWidget() = UserOrdersNoAuthSection().apply {
        id = "my-orders-test-section-no-auth"
    }
}
