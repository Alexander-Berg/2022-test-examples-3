package ru.yandex.market.integration.npd

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import org.springframework.test.web.servlet.MockMvc
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener
import ru.yandex.market.common.test.db.DbUnitTruncatePolicy
import ru.yandex.market.common.test.db.TruncateType
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient
import ru.yandex.mj.generated.OpenAPI2SpringBoot
import ru.yandex.mj.generated.client.fns_integration_client.api.SelfemployedApiClient

@ActiveProfiles(profiles = ["functionalTest"])
@AutoConfigureMockMvc
@ExtendWith(SpringExtension::class)
@TestExecutionListeners(value = [
    DependencyInjectionTestExecutionListener::class,
    DbUnitTestExecutionListener::class,
    MockitoTestExecutionListener::class
])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SpringApplicationConfig::class, OpenAPI2SpringBoot::class]
)
@MockBean(classes = [
    SelfemployedApiClient::class,
    MbiOpenApiClient::class,
])
@DbUnitTruncatePolicy(truncateType = TruncateType.TRUNCATE)
abstract class AbstractFunctionalTest {
    @Autowired
    lateinit var mockMvc: MockMvc
    @Autowired
    lateinit var client: SelfemployedApiClient
    @Autowired
    lateinit var mbiOpenApiClient: MbiOpenApiClient
}
