package ru.yandex.market.logistics.yard.repository.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils
import ru.yandex.market.logistics.yard_v2.converter.toRejectionDto
import ru.yandex.market.logistics.yard_v2.repository.mapper.CancelMapper

class CancelMapperTest(
    @Autowired private val cancelMapper: CancelMapper,
    @Autowired private val objectMapper:ObjectMapper
) : AbstractSecurityMockedContextualTest() {


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/cancel/before.xml"])
    fun testGetFullByIdAndConvertToDto() {
        val expected = FileContentUtils.getFileContent("classpath:fixtures/repository/cancel/result.json")
        val actual = objectMapper.writeValueAsString(cancelMapper.getFullById(1).toRejectionDto())
        JSONAssert.assertEquals(expected, actual, false)
    }
}
