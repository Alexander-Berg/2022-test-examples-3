package ru.yandex.market.logistics.cte.repo

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.enums.MatrixType

class GroupAttributeJdbcRepositoryTest(
    @Autowired private val groupAttributeJdbcRepository: GroupAttributeJdbcRepository) : IntegrationTest() {

    @Test
    @DatabaseSetup("classpath:repository/quality-attribute/on-conflict/before.xml")
    @ExpectedDatabase("classpath:repository/quality-attribute/on-conflict/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun addAttributeIdsToGroupOnConflictTest() {
        groupAttributeJdbcRepository.addAttributeIdsToGroup(1, listOf(1, 2, 3), MatrixType.RETURNS)
    }
}
