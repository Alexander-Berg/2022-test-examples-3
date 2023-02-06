package ru.yandex.market.contentmapping.controllers

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import javax.servlet.http.HttpServletRequest

object ControllerTestUtils {
    fun mockHttpRequest(userLogin: String = "testuser"): HttpServletRequest = mock {
        doReturn(userLogin).whenever(it).getAttribute(eq("user_login"))
        doReturn(userLogin).whenever(it).getAttribute(eq("user_name"))
    }
}
