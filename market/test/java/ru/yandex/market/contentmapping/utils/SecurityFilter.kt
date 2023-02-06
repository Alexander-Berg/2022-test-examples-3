package ru.yandex.market.contentmapping.utils

import ru.yandex.market.contentmapping.auth.filter.AuthInterceptor
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * MockMVC environment knows about Filters but not Interceptors for some strange reason. So just wrap it.
 */
class SecurityFilter(private val authInterceptor: AuthInterceptor) : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        authInterceptor.preHandle(request as HttpServletRequest, response as HttpServletResponse, 42L)

        chain.doFilter(request, response)
    }

    override fun init(filterConfig: FilterConfig) {
    }

    override fun destroy() {
    }
}
