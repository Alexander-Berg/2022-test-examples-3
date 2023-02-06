package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile
import ru.yandex.market.logistics.cte.base.IntegrationTest
import java.io.File
import java.io.IOException
import java.util.Objects
import ru.yandex.market.logistics.cte.client.enums.MatrixType

internal class QualityMatrixImportServiceTest(
    @Autowired private val qualityMatrixImportService: QualityMatrixImportService
) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:service/quality-matrix/correct_saving/before.xml")
    @ExpectedDatabase(
        value = "classpath:service/quality-matrix/correct_saving/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun saveHappyPath() {
        val file = getFile("service/quality-matrix/correct_saving/import_file.csv")

        qualityMatrixImportService.import(20, "GROUP 20", file, MatrixType.FULFILLMENT)
        qualityMatrixImportService.import(20, "GROUP 20", file, MatrixType.RETURNS)
    }

    private fun getFile(url: String): MockMultipartFile {
        val input = this.javaClass.classLoader.getResource(url)
        val file = File(Objects.requireNonNull(input).toURI())

        var content: ByteArray? = null
        try {
            content = file.readBytes()
        } catch (e: IOException) {
        }

        return MockMultipartFile(url, url, "text/plan", content)
    }
}
