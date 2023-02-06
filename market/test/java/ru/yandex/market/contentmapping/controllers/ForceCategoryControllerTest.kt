package ru.yandex.market.contentmapping.controllers

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletWebRequest
import ru.yandex.market.contentmapping.controllers.ForceCategoryController.ForceCategoryRequest
import ru.yandex.market.contentmapping.controllers.helper.ControllerAccessHelper
import ru.yandex.market.contentmapping.services.category.info.CategoryControlService

class ForceCategoryControllerTest {
    @After
    fun cleanup() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `Checks access, calls service and returns result`() {
        RequestContextHolder.setRequestAttributes(ServletWebRequest(ControllerTestUtils.mockHttpRequest()))
        val accessHelper = mock<ControllerAccessHelper>()

        val request = ForceCategoryRequest(
                shopId = 1337,
                forcedCategoryId = 321,
                shopSkus = listOf("TEST")
        )

        val categoryControlService = mock<CategoryControlService> {
            doReturn(true).`when`(it).forceOffersCategory(
                    eq(request.shopId),
                    eq(request.shopSkus),
                    eq(request.forcedCategoryId),
                    eq("testuser")
            )
        }

        val controller = ForceCategoryController(accessHelper, categoryControlService)

        val response = controller.forceCategory(request)

        response.success shouldBe true
        verify(accessHelper).validateUserIsOperator(eq(request.shopId), any())
        verify(accessHelper).validateShopIsNotLocked(eq(request.shopId))
    }
}
