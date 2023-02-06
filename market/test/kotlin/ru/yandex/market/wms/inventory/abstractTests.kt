package ru.yandex.market.wms.inventory

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest
import ru.yandex.market.wms.inventory.config.InventoryTestConfig

@TestPropertySource("classpath:application-test.properties")
abstract class AbstractJdbcTest : AbstractJdbcRecipeTest()

@TestPropertySource("classpath:application-test.properties")
@ExtendWith(SpringExtension::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SpringApplicationConfig::class, InventoryTestConfig::class]
)
abstract class AbstractFunctionalTest
