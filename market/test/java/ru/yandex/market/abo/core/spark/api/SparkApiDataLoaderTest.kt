package ru.yandex.market.abo.core.spark.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.core.CoreConfig
import ru.yandex.market.abo.core.spark.data.CompaniesNumberWithSameInfo
import ru.yandex.market.abo.core.spark.data.ExtendedReport
import ru.yandex.market.abo.core.spark.data.FailureScore
import ru.yandex.market.abo.core.spark.data.IndexOfDueDiligence
import ru.yandex.market.abo.core.spark.data.OKOPF
import ru.yandex.market.util.db.ConfigurationService

class SparkApiDataLoaderTest {

    private val sparkClient: ISparkClient = mock()

    private val coreConfigService: ConfigurationService = mock()

    private val sparkApiDataLoader: SparkApiDataLoader = SparkApiDataLoader(sparkClient, coreConfigService)

    @Test
    fun `company with filial`() {
        whenever(sparkClient.getCompanyExtendedReport(any())).doReturn(
            getReportWithCompanies(getMainCompany(), getFilialCompany())
        )
        val result = sparkApiDataLoader.loadFromSpark(COMPANY_OGRN)
        assertEquals(MAIN_COMPANY_SPARK_ID, result?.sparkId)
    }

    @Test
    fun `only filial`() {
        whenever(sparkClient.getCompanyExtendedReport(any())).doReturn(
            getReportWithCompanies(getFilialCompany())
        )
        val result = sparkApiDataLoader.loadFromSpark(COMPANY_OGRN)
        assertEquals(FILIAL_SPARK_ID, result?.sparkId)
    }

    @Test
    fun `ogrn not found`() {
        whenever(sparkClient.getCompanyExtendedReport(any())).doReturn(ExtendedReport().withData(ExtendedReport.Data()))
        val result = sparkApiDataLoader.loadFromSpark(COMPANY_OGRN)
        assertNull(result)
    }

    @Test
    fun `wrong format ogrn`() {
        val result = sparkApiDataLoader.loadFromSpark(WRONG_OGRN)
        assertNull(result)
    }

    @Test
    fun `check if enabled`() {
        whenever(coreConfigService.getValueAsInt(CoreConfig.DONT_USE_SPARK.id)).doReturn(1)
        assertTrue(sparkApiDataLoader.isDisabled())

        whenever(coreConfigService.getValueAsInt(CoreConfig.DONT_USE_SPARK.id)).doReturn(0)
        assertFalse(sparkApiDataLoader.isDisabled())
    }

    private fun getReportWithCompanies(vararg codes: ExtendedReport.Data.Report): ExtendedReport =
        ExtendedReport().withData(
            ExtendedReport.Data().apply {
                codes.forEach { report.add(it) }
            }
        )

    private fun getMainCompany() =
        getTemplateCompany()
            .withOGRN(COMPANY_OGRN)
            .withSparkID(MAIN_COMPANY_SPARK_ID)
            .withOKOPF(OKOPF().withCode(MAIN_COMPANY_OKOPF_CODE))

    private fun getFilialCompany() =
        getTemplateCompany()
            .withOGRN(COMPANY_OGRN)
            .withSparkID(FILIAL_SPARK_ID)
            .withOKOPF(OKOPF().withCode(FILIAL_OKOPF_CODE))

    private fun getTemplateCompany() =
        ExtendedReport.Data.Report()
            .withCompanyWithSameInfo(CompaniesNumberWithSameInfo())
            .withIsActing("true")
            .withStatus(ExtendedReport.Data.Report.Status())
            .withIndexOfDueDiligence(IndexOfDueDiligence().withIndex("0"))
            .withFailureScore(FailureScore().withFailureScoreValue("0"))

    companion object {
        const val COMPANY_OGRN = "1000000000000"
        const val MAIN_COMPANY_OKOPF_CODE = "10000"
        const val MAIN_COMPANY_SPARK_ID = 1
        const val FILIAL_OKOPF_CODE = "30001"
        const val FILIAL_SPARK_ID = 2

        const val WRONG_OGRN = "123456789"
    }

}
