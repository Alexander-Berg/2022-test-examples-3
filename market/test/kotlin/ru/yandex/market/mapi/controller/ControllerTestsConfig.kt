package ru.yandex.market.mapi.controller

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import ru.yandex.market.mapi.controller.screen.MainScreenController
import ru.yandex.market.mapi.core.MapiContext
import ru.yandex.market.mapi.core.MockContext
import ru.yandex.market.mapi.core.NonEngineConfig
import ru.yandex.market.mapi.core.contract.ScreenProcessor
import ru.yandex.market.mapi.core.util.mockMapiContext
import ru.yandex.market.mapi.core.util.spring.ScreenRequestResolver
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Internal modifier is required to forbid usage in engine tests
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2022
 */
@Configuration
@NonEngineConfig
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class, HibernateJpaAutoConfiguration::class,
        LiquibaseAutoConfiguration::class, GsonAutoConfiguration::class, SpringDataWebAutoConfiguration::class,
        FreeMarkerAutoConfiguration::class, QuartzAutoConfiguration::class
    ]
)
@ComponentScan(
    basePackageClasses = [MainScreenController::class]
)
internal open class ControllerTestsConfig {
    @Bean
    open fun screenProcessor() = MockContext.registerMock<ScreenProcessor>()

    @NonEngineConfig
    @Configuration
    @EnableWebSecurity
    internal open class SecurityConfig : WebSecurityConfigurerAdapter() {
        override fun configure(http: HttpSecurity) {
            // disable all checks in tests
            http
                .authorizeRequests()
                .anyRequest().permitAll()
                .and().csrf().disable()
        }
    }

    @NonEngineConfig
    @EnableWebMvc
    @Configuration
    open class WebConfig : WebMvcConfigurer {
        override fun addInterceptors(registry: InterceptorRegistry) {
            registry.addInterceptor(TestInterceptor())
        }

        override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
            resolvers.add(ScreenRequestResolver())
        }
    }

    class TestInterceptor : HandlerInterceptor {
        override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
            // specially oversimplified
            // do not test engine logic here
            MapiContext.set(mockMapiContext { })
            return true
        }
    }
}