package ru.yandex.market.wms.inbound_management.dao

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.TestConstructor
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.inbound_management.entity.ReceiptToPriority
import java.math.BigDecimal
import java.time.Clock

@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptToPriorityDaoTest(private val jdbcTemplate: NamedParameterJdbcTemplate, private val clock: Clock) :
    IntegrationTest() {

    @Autowired
    private lateinit var receiptToPriorityDao: ReceiptToPriorityDao

    companion object {
        private const val USER = "TEST"
        private val RCP_1 = ReceiptToPriority("00001", BigDecimal.valueOf(26100, 4))
        private val RCP_2 = ReceiptToPriority("00002", BigDecimal.valueOf(52200, 4))
    }

    @BeforeEach
    fun setupDao() {
        receiptToPriorityDao = ReceiptToPriorityDao(jdbcTemplate, clock)
    }

    @Test
    @DatabaseSetup(value = ["/db/empty-db.xml"])
    @ExpectedDatabase(value = "/db/receipts-to-priorities/after-update.xml", assertionMode = NON_STRICT_UNORDERED)
    fun selectInsertUpdateByReceiptKey() {
        receiptToPriorityDao.insert(RCP_1, USER)
        val rcpPriority = receiptToPriorityDao.findByReceiptKey(RCP_1.receiptKey)
        Assertions.assertTrue(rcpPriority.isPresent)
        MatcherAssert.assertThat(rcpPriority.get().priorityCoeff, Matchers.equalToObject(RCP_1.priorityCoeff))
        receiptToPriorityDao.updateReceiptPriority(ReceiptToPriority(RCP_1.receiptKey, RCP_2.priorityCoeff), USER)
    }

    @Test
    @DatabaseSetup(value = ["/db/empty-db.xml"])
    @ExpectedDatabase(value = "/db/receipts-to-priorities/after-update-all.xml", assertionMode = NON_STRICT_UNORDERED)
    fun selectInsertUpdateByReceiptKeys() {
        val prioritiesInfoList = listOf<ReceiptToPriority>(RCP_1, RCP_2)
        val rcpKeys = prioritiesInfoList.map { it.receiptKey }
        receiptToPriorityDao.insertAll(prioritiesInfoList, USER)
        val receiptToPriorityList = receiptToPriorityDao.findByReceiptKeys(rcpKeys)
        MatcherAssert.assertThat(receiptToPriorityList, Matchers.hasSize(2))
        receiptToPriorityDao.deleteByReceiptKey(RCP_1.receiptKey)
        receiptToPriorityDao.deleteByReceiptKey(RCP_2.receiptKey)
        val receiptToPriorityListAfterDelete = receiptToPriorityDao.findByReceiptKeys(rcpKeys)
        MatcherAssert.assertThat(receiptToPriorityListAfterDelete, Matchers.empty())
        receiptToPriorityDao.insertAll(listOf<ReceiptToPriority>(RCP_1, RCP_2), USER)
    }
}
