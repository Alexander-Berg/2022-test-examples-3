package ru.yandex.market.mdm.metadata.service

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.assertj.core.api.Assertions
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.transaction.TestTransaction
import ru.yandex.market.mbo.lightmapper.exceptions.ItemNotFoundException
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEvent
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass

class AuditorTest : BaseAppTestClass() {

    @Autowired
    lateinit var auditor: Auditor

    @Autowired
    lateinit var mdmEventRepository: MdmEventRepository

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Test
    fun `should save audit event on success`() {
        val toUpdate: List<MdmEntityType> = listOf(mdmEntityType())
        val context = MdmEvent(commitMessage = "test commit", userLogin = "test user")
        val updated = auditor.updateWithAudit(toUpdate, context) {
            mdmEntityTypeRepository.insertOrUpdateBatch(it)
        }

        assertSoftly {
            updated.size shouldBe 1
            updated[0].version.eventId shouldNotBe 0
            context.auditEventId shouldNotBe 0
            context.auditEventId shouldBeEqualComparingTo updated[0].version.eventId
        }

        val auditEvent = mdmEventRepository.findById(context.auditEventId)

        assertSoftly {
            auditEvent.commitMessage shouldBe "test commit"
            auditEvent.userLogin shouldBe "test user"
        }
    }

    @Test
    fun `should rollback all on failure`() {
        try {
            TestTransaction.end()
            val toUpdate: List<MdmEntityType> = listOf(mdmEntityType())
            val context = MdmEvent()
            var updated: List<MdmEntityType> = listOf()
            Assertions.assertThatThrownBy {
                auditor.updateWithAudit(toUpdate, context) {
                    updated = mdmEntityTypeRepository.insertOrUpdateBatch(toUpdate)
                    throw RuntimeException("error")
                }
            }
            assertSoftly {
                updated.size shouldBe 1
                shouldThrowExactly<ItemNotFoundException> { mdmEventRepository.findById(context.auditEventId) }
                mdmEntityTypeRepository.findLatestById(updated[0].mdmId) shouldBe null
            }
        } finally {
            TestTransaction.start()
        }
    }
}
