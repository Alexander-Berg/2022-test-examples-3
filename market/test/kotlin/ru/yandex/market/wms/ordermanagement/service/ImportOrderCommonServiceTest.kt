package ru.yandex.market.wms.ordermanagement.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO
import ru.yandex.market.wms.common.spring.utils.FileContentUtils
import ru.yandex.market.wms.ordermanagement.service.order.ImportOrderCommonService

@SpringBootTest(classes = [IntegrationTestConfig::class])
class ImportOrderCommonServiceTest : IntegrationTest() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var importOrderCommonService: ImportOrderCommonService

    @Test
    @DatabaseSetup(value = ["/service/import-order/before/db.xml"])
    fun prepareOrderForInsertTest() {

        val order: OrderDTO = objectMapper.readValue<OrderDTO>(
            FileContentUtils.getFileContent("service/import-order/outbound-order.json"),
            object : TypeReference<OrderDTO?>() {})
        importOrderCommonService.prepareOrderForInsert(order)
        Assertions.assertEquals(order.externorderkey, "outbound-8223278")
        Assertions.assertEquals(order.status, "02")
    }
}