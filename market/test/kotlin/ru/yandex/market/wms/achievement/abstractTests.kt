package ru.yandex.market.wms.achievement

import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.multipart.MultipartFile
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.javaframework.postgres.test.AbstractJdbcRecipeTest
import ru.yandex.market.wms.achievement.model.condition.Condition
import ru.yandex.market.wms.achievement.model.condition.impl.TestCondition
import ru.yandex.market.wms.achievement.service.storage.StorageService
import java.net.URL
import javax.jms.ConnectionFactory

@TestPropertySource("classpath:application-test.properties")
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = [SpringApplicationConfig::class])
abstract class AbstractFunctionalTest

@TestPropertySource("classpath:application-test.properties")
@Sql("classpath:scripts/truncate.sql")
abstract class AbstractJdbcTest : AbstractJdbcRecipeTest()

@TestConfiguration
class TestConfig {
    @Bean
    fun connectionFactory(): ConnectionFactory = Mockito.mock(ConnectionFactory::class.java)

    @Bean
    fun mockStorageService(): StorageService = object : StorageService {
        override fun uploadImage(image: MultipartFile, achievementId: Long): URL = URL("https://test.com")
    }

    @Bean
    fun testCondition(): Condition = TestCondition()

    @Bean
    fun executor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor()

    @Bean
    fun staffOauth() = ""
}
