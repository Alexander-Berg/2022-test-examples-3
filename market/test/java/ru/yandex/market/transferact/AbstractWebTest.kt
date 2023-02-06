package ru.yandex.market.transferact

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus
import ru.yandex.mj.generated.OpenAPI2SpringBoot
import ru.yandex.market.transferact.api.DocumentApiService
import ru.yandex.market.transferact.api.SignatureApiService
import ru.yandex.market.transferact.api.TransferApiService
import ru.yandex.market.transferact.config.TestConfiguration
import ru.yandex.market.transferact.entity.document.DocumentUploadService
import ru.yandex.market.transferact.utils.CleanupAfterEachExtension
import ru.yandex.market.transferact.utils.TestOperationHelper

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [
        TestConfiguration::class,
        TestOperationHelper::class,
        OpenAPI2SpringBoot::class,
        TransferApiService::class,
        SignatureApiService::class,
        DocumentApiService::class
    ]
)
@ActiveProfiles("integrationTest")
@AutoConfigureMockMvc(secure = true, addFilters = true)
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
@ComponentScan(basePackages = [
    "ru.yandex.mj.generated",
    "ru.yandex.mj.generated.server.api",
    "ru.yandex.mj.generated.server",
    "ru.yandex.market.transferact.api.document"
])
abstract class AbstractWebTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var documentUploadService: DocumentUploadService

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Autowired
    protected lateinit var tvmClientApi: TvmClientApi

    @BeforeEach
    fun setUp() {
        Mockito.`when`(tvmClientApi.checkServiceTicket(ArgumentMatchers.any()))
            .thenReturn(
                TvmServiceTicket(
                    123,
                    TvmTicketStatus.OK,
                    ""
                )
            )
    }

    protected fun readFileContent(filename: String): String {
        return this::class.java.classLoader.getResource(filename)?.readText()!!
    }
}
