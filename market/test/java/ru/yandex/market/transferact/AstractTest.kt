package ru.yandex.market.transferact

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import ru.yandex.market.transferact.common.serialization.ObjectMappers
import ru.yandex.market.transferact.config.TestConfiguration
import ru.yandex.market.transferact.utils.CleanupAfterEachExtension
import ru.yandex.market.transferact.utils.TestOperationHelper

@ActiveProfiles("integrationTest")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [
        TestConfiguration::class,
        TestOperationHelper::class,
    ])
@ExtendWith(CleanupAfterEachExtension::class)
@TestPropertySource(
    properties = [
        "aws.s3.endpoint=https://s3.mds.yandex.net",
        "aws.s3.region=ru-central1",
        "aws.accessKeyId=<your-access-key-id>",
        "aws.secretKey=<your-secret-key>",
        "bucket.name=market-transfer-act-production-document",
    ]
)
abstract class AbstractTest {

    protected fun <T> readObjectFromFile(filename: String, clazz: Class<T>, param: String? = null): T {
        var fileContent = this::class.java.classLoader.getResource(filename)?.readText()!!
        param?.let {
            fileContent = String.format(fileContent, param)
        }
        return ObjectMappers.mapper.readValue(fileContent, clazz)
    }
}
