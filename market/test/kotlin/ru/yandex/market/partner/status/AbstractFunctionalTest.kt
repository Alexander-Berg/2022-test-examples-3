package ru.yandex.market.partner.status

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.abo.api.client.AboAPI
import ru.yandex.market.application.properties.AppPropertyContextInitializer
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.junit.JupiterDbUnitTest
import ru.yandex.market.common.test.spring.PropertiesDirInitializer
import ru.yandex.market.ff4shops.client.FF4ShopsClient
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopDeliveryStateApi
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.datacamp.saas.SaasService
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient
import ru.yandex.mj.generated.client.wizard_client.api.PartnerPlacementStatusApiClient
import ru.yandex.mj.generated.client.wizard_client.api.PartnerPlacementWizardApiClient
import ru.yandex.mj.generated.client.wizard_client.api.PartnerReplicationStatusApiClient
import ru.yandex.mj.generated.server.api.PartnerReplicationStatusApiDelegate
import java.time.Clock

@ExtendWith(SpringExtension::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = [SpringApplicationConfig::class, FunctionalTestConfig::class]
)
@TestPropertySource("classpath:functional-test.properties", "classpath:postgres_test.properties")
@ContextConfiguration(
    initializers = [
        PropertiesDirInitializer::class,
        AppPropertyContextInitializer::class
    ]
)
@DbUnitDataSet // чтобы перед каждым тестом чистилась база
abstract class AbstractFunctionalTest : JupiterDbUnitTest() {

    @Autowired
    protected lateinit var partnerPlacementWizardApiClient: PartnerPlacementWizardApiClient

    @Autowired
    protected lateinit var partnerPlacementStatusApiClient: PartnerPlacementStatusApiClient

    @Autowired
    protected lateinit var partnerReplicationStatusApiClient: PartnerReplicationStatusApiClient

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    protected lateinit var mbiOpenApiClient: MbiOpenApiClient

    @Autowired
    protected lateinit var mbiBillingClient: MbiBillingClient

    @Autowired
    protected lateinit var mbiApiClient: MbiApiClient

    @Autowired
    protected lateinit var saasService: SaasService

    @Autowired
    protected lateinit var lmsClient: LMSClient

    @Autowired
    protected lateinit var mbiLogProcessorClient: MbiLogProcessorClient

    @Autowired
    protected lateinit var ff4shopsClient: FF4ShopsClient

    @Autowired
    protected lateinit var aboApi: AboAPI

    @Autowired
    protected lateinit var shopDeliveryStateApi: ShopDeliveryStateApi

    @Autowired
    protected lateinit var clock: Clock
}
