package ru.yandex.market.logistics.calendaring.base

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.yandex.market.logistics.calendaring.config.DbqueueNotActiveIntegrationTestConfig
import ru.yandex.market.logistics.calendaring.config.IdmSecurityTestConfig
import ru.yandex.market.logistics.calendaring.config.IntegrationTestConfig
import ru.yandex.market.logistics.calendaring.security.CalendaringIdmRolesAuthenticationFilter
import ru.yandex.market.logistics.calendaring.service.IdmService
import ru.yandex.market.logistics.calendaring.service.system.IdmRoleService

@SpringBootTest(classes = [
    IdmSecurityTestConfig::class,
    IntegrationTestConfig::class,
    DbqueueNotActiveIntegrationTestConfig::class,
])
abstract class IdmContextualTest : IntegrationTest() {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var idmService: IdmService

    @Autowired
    private lateinit var idmRoleService: IdmRoleService

    @BeforeEach
    fun init() {
        val idmAuthenticationFilter = CalendaringIdmRolesAuthenticationFilter(idmService, idmRoleService)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .addFilter<DefaultMockMvcBuilder?>(idmAuthenticationFilter)
            .build()
    }

}
