package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.annotation.ExpectedDatabases
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi
import ru.yandex.market.ff.client.dto.RealSupplierInfoDTO
import ru.yandex.market.ff.client.dto.RealSupplierInfoListDTO
import ru.yandex.market.logistics.cte.base.IntegrationTest

class AscEnrichRealSupplierServiceTest(
    @Autowired val ascEnrichRealSupplierService: AscEnrichRealSupplierService,
    @Autowired val ffwfClient: FulfillmentWorkflowClientApi
) : IntegrationTest() {

    @Test
    @DatabaseSetup(
        value = ["classpath:/service/asc-enrich-real-supplier/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:/service/asc-enrich-real-supplier/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    fun testEnrichSuccessfully() {

        val realSupplierInfo1 = RealSupplierInfoDTO()
        realSupplierInfo1.id = "010765"
        realSupplierInfo1.name = "ООО АБВ"
        val realSupplierInfo2 = RealSupplierInfoDTO()
        realSupplierInfo2.id = "023702"
        realSupplierInfo2.name = "ИП Иванов"

        val realSupplierInfoListDTO = RealSupplierInfoListDTO()
        realSupplierInfoListDTO.realSuppliers = listOf(
            realSupplierInfo1, realSupplierInfo2
        )

        whenever(ffwfClient.findRealSupplyInfoByIds(any())).thenReturn(realSupplierInfoListDTO)

        ascEnrichRealSupplierService.enrich(listOf(1, 2))

    }

}
