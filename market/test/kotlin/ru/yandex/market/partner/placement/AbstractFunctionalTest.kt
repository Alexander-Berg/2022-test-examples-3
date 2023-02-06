package ru.yandex.market.partner.placement

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.mj.generated.client.wizard_client.api.PartnerPlacementWizardApiClient
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, classes = [SpringApplicationConfig::class])
abstract class AbstractFunctionalTest {

    @Autowired
    protected lateinit var partnerPlacementWizardApiClient: PartnerPlacementWizardApiClient;
}

