package ru.yandex.market.wms.taskrouter.util

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockHttpServletRequestDsl
import ru.yandex.market.wms.auth.core.model.InforAuthentication

private const val ATTR = "SPRING_SECURITY_CONTEXT"

fun MockHttpServletRequestDsl.setSecurityContextAttribute(username: String = "defaultTestUser") {
    sessionAttr(ATTR, createSecurityContext(username))
}

private fun createSecurityContext(username: String): SecurityContext {
    val tokenContent = "$username-token"
    val token = InforAuthentication(username, tokenContent, emptyList()).apply { isAuthenticated = true }
    return TestSecurityContextHolder.getContext().apply { authentication = token }
}
