package ru.yandex.market.ext.marketplace.integrator.app

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.mj.generated.client.ozon_seller_api.api.OzonStocksApiClient

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [SpringApplicationConfig::class])
@TestExecutionListeners(
    mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS,
    listeners = [DbUnitTestExecutionListener::class]
)
@TestPropertySource(
    properties = [
        "ozon.seller_api.url=https://api-seller.ozon.ru"
    ]
)
@ActiveProfiles(profiles = ["functionalTest"])
abstract class AbstractFunctionalTest {

    @MockBean
    lateinit var ozonStocksApiClient: OzonStocksApiClient

}
