package ru.yandex.market.logistics.mqm.repository


import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.Claim
import ru.yandex.market.logistics.mqm.entity.ClaimUnit
import ru.yandex.market.logistics.mqm.entity.Transaction
import ru.yandex.market.logistics.mqm.entity.enums.ClaimStatus
import ru.yandex.market.logistics.mqm.entity.enums.ClaimUnitStatus
import java.math.BigDecimal
import ru.yandex.market.logistics.mqm.entity.enums.ClaimType

class ClaimRepositoryTest : AbstractContextualTest() {

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Test
    @ExpectedDatabase(query = "/repository/claim/claim.xml", assertionMode = DatabaseAssertionMode.NON_STRICT)
    fun shouldSaveTransactions() {
        transactionTemplate.execute(fun(_: TransactionStatus) {
            val realClaim =
                Claim(
                    name = "Претензия",
                    transactions = listOf(Transaction("123")),
                    status = ClaimStatus.CREATED,
                    plannedAmount = BigDecimal("123.0"),
                    type = ClaimType.FBS_CLAIM
                )

            realClaim.claimUnits = listOf(
                ClaimUnit(status = ClaimUnitStatus.COMPENSATE_SD, issueTicket = "MQMRETURN-123", claim = realClaim)
            )
            val save = claimRepository.save(realClaim)
            val find = claimRepository.getOne(1)
            assertSoftly {
                save.claimUnits shouldBe find.claimUnits
            }
        })
    }
}
