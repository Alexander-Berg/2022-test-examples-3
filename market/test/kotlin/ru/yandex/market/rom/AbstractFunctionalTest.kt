package ru.yandex.market.rom

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig

@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [SpringApplicationConfig::class])
abstract class AbstractFunctionalTest
