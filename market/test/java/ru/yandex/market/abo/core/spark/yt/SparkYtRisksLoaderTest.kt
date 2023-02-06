package ru.yandex.market.abo.core.spark.yt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.spark.yt.risks.SparkYtRisksLoader
import ru.yandex.market.abo.core.yt.YtService
import java.util.Optional

class SparkYtRisksLoaderTest @Autowired constructor(
    private val ytService: YtService,
) : EmptyTest() {

    /**
    Для запуска нужно вставить роботные credentials из секретницы(или свои), чтобы
    сходить в YT. После подстановки пересобрать проект

    /core/src/test/resources/test-application.properties строчки 83-85
     */
    @Test
    @Disabled
    fun `load from yt acting`() {
        val risksRow = SparkYtRisksLoader.load(ytService, "1001601569980")
        assertThat(risksRow)
            .usingRecursiveComparison()
            .isEqualTo(object {
                val ogrn = "1001601569980"
                val sparkId = 1693113
                val inn = "1643002863"
                val isActing = true
                val riskFactors = listOf(
                    object {
                        val factorName = "Наличие контрактов, расторгнутых по соглашению сторон"
                        val addedInfo = Optional.ofNullable(null)
                    }
                )
                val cashRegistersNumber = null
                val salesPointsNumber = null
                val subsidiesSum = "0"
                val activeLicensesNumber = 14
                val activeCertificatesNumber = null
                val property = object {
                    val customsWarehousesNumber = "0"
                    val dutyFreeShopsNumber = "0"
                    val realPropertiesNumber = "0"
                    val rentalPropertiesNumber = "0"
                }
                val domainsNumber = 1
                val trademarksNumber = null
                val inventionsNumber = null
                val industrialModelsNumber = null
                val applicationSoftwareNumber = null
                val integratedCircuitTopographiesNumber = null
                val patentApplicationNumber = null
                val usingTrademarksNumber = null
                val inspections = object {
                    val completed = "1"
                    val scheduled = "0"
                    val withViolations = "0"
                }
            })
    }

    @Test
    @Disabled
    fun `load from yt not acting`() {
        val risksRow = SparkYtRisksLoader.load(ytService, "1000000000032")
        assertThat(risksRow)
            .usingRecursiveComparison()
            .isEqualTo(object {
                val ogrn = "1000000000032"
                val sparkId = 2118359
                val inn = null
                val isActing = false
                val riskFactors = listOf<Any>()
                val cashRegistersNumber = null
                val salesPointsNumber = null
                val subsidiesSum = null
                val activeLicensesNumber = null
                val activeCertificatesNumber = null
                val property = null
                val domainsNumber = null
                val trademarksNumber = null
                val inventionsNumber = null
                val industrialModelsNumber = null
                val applicationSoftwareNumber = null
                val integratedCircuitTopographiesNumber = null
                val patentApplicationNumber = null
                val usingTrademarksNumber = null
                val inspections = null
            })
    }


}

