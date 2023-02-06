package ru.yandex.market.wms.common.controller

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestConstructor
import org.springframework.web.servlet.function.support.RouterFunctionMapping
import ru.yandex.market.wms.common.spring.controller.pagematch.RouterFunctionsToStringVisitor

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@Import(PagematchCotrollerTestConfig::class)
class PageMatchControllerTest {

    @Autowired
    private lateinit var routerFunctionMapping: RouterFunctionMapping
    private lateinit var toStringVisitor: RouterFunctionsToStringVisitor

    @BeforeEach
    fun initMocks() {
        routerFunctionMapping = Mockito.mock(RouterFunctionMapping::class.java)
        Mockito.`when`(routerFunctionMapping.routerFunction).thenReturn(
            PagematchCotrollerTestConfig().router()
        )
        toStringVisitor = RouterFunctionsToStringVisitor()
    }

    @Test
    fun fetchRouterFunctions() {
        val routerFunction = routerFunctionMapping.routerFunction
        if (routerFunction != null) {
            val visitor = RouterFunctionsToStringVisitor()
            routerFunction.accept(visitor)
            val content = visitor.toString().split(";").filter { it.trim().isNotBlank() }.toSet()
            assertTrue(content.containsAll(listOf("/baseUrl/upsert", "/baseUrl", "/anotherUrl/activate")))
        }
    }
}
