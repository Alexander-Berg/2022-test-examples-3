package ru.yandex.market.pricingmgmt.config

import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
class WorkbookFactoryTestConfiguration {

    class XSSFWorkbookFactory : WorkbookFactory() {
        override val createWorkbook: Workbook
            get() = XSSFWorkbook()
    }

    @Bean
    @Primary
    fun testWorkbookFactory(): WorkbookFactory = if (isLinux()) {
        XSSFWorkbookFactory()
    } else {
        WorkbookFactory()
    }

    private fun isLinux(): Boolean {
        val osName = System.getProperty("os.name", "generic").lowercase()
        return osName.contains("linux")
    }
}
