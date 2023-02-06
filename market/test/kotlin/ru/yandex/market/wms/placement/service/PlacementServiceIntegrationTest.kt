package ru.yandex.market.wms.placement.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.common.spring.exception.NotFoundException
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.placement.service.impl.PlacementServiceImpl

internal class PlacementServiceIntegrationTest @Autowired constructor(
        val placementService: PlacementServiceImpl
): IntegrationTest() {

    @MockBean
    @Autowired
    private lateinit var coreClient: CoreClient

    @MockBean(name = "coreHttpClientSettings")
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    private lateinit var coreHttpClientSettings: HttpClientSettings

    @Test
    @DatabaseSetup("/service/placement/placepallethappypath/before.xml")
    @ExpectedDatabase(
            value = "/service/placement/placepallethappypath/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun placePalletHappyPath() {
        placementService.placePackaging("PLT0000001", "LOC01")
    }

    @Test
    @DatabaseSetup("/service/placement/regular-id/before.xml")
    @ExpectedDatabase(
            value = "/service/placement/regular-id/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun placePalletWithExistsRegularId() {
        placementService.placePackaging("CONTAINER02", "LOC01")
    }

    @Test
    @DatabaseSetup("/service/placement/not-found-id/before.xml")
    @ExpectedDatabase(
            value = "/service/placement/not-found-id/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun placePalletWithNotExistsId() {
        Assertions.assertThrows(NotFoundException::class.java
        ) { placementService.placePackaging("NOT_FOUND", "LOC01") }
    }
}
