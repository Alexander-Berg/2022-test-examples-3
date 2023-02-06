package ru.yandex.market.replenishment.autoorder.service.tender

import com.amazonaws.services.s3.AmazonS3
import org.apache.ibatis.session.SqlSession
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mboc.http.MboMappings
import ru.yandex.market.mboc.http.MboMappingsService
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.MboExcelFileProcessId
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin
import ru.yandex.market.replenishment.autoorder.service.s3.S3Service
import java.io.InputStream
import java.net.URL
import java.time.LocalDateTime

@WithMockLogin
class TenderAssortmentLoadingServiceTest : FunctionalTest() {

    companion object {
        private val MOCKED_DATE_TIME = LocalDateTime.of(2020, 9, 6, 8, 0)
    }

    private lateinit var tenderAssortmentLoadingService: TenderAssortmentLoadingService

    @Autowired
    private lateinit var batchSqlSession: SqlSession

    private lateinit var mboMappingService: MboMappingsService
    private lateinit var s3Client: AmazonS3

    @Before
    fun mockDateTime() {
        setTestTime(MOCKED_DATE_TIME)
        mboMappingService = Mockito.mock(MboMappingsService::class.java)
        s3Client = Mockito.mock(AmazonS3::class.java)
        tenderAssortmentLoadingService = TenderAssortmentLoadingService(
            batchSqlSession,
            mboMappingService,
            S3Service("test", s3Client),
            timeService
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["TenderAssortmentLoadingServiceTest.importSskuTemplateForSupplier.before.csv"],
        after = ["TenderAssortmentLoadingServiceTest.importSskuTemplateForSupplier.after.csv"]
    )
    fun importSskuTemplateForSupplier_isOk() {
        val requestId = 1L
        Mockito.`when`(mboMappingService.uploadExcelFile(Mockito.any())).thenReturn(
            MboMappings.OfferExcelUpload.Response.newBuilder()
                .setRequestId(requestId)
                .build()
        )
        Mockito.`when`(s3Client.getUrl(Mockito.any(), Mockito.any())).thenReturn(URL("http://localhost:65432/test_url"))
        val excelInputStream: InputStream? =
            this::class.java.getResourceAsStream("TenderAssortmentLoadingServiceTest_supplier-ssku-template.xlsm")
        val bytes: ByteArray = excelInputStream?.readAllBytes() ?: throw IllegalStateException()

        var mboExcelFileProcessId: MboExcelFileProcessId = tenderAssortmentLoadingService.importSskuTemplateForSupplier(
            123L, 4201L, bytes.inputStream(), bytes.size.toLong()
        )
        Assertions.assertEquals(requestId, mboExcelFileProcessId.requestId)
    }
}
