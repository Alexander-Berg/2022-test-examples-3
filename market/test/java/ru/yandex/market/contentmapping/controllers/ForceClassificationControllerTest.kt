package ru.yandex.market.contentmapping.controllers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.mockito.Mockito
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import ru.yandex.market.contentmapping.controllers.ControllerTestUtils.mockHttpRequest
import ru.yandex.market.contentmapping.controllers.ForceClassificationController.ForceClassificationRequest
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.services.category.info.CategoryControlService

class ForceClassificationControllerTest {
    @After
    fun cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `Checks access, calls service and returns result`() {
        RequestContextHolder.setRequestAttributes(ServletWebRequest(mockHttpRequest()))
        val accessHelper = mock<ControllerAccessHelper>()

        val request = ForceClassificationRequest(123L, listOf("sku1", "sku2"))
        val categoryControlService = mock<CategoryControlService> {
            Mockito.doReturn(true).`when`(it).forceReclassifficateOffers(
                    eq(request.shopId),
                    eq(request.shopSkus),
                    eq("testuser")
            )
        }

        val controller = ForceClassificationController(accessHelper, categoryControlService)
        val response = controller.forceClassification(request)

        response.success shouldBe true
        verify(accessHelper).validateUserIsOperator(eq(request.shopId), any())
        verify(accessHelper).validateShopIsNotLocked(eq(request.shopId))
    }
}
