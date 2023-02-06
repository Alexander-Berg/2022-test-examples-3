package ru.yandex.market.logistics.calendaring.service.mapper

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest

class CleanMetaMapperErrorsServiceTest(
    @Autowired private val cleanMetaMapperErrorsServiceTest: CleanMetaMapperErrorsService
) : AbstractContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/service/mapper/clean-meta-mapper-errors-before.xml")
    @ExpectedDatabase(
        value = "classpath:fixtures/service/mapper/clean-meta-mapper-errors-after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun cleanErrorsSuccessfully() {
        cleanMetaMapperErrorsServiceTest.clean()
    }

}
