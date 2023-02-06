package ru.yandex.market.dsm.core.ddd.mapper

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.dsm.core.test.AbstractTest
import ru.yandex.market.dsm.domain.employer.db.EmployerDbo
import ru.yandex.market.dsm.domain.employer.mapper.EmployerProjectionMapper
import ru.yandex.market.dsm.test.AssertsEmployerFactory
import ru.yandex.market.dsm.test.TestUtil

internal class EmployerProjectionMapperTest : AbstractTest() {

    @Autowired
    private lateinit var employerProjectionMapper: EmployerProjectionMapper

    @Test
    fun map() {
        //given
        val entity = TestUtil.OBJECT_GENERATOR.nextObject(EmployerDbo::class.java)

        //then
        AssertsEmployerFactory.asserts(entity, employerProjectionMapper.map(entity))
    }
}
